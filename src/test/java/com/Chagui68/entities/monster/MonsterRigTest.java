package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Monster's rig as the game uses it: every head moves with one part, every pose rests on what it
 * is meant to rest on (the floor, the underside of a ceiling, the top of a block), there is a pose for
 * every kind of place a player might be in, and the hand it ends with reaches the player's eyes.
 */
class MonsterRigTest {

    private static final MonsterRig RIG = MonsterRig.get();
    /** How far a pose may sink into, or float off, what holds it. */
    private static final float TOUCH = 0.06f;

    @Test
    @DisplayName("Every one of the eighty heads moves with exactly one part")
    void everyHeadHasAPart() {
        assertEquals(80, RIG.size());
        for (int i = 0; i < RIG.size(); i++) {
            assertTrue(RIG.partNames().contains(RIG.partOf(i)), "head " + i);
        }
    }

    @Test
    @DisplayName("A head without a part, a head in two parts and a pose turning a missing part are refused")
    void brokenRigsAreRefused() {
        String twoHeads = "/summon block_display ~ ~ ~ {Passengers:["
                + head() + "," + head() + "]}";
        assertThrows(IllegalArgumentException.class, () -> MonsterRig.parse(rig("[[0, 0]]", "[[1, 1]]", "{}"), twoHeads.replace(head() + ",", "")),
                "one head in the export, but the parts claim two");
        assertThrows(IllegalArgumentException.class, () -> MonsterRig.parse(rig("[[0, 0]]", "[[0, 0]]", "{}"), twoHeads),
                "head 0 claimed twice, head 1 by nobody");
        assertThrows(IllegalArgumentException.class,
                () -> MonsterRig.parse(rig("[[0, 0]]", "[[1, 1]]", "{\"place\": \"ground\", \"TAIL\": [1, 0, 0]}"), twoHeads));
        MonsterRig fine = MonsterRig.parse(rig("[[0, 0]]", "[[1, 1]]", "{\"place\": \"ground\", \"B\": [10, 0, 0]}"), twoHeads);
        assertEquals("B", fine.partOf(1));
    }

    @Test
    @DisplayName("Every pose rests on what holds it: the floor, the ceiling above or the block below")
    void posesRestOnTheirPlace() {
        for (Pose pose : RIG.poses().values()) {
            List<Vector3f> corners = corners(pose);
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (Vector3f c : corners) {
                minY = Math.min(minY, c.y);
                maxY = Math.max(maxY, c.y);
            }
            switch (pose.place()) {
                case GROUND -> assertEquals(0f, minY, TOUCH, pose.name() + " stands on the floor");
                case CEILING -> assertEquals(0f, maxY, TOUCH, pose.name() + " hangs from the ceiling");
                case BLOCK -> {
                    assertTrue(minY > -1 - TOUCH, pose.name() + " reaches through the floor round the block");
                    // Limbs may hang down the block's sides, but nothing goes into it, and the body
                    // touches its top instead of floating over it.
                    // A double chest is two blocks along x; anything else one.
                    float hx = pose.wide() ? 1f : 0.5f;
                    boolean touches = false;
                    for (Vector3f c : corners) {
                        touches |= Math.abs(c.x) < hx && Math.abs(c.z) < 0.5f && Math.abs(c.y) < TOUCH;
                        assertFalse(Math.abs(c.x) < hx - 0.12f && Math.abs(c.z) < 0.38f && c.y < -0.08f && c.y > -0.9f,
                                pose.name() + " sinks into its block at " + c);
                    }
                    assertTrue(touches, pose.name() + " lies on the top of its block, not over it");
                }
                default -> {
                }
            }
        }
    }

    @Test
    @DisplayName("There are poses for low rooms, ceilings, chests and blocks, and the air")
    void posesForEveryPlace() {
        assertTrue(RIG.poses(Place.CEILING).size() >= 2, "ceiling poses");
        assertTrue(RIG.poses(Place.BLOCK).size() >= 4, "block poses");
        assertTrue(RIG.poses(Place.AIR).size() >= 1, "poses for a flying player");
        long low = RIG.poses(Place.GROUND).stream()
                .filter(p -> RIG.bounds(p, MonsterRig.body(p))[4] <= 2.1f).count();
        assertTrue(low >= 3, "poses that fit under a two-block ceiling: " + low);
        assertTrue(RIG.poses().size() - RIG.poses(Place.ACTION).size() >= 38, "8 first poses, 10 for places, 15 tight, 5 on double chests");
        assertEquals(5, RIG.poses(Place.BLOCK).stream().filter(Pose::wide).count(), "poses on a double chest");
        // The visit plays these by name.
        for (String name : List.of("idle", "crouched", "perched", "hand on camera", "hand on camera low",
                "hand on camera crawl", "sitting up on a double chest", "sitting on a double chest", "corridor stalk",
                "corridor crouch", "corridor crawl", "crawling", "ceiling cling", "corridor ceiling crawl")) {
            assertNotNull(RIG.pose(name), name);
        }
    }

