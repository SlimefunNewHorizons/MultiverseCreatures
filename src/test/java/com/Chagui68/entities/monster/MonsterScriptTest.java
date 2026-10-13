package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import com.Chagui68.entities.monster.MonsterScript.Behavior;
import com.Chagui68.entities.monster.MonsterScript.Event;
import com.Chagui68.entities.monster.MonsterScript.Frame;
import com.Chagui68.entities.monster.MonsterScript.Phase;
import com.Chagui68.entities.monster.MonsterScript.Take;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Monster's visit as {@link MonsterScript} plays it, from every pose it waits in, in the blocks the
 * viewer shows round that pose: the same plan the game runs in the world.
 */
class MonsterScriptTest {

    private static final MonsterRig RIG = MonsterRig.get();
    private static final MonsterScript.Settings SETTINGS = new MonsterScript.Settings(0.35, 25, 100_000, 30);
    /** Every pose's rehearsal of every behaviour it really plays. */
    private static final Map<String, Map<Behavior, Take>> TAKES = new LinkedHashMap<>();

    @BeforeAll
    static void rehearseEverything() {
        for (Pose pose : RIG.poses().values()) {
            if (pose.place() == Place.ACTION) continue;
            Map<Behavior, Take> plays = new LinkedHashMap<>();
            for (Behavior behavior : Behavior.values()) {
                Take take = MonsterScript.rehearse(RIG, pose, behavior, SETTINGS, 7L);
                if (take.played() == behavior) plays.put(behavior, take);
            }
            TAKES.put(pose.name(), plays);
        }
    }

    @Test
    @DisplayName("No frame of any visit puts its body inside a block")
    void neverInsideABlock() {
        int checked = 0;
        for (Map.Entry<String, Map<Behavior, Take>> pose : TAKES.entrySet()) {
            MonsterSpace space = space(RIG.pose(pose.getKey()));
            for (Map.Entry<Behavior, Take> play : pose.getValue().entrySet()) {
                List<Frame> frames = play.getValue().frames();
                for (int i = 0; i < frames.size(); i++) {
                    Frame f = frames.get(i);
                    // Its face alone is drawn at the player's camera, where there is no block.
                    if (!f.visible() || f.face()) continue;
                    checked++;
                    float[] box = RIG.bounds(f.pose(), MonsterRig.body(f.pose()));
                    assertTrue(MonsterSpace.fits(space, f.anchor(), f.yaw(), box, f.resting()),
                            pose.getKey() + ", " + play.getKey().id() + ", tick " + i + " (" + f.phase() + ", "
                                    + f.pose().name() + ") is inside a block");
                }
            }
        }
        assertTrue(checked > 1000, "only " + checked + " frames checked");
    }

    @Test
    @DisplayName("Every visit ends with its face in the player's face, screaming, and is gone")
    void everyVisitEndsInTheFace() {
        for (Map.Entry<String, Map<Behavior, Take>> pose : TAKES.entrySet()) {
            assertFalse(pose.getValue().isEmpty(), pose.getKey() + " plays nothing");
            for (Map.Entry<Behavior, Take> play : pose.getValue().entrySet()) {
                List<Frame> frames = play.getValue().frames();
                String what = pose.getKey() + ", " + play.getKey().id();
                assertEquals(Phase.GONE, frames.get(frames.size() - 1).phase(), what + " never ends");
                assertTrue(frames.stream().anyMatch(f -> f.event() == Event.SCREAM), what + " never screams");
                assertTrue(frames.stream().anyMatch(f -> f.event() == Event.VANISHED), what + " does not vanish before the player");
                assertTrue(frames.stream().anyMatch(f -> f.event() == Event.NOTICED), what + " is never noticed");
            }
        }
    }

