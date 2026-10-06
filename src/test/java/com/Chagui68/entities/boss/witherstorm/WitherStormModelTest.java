package com.Chagui68.entities.boss.witherstorm;

import com.Chagui68.testsupport.ProjectPaths;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Wither Storm's forms, posed without a server: the boxes the mod draws all land somewhere, every
 * display gets a real rotation, the heads look and open the way the mod turns them, and the forms
 * grow in the order the storm grows through them.
 */
class WitherStormModelTest {

    @Test
    @DisplayName("Every box of every form is drawn, with a unit rotation and a real size")
    void everyBoxIsPlaced() {
        for (WitherStormForm form : WitherStormForm.values()) {
            WitherStormModel model = form.model();
            assertFalse(model.boxes().isEmpty(), form + " has no boxes");
            WitherStormModel.Placement[] placements = model.place(new WitherStormModel.Pose(), 1f);
            for (int i = 0; i < placements.length; i++) {
                WitherStormModel.Placement p = placements[i];
                assertNotNull(p, form + " box " + i + " belongs to no rendered group");
                // A display with an unnormalised rotation is drawn scaled by its length squared: the
                // first export had every head five times too big.
                assertEquals(1.0, p.rotation().lengthSquared(), 1.0e-3, form + " box " + i + " rotation");
                assertTrue(p.scale().x > 0 && p.scale().y > 0 && p.scale().z > 0, form + " box " + i + " size");
            }
        }
    }

    @Test
    @DisplayName("The forms grow in the order the storm grows through them")
    void formsGrow() {
        float previous = 0;
        for (WitherStormForm form : WitherStormForm.values()) {
            float[] b = form.model().bounds(new WitherStormModel.Pose(), 1f);
            float height = b[4];
            assertTrue(height > previous, form + " is not taller than the form before it: " + height);
            previous = height;
        }
        float[] devourer = WitherStormForm.DEVOURER.model().bounds(new WitherStormModel.Pose(), 1f);
        assertTrue(devourer[3] - devourer[0] > 90, "the Devourer is the mod's hundred-block monster");
    }

    @Test
    @DisplayName("A form drawn at twice the size is twice as big round the anchor")
    void sizeScalesRoundTheAnchor() {
        WitherStormModel model = WitherStormForm.DESTROYER.model();
        float[] one = model.bounds(new WitherStormModel.Pose(), 1f);
        float[] two = model.bounds(new WitherStormModel.Pose(), 2f);
        for (int i = 0; i < 6; i++) {
            assertEquals(one[i] * 2, two[i], 1.0e-2, "bound " + i);
        }
    }

    @Test
    @DisplayName("Each form carries the heads the mod gives it")
    void headsPerForm() {
        assertGazes(WitherStormForm.HUNCHBACK, false, false, false);
        assertGazes(WitherStormForm.GROWING, true, false, false);
        assertGazes(WitherStormForm.PREGNANT, true, false, false);
        assertGazes(WitherStormForm.DESTROYER, true, true, true);
        assertGazes(WitherStormForm.DEVOURER, true, true, true);
    }

    private static void assertGazes(WitherStormForm form, boolean... jawed) {
        List<WitherStormModel.Gaze> gazes = form.model().gazes();
        assertEquals(jawed.length, gazes.size(), form + " heads");
        for (int i = 0; i < jawed.length; i++) {
            assertEquals(i, gazes.get(i).index(), form + " head order");
            assertEquals(jawed[i], gazes.get(i).jawed(), form + " head " + i + " jawed");
        }
    }

    @Test
    @DisplayName("Heads look ahead at rest, turn with their yaw and dip with their pitch")
    void headsLook() {
        for (WitherStormForm form : WitherStormForm.values()) {
            WitherStormModel model = form.model();
            for (WitherStormModel.Gaze gaze : model.gazes()) {
                Vector3f ahead = facing(model, gaze, 0, 0);
                assertTrue(ahead.z > 0.9, form + " head " + gaze.index() + " looks " + ahead + " at rest");

                // Minecraft yaw: +90 degrees faces -x.
                Vector3f turned = facing(model, gaze, 90, 0);
                assertTrue(turned.x < -0.9, form + " head " + gaze.index() + " turned to " + turned);

                // Positive pitch looks down.
                Vector3f down = facing(model, gaze, 0, 45);
                assertTrue(down.y < -0.5, form + " head " + gaze.index() + " pitched to " + down);
            }
        }
    }

    private static Vector3f facing(WitherStormModel model, WitherStormModel.Gaze gaze, float yaw, float pitch) {
        WitherStormModel.Pose pose = new WitherStormModel.Pose();
        pose.headYaw[gaze.index()] = yaw;
        pose.headPitch[gaze.index()] = pitch;
        Matrix4f m = model.partMatrices(pose, 1f)[gaze.part()];
        Vector3f centre = WitherStormModel.point(m, gaze.centre().x, gaze.centre().y, gaze.centre().z);
        Vector3f mouth = WitherStormModel.point(m, gaze.mouth().x, gaze.mouth().y, gaze.mouth().z);
        return mouth.sub(centre).normalize();
    }

