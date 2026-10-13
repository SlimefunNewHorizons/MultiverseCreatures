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
                    if (!f.visible()) continue;
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
    @DisplayName("Every visit ends with the hand over the eyes, or vanishing in front of them, and is gone")
    void everyVisitEndsInTheFace() {
        for (Map.Entry<String, Map<Behavior, Take>> pose : TAKES.entrySet()) {
            assertFalse(pose.getValue().isEmpty(), pose.getKey() + " plays nothing");
            for (Map.Entry<Behavior, Take> play : pose.getValue().entrySet()) {
                List<Frame> frames = play.getValue().frames();
                String what = pose.getKey() + ", " + play.getKey().id();
                assertEquals(Phase.GONE, frames.get(frames.size() - 1).phase(), what + " never ends");
                assertTrue(frames.stream().anyMatch(f -> f.event() == Event.VANISHED), what + " does not vanish before the player");
                assertTrue(frames.stream().anyMatch(f -> f.event() == Event.NOTICED), what + " is never noticed");
            }
        }
    }

    @Test
    @DisplayName("The palm it holds up sits just in front of the player's eyes")
    void thePalmCoversTheEyes() {
        for (Map.Entry<String, Map<Behavior, Take>> pose : TAKES.entrySet()) {
            Pose start = RIG.pose(pose.getKey());
            float[] p = start.player();
            Vector3d eye = new Vector3d(p[0], p[1] + 1.62, p[2]);
            for (Map.Entry<Behavior, Take> play : pose.getValue().entrySet()) {
                Take take = play.getValue();
                for (int i = 0; i < take.frames().size(); i++) {
                    Frame f = take.frames().get(i);
                    if (f.phase() != Phase.GRAB || !f.visible() || !f.pose().name().startsWith("hand on camera")) continue;
                    if (i + 1 < take.frames().size() && take.frames().get(i + 1).phase() == Phase.GRAB) continue;
                    Vector3f palm = RIG.palm(f.pose(), MonsterMotion.root(f.anchor(), f.yaw(), f.pose()));
                    assertTrue(eye.distance(palm.x, palm.y, palm.z) < 0.75,
                            pose.getKey() + ", " + play.getKey().id() + ": the palm is " + eye.distance(palm.x, palm.y, palm.z)
                                    + " from the eyes");
                }
            }
        }
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
    @DisplayName("The grab aims the palm at the eyes whichever way the player looks")
    void grabAnchorAimsAtTheEyes() {
        Vector3d eye = new Vector3d(10.5, 65.62, -3.5);
        for (Vector3d look : List.of(new Vector3d(0, 0, 1), new Vector3d(1, -0.4, 0), new Vector3d(-0.3, 0.5, -1))) {
            float yaw = MonsterMotion.facing(look, 0f);
            Vector3d anchor = MonsterMotion.grabAnchor(RIG, eye, look, yaw);
            Pose hand = RIG.pose("hand on camera");
            Vector3f palm = RIG.palm(hand, new Matrix4f().rotateY(yaw).mul(MonsterRig.body(hand)));
            Vector3d at = new Vector3d(anchor).add(palm.x, palm.y, palm.z);
            Vector3d wanted = new Vector3d(look).normalize().mul(MonsterMotion.PALM_REACH).add(eye);
            assertEquals(0, at.distance(wanted), 1e-4, "looking along " + look);
        }
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