    @Test
    @DisplayName("Its face pops up just in front of the player's eyes, turned to them, and grows to full size")
    void theFaceIsInTheirFace() {
        int faces = 0;
        for (Map.Entry<String, Map<Behavior, Take>> pose : TAKES.entrySet()) {
            float[] p = RIG.pose(pose.getKey()).player();
            Vector3d eye = new Vector3d(p[0], p[1] + 1.62, p[2]);
            for (Map.Entry<Behavior, Take> play : pose.getValue().entrySet()) {
                List<Frame> frames = play.getValue().frames();
                float last = 0;
                for (Frame f : frames) {
                    if (!f.face()) continue;
                    faces++;
                    assertEquals(Phase.SCREAM, f.phase());
                    assertEquals(MonsterScript.FACE_DISTANCE, eye.distance(f.anchor()), 1e-6,
                            pose.getKey() + ", " + play.getKey().id() + ": the face is not in front of the eyes");
                    assertTrue(f.faceScale() >= last && f.faceScale() <= 1f, "it grows, then stays");
                    last = f.faceScale();
                }
                if (last > 0) assertEquals(1f, last, 1e-6, "it ends full size");
            }
        }
        assertTrue(faces > 100, "only " + faces + " face frames");
    }

    @Test
    @DisplayName("Each of the five new behaviours plays where it was made for")
    void theNewBehavioursPlay() {
        Set<Behavior> played = EnumSet.noneOf(Behavior.class);
        TAKES.values().forEach(plays -> played.addAll(plays.keySet()));
        assertEquals(EnumSet.allOf(Behavior.class), played, "behaviours that never play anywhere");
        for (String chest : List.of("lying on a double chest", "sitting up on a double chest", "face down on a double chest")) {
            assertTrue(TAKES.get(chest).containsKey(Behavior.COFFIN_RISE), chest + " rises like from a coffin");
        }
        assertTrue(TAKES.get("ceiling cling").containsKey(Behavior.CEILING_SCUTTLE), "it runs along an open ceiling");
        assertTrue(TAKES.get("corridor crawl").containsKey(Behavior.WALK), "it crawls out of a corridor");
        assertFalse(TAKES.get("reaching up a shaft").containsKey(Behavior.WALK), "a closed shaft has no way out");
        assertTrue(TAKES.get("reaching up a shaft").containsKey(Behavior.BLINK), "so it blinks out of it");
    }

    @Test
    @DisplayName("Out of a low corridor it walks crouched or on all fours, never standing")
    void itStoopsInLowPlaces() {
        Take take = TAKES.get("corridor crawl").get(Behavior.WALK);
        MonsterSpace space = space(RIG.pose("corridor crawl"));
        for (Frame f : take.frames()) {
            if (f.phase() != Phase.MOVE) continue;
            // Inside the corridor (x 0 to 1, z -3 to 1, two blocks high) nothing taller than it fits.
            if (f.anchor().z < 0.9 && f.anchor().x > 0 && f.anchor().x < 1) {
                float[] box = RIG.bounds(f.pose(), MonsterRig.body(f.pose()));
                assertTrue(box[4] - box[1] <= 2.2f, f.pose().name() + " is too tall for the corridor");
            }
            assertTrue(MonsterSpace.fits(space, f.anchor(), f.yaw(), RIG.bounds(f.pose(), MonsterRig.body(f.pose())), Set.of()));
        }
    }

    @Test
    @DisplayName("The face it screams with is its head and jaw alone, its eyes where it is put")
    void theFaceIsOnlyItsHead() {
        Pose pose = RIG.pose("idle").plus(Map.of("JAW", new Vector3f(55, 0, 0)));
        Matrix4f place = new Matrix4f().translation(3, 70, -2).rotateY(1.1f).rotateX(-0.4f);
        Matrix4f[] heads = RIG.face(pose, place, 1f);
        int shown = 0;
        for (int i = 0; i < heads.length; i++) {
            String part = RIG.partOf(i);
            boolean face = part.equals("HEAD") || part.equals("JAW");
            assertEquals(face, heads[i].determinant() != 0, "head " + i + " of " + part);
            if (face) shown++;
        }
        assertTrue(shown > 10, "the face has " + shown + " heads");
        // Where its eyes were, the face carried there: on the place's origin.
        Matrix4f[] whole = RIG.heads(pose, MonsterRig.body(pose));
        Vector3f eyes = RIG.eyes(pose, MonsterRig.body(pose));
        int h = RIG.partNames().indexOf("HEAD");
        for (int i = 0; i < heads.length; i++) {
            if (RIG.ownerOf(i) != h) continue;
            Vector3f before = whole[i].transformPosition(new Vector3f()).sub(eyes);
            Vector3f after = heads[i].transformPosition(new Vector3f());
            Vector3f wanted = place.transformPosition(new Vector3f(before));
            assertEquals(0, after.distance(wanted), 1e-4, "head " + i);
        }
    }