    @Test
    @DisplayName("A roaring head drops its lower jaw")
    void jawOpens() {
        WitherStormModel model = WitherStormForm.DESTROYER.model();
        WitherStormModel.Head head = model.heads().get(0);
        WitherStormModel.Pose closed = new WitherStormModel.Pose();
        WitherStormModel.Pose open = new WitherStormModel.Pose();
        open.mouth[head.index()] = 2f;
        Vector3f shut = jawTip(model, head, closed);
        Vector3f wide = jawTip(model, head, open);
        assertTrue(wide.y < shut.y - 0.5, "the jaw tip should fall when the mouth opens: " + shut + " -> " + wide);
    }

    private static Vector3f jawTip(WitherStormModel model, WitherStormModel.Head head, WitherStormModel.Pose pose) {
        return WitherStormModel.point(model.partMatrices(pose, 1f)[head.lowerJaw()], 0, 2.5f, 14);
    }

    @Test
    @DisplayName("The old wither skulls stand upright with their faces forward")
    void skullsStandUpright() {
        WitherStormModel model = WitherStormForm.HUNCHBACK.model();
        WitherStormModel.Placement[] placements = model.place(new WitherStormModel.Pose(), 1f);
        int skulls = 0;
        for (int i = 0; i < placements.length; i++) {
            if (!model.boxes().get(i).skull()) continue;
            skulls++;
            Quaternionf q = placements[i].rotation();
            Vector3f up = q.transform(new Vector3f(0, 1, 0));
            Vector3f face = q.transform(new Vector3f(0, 0, -1));
            assertTrue(up.y > 0.9, "skull " + i + " is upside down: " + up);
            assertTrue(face.z > 0.9, "skull " + i + " faces " + face);
        }
        assertEquals(3, skulls, "the hunchback still has the Wither's three heads");
    }

    @Test
    @DisplayName("The command block rides in the hunchbacks' ribcage and is gone once the mass swallows it")
    void commandBlock() {
        assertTrue(uses(WitherStormForm.HUNCHBACK, "COMMAND_BLOCK"));
        assertTrue(uses(WitherStormForm.GROWING, "COMMAND_BLOCK"));
        assertFalse(uses(WitherStormForm.DESTROYER, "COMMAND_BLOCK"));
        assertFalse(uses(WitherStormForm.DEVOURER, "COMMAND_BLOCK"));
    }

    @Test
    @DisplayName("The command block stands upright in the ribcage with its front towards the storm's front")
    void commandBlockStandsUpright() {
        for (WitherStormForm form : List.of(WitherStormForm.HUNCHBACK, WitherStormForm.GROWING)) {
            WitherStormModel model = form.model();
            WitherStormModel.Pose rest = new WitherStormModel.Pose();
            WitherStormModel.Placement[] placements = model.place(rest, 1f);
            Matrix4f[] parts = model.partMatrices(rest, 1f);
            for (int i = 0; i < placements.length; i++) {
                WitherStormModel.Box box = model.boxes().get(i);
                if (!box.oriented()) continue;
                WitherStormModel.Placement p = placements[i];
                Vector3f up = p.rotation().transform(new Vector3f(0, 1, 0));
                Vector3f front = p.rotation().transform(new Vector3f(0, 0, -1));
                assertTrue(up.y > 0.8, form + " command block is upside down: " + up);
                assertTrue(front.z > 0.8, form + " command block faces " + front);
                // Turned round, it must still fill the box the mod draws.
                Vector3f centre = p.rotation().transform(new Vector3f(p.scale()).mul(0.5f)).add(p.translation());
                Vector3f expected = WitherStormModel.point(parts[box.part()], box.x() + box.w() / 2,
                        box.y() + box.h() / 2, box.z() + box.d() / 2);
                assertTrue(centre.distance(expected) < 1.0e-3, form + " command block moved: " + centre + " vs " + expected);
            }
        }
    }

    private static boolean uses(WitherStormForm form, String material) {
        return form.model().boxes().stream().anyMatch(b -> b.material().equals(material));
    }

    @Test
    @DisplayName("Tentacles sway with their own clock")
    void tentaclesSway() {
        for (WitherStormForm form : List.of(WitherStormForm.PREGNANT, WitherStormForm.DESTROYER, WitherStormForm.DEVOURER)) {
            WitherStormModel model = form.model();
            assertFalse(model.tentacles().isEmpty(), form + " has tentacles");
            WitherStormModel.Pose later = new WitherStormModel.Pose();
            later.tentacleTicks = 40;
            Matrix4f[] at0 = model.partMatrices(new WitherStormModel.Pose(), 1f);
            Matrix4f[] at40 = model.partMatrices(later, 1f);
            for (WitherStormModel.Tentacle t : model.tentacles()) {
                Vector3f a = model.tip(at0, t), b = model.tip(at40, t);
                assertTrue(a.distance(b) > 0.05, form + " tentacle did not move: " + a + " " + b);
            }
        }
    }