    @Test
    @DisplayName("Fifteen tight poses fit the one or two block rooms they were made for")
    void tightPosesFitTheirRoom() {
        java.util.Set<String> rooms = new java.util.HashSet<>();
        int tight = 0;
        for (Pose pose : RIG.poses().values()) {
            float[] space = pose.space();
            if (space == null) continue;
            tight++;
            rooms.add(Math.round(space[0]) + "x" + Math.round(space[1]));
            float[] b = RIG.bounds(pose, MonsterRig.body(pose));
            // The game shrinks the box by 0.2 a side before it checks blocks; this keeps a margin.
            float slack = 0.25f;
            assertTrue(b[3] - b[0] <= space[0] + slack, pose.name() + " is " + (b[3] - b[0]) + " wide for " + space[0]);
            assertTrue(b[5] - b[2] <= space[1] + slack, pose.name() + " is " + (b[5] - b[2]) + " deep for " + space[1]);
            assertTrue(b[4] - b[1] <= space[2] + slack, pose.name() + " is " + (b[4] - b[1]) + " tall for " + space[2]);
        }
        assertTrue(tight >= 15, "tight poses: " + tight);
        assertTrue(rooms.containsAll(List.of("1x1", "1x2", "2x1")), "rooms covered: " + rooms);
    }

    @Test
    @DisplayName("Every pose has its own command name, and the command finds it in any case")
    void poseIdsAreUnique() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Pose pose : RIG.poses().values()) {
            assertFalse(pose.id().contains(" "), pose.id());
            assertTrue(ids.add(pose.id().toLowerCase(java.util.Locale.ROOT)), "two poses are called " + pose.id());
            assertSame(pose, RIG.byId(pose.id().toUpperCase(java.util.Locale.ROOT)));
        }
        assertNull(RIG.byId("random"), "random is the command's, not a pose");
    }

    @Test
    @DisplayName("The hand it ends with reaches out in front of it, at a player's eye height")
    void handReachesTheEyes() {
        Pose hand = RIG.pose("hand on camera");
        Vector3f palm = RIG.palm(hand, MonsterRig.body(hand));
        assertTrue(palm.z > 1.2f, "the palm is held out in front: " + palm);
        assertTrue(palm.y > 1.3f && palm.y < 2.1f, "the palm is at a standing player's eyes: " + palm);
        Vector3f eyes = RIG.eyes(hand, MonsterRig.body(hand));
        assertTrue(eyes.z < palm.z, "its head stays behind its hand");
    }

    @Test
    @DisplayName("Blending eases from one pose to the next, every head starting and ending in place")
    void blendStartsAndEndsOnThePoses() {
        Pose from = RIG.pose("ceiling cling"), to = RIG.pose("crouched");
        assertHeads(from, from.blend(to, 0f));
        assertHeads(to, from.blend(to, 1f));
        Pose half = from.blend(to, 0.5f);
        assertEquals((from.tilt().z + to.tilt().z) / 2, half.tilt().z, 1e-4, "the tilt is half way");
    }

    // ------------------------------------------------------------------ helpers

    private static void assertHeads(Pose expected, Pose actual) {
        Matrix4f[] a = RIG.heads(expected, MonsterRig.body(expected));
        Matrix4f[] b = RIG.heads(actual, MonsterRig.body(actual));
        for (int i = 0; i < a.length; i++) {
            assertTrue(a[i].equals(b[i], 1e-4f), "head " + i + " of " + expected.name());
        }
    }

    private static List<Vector3f> corners(Pose pose) {
        List<Vector3f> out = new ArrayList<>();
        for (Matrix4f m : RIG.heads(pose, MonsterRig.body(pose))) {
            for (int c = 0; c < 8; c++) {
                out.add(m.transformPosition((c & 1) == 0 ? -0.25f : 0.25f, (c & 2) == 0 ? 0f : -0.5f,
                        (c & 4) == 0 ? -0.25f : 0.25f, new Vector3f()));
            }
        }
        return out;
    }

    private static String head() {
        return "{id:\"minecraft:item_display\",item:{id:\"minecraft:player_head\",components:{\"minecraft:profile\":"
                + "{properties:[{name:\"textures\",value:\"e30=\"}]}}},"
                + "transformation:[0.5f,0f,0f,0f,0f,0.5f,0f,0f,0f,0f,0.5f,0f,0f,0f,0f,1f]}";
    }

    private static JsonObject rig(String a, String b, String pose) {
        return JsonParser.parseString("{\"key\": \"t\", \"name\": \"T\", \"group\": \"G\", \"player\": [0, 0, 2],"
                + "\"eyes\": [0, 0, 0], \"palm\": [0, 0, 0],"
                + "\"parts\": {\"A\": {\"parent\": null, \"pivot\": [0, 0, 0], \"heads\": " + a + "},"
                + "\"B\": {\"parent\": \"A\", \"pivot\": [0, 0, 0], \"heads\": " + b + "}},"
                + "\"poses\": {\"p\": " + pose + "}}").getAsJsonObject();
    }
}