    @Test
    @DisplayName("Left to choose, it plays every behaviour its place allows, not always the same one")
    void theGameUsesEveryBehaviour() {
        Set<Behavior> floor = EnumSet.noneOf(Behavior.class), ceiling = EnumSet.noneOf(Behavior.class),
                chest = EnumSet.noneOf(Behavior.class);
        // Seeds spread out: java.util.Random gives nearly the same first numbers for 1, 2, 3...
        for (long n = 1; n <= 40; n++) {
            long seed = n * 0x9E3779B97F4A7C15L;
            floor.add(MonsterScript.rehearse(RIG, RIG.pose("crouched"), null, SETTINGS, seed).played());
            ceiling.add(MonsterScript.rehearse(RIG, RIG.pose("ceiling cling"), null, SETTINGS, seed).played());
            chest.add(MonsterScript.rehearse(RIG, RIG.pose("lying on a double chest"), null, SETTINGS, seed).played());
        }
        assertTrue(floor.containsAll(EnumSet.of(Behavior.WALK, Behavior.CRAWL_RUSH, Behavior.BLINK, Behavior.PEEK_AND_HIDE)),
                "from the floor it played " + floor);
        assertTrue(ceiling.contains(Behavior.CEILING_SCUTTLE) && ceiling.size() >= 3, "from a ceiling it played " + ceiling);
        assertTrue(chest.contains(Behavior.COFFIN_RISE) && chest.size() >= 3, "from a double chest it played " + chest);
    }

    @Test
    @DisplayName("Its dry run knows a closed shaft has no way out and open ground has one")
    void itKnowsWhereItCanGetOut() {
        Pose shaft = RIG.pose("reaching up a shaft"), open = RIG.pose("crouched");
        float[] p = shaft.player();
        Vector3d feet = new Vector3d(p[0], p[1], p[2]), eye = new Vector3d(feet).add(0, 1.62, 0);
        assertFalse(MonsterScript.reaches(RIG, shaft.world().space(), shaft, new Vector3d(shaft.at()),
                (float) Math.toRadians(shaft.yaw()), feet, eye, false, null), "walls on every side");
        assertTrue(MonsterScript.reaches(RIG, shaft.world().space(), shaft, new Vector3d(shaft.at()),
                (float) Math.toRadians(shaft.yaw()), feet, eye, false, Behavior.BLINK), "but it can always blink");
        float[] q = open.player();
        Vector3d feet2 = new Vector3d(q[0], q[1], q[2]);
        assertTrue(MonsterScript.reaches(RIG, new MonsterSpace.Blocks(true), open, new Vector3d(open.at()),
                (float) Math.toRadians(open.yaw()), feet2, new Vector3d(feet2).add(0, 1.62, 0), false, null), "open ground");
    }

    @Test
    @DisplayName("A block key gives back its block, below zero too")
    void keysRoundTrip() {
        for (int[] b : List.of(new int[]{0, 0, 0}, new int[]{-1, -64, -1}, new int[]{123456, 319, -98765}, new int[]{-3, 5, 7})) {
            assertArrayEquals(b, MonsterSpace.unkey(MonsterSpace.key(b[0], b[1], b[2])));
        }
    }

    private static MonsterSpace space(Pose pose) {
        return pose.world() != null ? pose.world().space() : new MonsterSpace.Blocks(true);
    }
}