    @Test
    @DisplayName("Every block a form names exists in the palette the plugin can draw")
    void materialsAreKnown() {
        List<String> known = List.of("BLACK_CONCRETE", "OBSIDIAN", "CRYING_OBSIDIAN", "BLACKSTONE", "POLISHED_BLACKSTONE",
                "GRAY_CONCRETE", "PURPLE_CONCRETE", "MAGENTA_CONCRETE", "AMETHYST_BLOCK", "LIGHT_GRAY_CONCRETE",
                "WHITE_CONCRETE", "COMMAND_BLOCK", "SKULL");
        for (WitherStormForm form : WitherStormForm.values()) {
            for (WitherStormModel.Box box : form.model().boxes()) {
                assertTrue(known.contains(box.material()), form + " uses " + box.material());
            }
        }
    }

    @Test
    @DisplayName("Tractor beams come the way the mod gives them: none at first, the middle head, then all")
    void beamsPerForm() {
        for (int head = 0; head < 3; head++) {
            assertFalse(WitherStormForm.HUNCHBACK.hasBeam(head), "the first form has no beam");
            assertEquals(head == 0, WitherStormForm.GROWING.hasBeam(head));
            assertEquals(head == 0, WitherStormForm.PREGNANT.hasBeam(head));
            assertTrue(WitherStormForm.DESTROYER.hasBeam(head));
            assertTrue(WitherStormForm.DEVOURER.hasBeam(head));
        }
    }

    @Test
    @DisplayName("The hunchbacks feed by sucking in the ground, more every form; the big forms feed by beam")
    void hunchbacksSuckInTheGround() {
        int previous = 0;
        for (WitherStormForm form : List.of(WitherStormForm.HUNCHBACK, WitherStormForm.GROWING, WitherStormForm.PREGNANT)) {
            assertTrue(form.suctionClusters() > previous, form + " sucks in " + form.suctionClusters());
            previous = form.suctionClusters();
        }
        assertEquals(0, WitherStormForm.DESTROYER.suctionClusters());
        assertEquals(0, WitherStormForm.DEVOURER.suctionClusters());
    }

    @Test
    @DisplayName("A head with its beam on turns slower than a player sprints across it")
    void beamsCanBeOutrun() {
        for (WitherStormForm form : WitherStormForm.values()) {
            // A sprint is 0.28 blocks a tick: at 12 blocks that is 1.3 degrees a tick.
            assertTrue(form.beamAimRate() <= 1.4, form + " aims its beam at " + form.beamAimRate());
        }
    }

    @Test
    @DisplayName("The Destroyer flies its low-detail mass unless high detail is asked for")
    void destroyerDetail() {
        int low = WitherStormForm.DESTROYER.model(false).boxes().size();
        int high = WitherStormForm.DESTROYER.model(true).boxes().size();
        assertTrue(low < 250, "low-detail Destroyer has " + low + " boxes");
        assertTrue(high > low * 2, "high-detail Destroyer has " + high + " boxes");
        assertEquals(WitherStormForm.DEVOURER.model(), WitherStormForm.DEVOURER.model(true),
                "only the Destroyer has a second model");
    }

    @Test
    @DisplayName("Only a Wither built on the core block becomes a storm")
    void altarCore() {
        assertTrue(WitherStormBoss.isAltarCore(org.bukkit.Material.CRYING_OBSIDIAN, org.bukkit.Material.CRYING_OBSIDIAN));
        assertFalse(WitherStormBoss.isAltarCore(org.bukkit.Material.SOUL_SAND, org.bukkit.Material.CRYING_OBSIDIAN));
        assertFalse(WitherStormBoss.isAltarCore(null, org.bukkit.Material.CRYING_OBSIDIAN));
    }

    @Test
    @DisplayName("config.yml configures every form, and each one needs more to outgrow than the last")
    @SuppressWarnings("unchecked")
    void configuredForms() throws IOException {
        Map<String, Object> config;
        try (InputStream in = Files.newInputStream(ProjectPaths.resource("config.yml"))) {
            config = new Yaml().load(in);
        }
        Map<String, Object> storm = (Map<String, Object>) ((Map<String, Object>) config.get("entities")).get("wither-storm");
        Map<String, Object> forms = (Map<String, Object>) storm.get("forms");
        int previous = 0;
        for (WitherStormForm form : WitherStormForm.values()) {
            Map<String, Object> f = (Map<String, Object>) forms.get(form.key());
            assertNotNull(f, "no config for " + form.key());
            assertTrue(((Number) f.get("scale")).doubleValue() > 0, form + " scale");
            int evolveAt = ((Number) f.get("evolve-at")).intValue();
            if (form.next() != null) {
                assertTrue(evolveAt > previous, form + " evolves at " + evolveAt + ", not after " + previous);
                previous = evolveAt;
            }
        }
        assertEquals("CRYING_OBSIDIAN", ((Map<String, Object>) storm.get("summon")).get("core-block"));
    }
}
