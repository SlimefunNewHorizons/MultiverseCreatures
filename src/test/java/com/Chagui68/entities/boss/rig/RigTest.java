package com.Chagui68.entities.boss.rig;

import com.Chagui68.utils.DisplaySculpt;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RigTest {

    private static Rig.Builder trunk() {
        return Rig.skeleton(1f).bone("ROOT", null, new Vector3f(0, 1, 0)).bone("CHEST", "ROOT", new Vector3f(0, 1.4f, 0));
    }

    @Test
    @DisplayName("A skeleton lists parents before children and keeps its legs last")
    void skeletonIsOrdered() {
        assertThrows(IllegalArgumentException.class, () -> Rig.skeleton(1f).bone("ROOT", null, new Vector3f()).bone("HAND", "ARM", new Vector3f()));
        assertThrows(IllegalArgumentException.class, () -> Rig.skeleton(1f).bone("CHEST", null, new Vector3f()).build());
        assertThrows(IllegalArgumentException.class, () -> trunk().bone("LEG_R", "ROOT", new Vector3f()).bone("HEAD", "CHEST", new Vector3f()).build());
    }

    @Test
    @DisplayName("A key that names no bone of the skeleton is refused when the move is written")
    void unknownBoneIsRefused() {
        Rig rig = trunk().build();
        assertThrows(IllegalArgumentException.class, () -> rig.move("x", "X", 10).key(0, Rig.Ease.SMOOTH, "ARM_R 10 0 0"));
    }

    @Test
    @DisplayName("A track holds before its first key and after its last, and eases between")
    void tracksAreSampled() {
        Rig rig = trunk().build();
        Rig.Move move = rig.move("x", "X", 20).key(0, Rig.Ease.SMOOTH, "").key(10, Rig.Ease.LINEAR, "CHEST 40 0 0; move 0 0 2").build();
        List<Rig.Key> chest = move.bones().get(rig.bone("CHEST"));
        assertEquals(0f, Rig.sample(chest, -3)[0], 1e-5f);
        assertEquals(20f, Rig.sample(chest, 5)[0], 1e-4f);
        assertEquals(40f, Rig.sample(chest, 99)[0], 1e-5f);
        assertEquals(1f, Rig.sample(move.travel(), 5)[2], 1e-4f);
    }

    @Test
    @DisplayName("A child bone is carried by its parent's turn")
    void bonesChain() {
        Rig rig = trunk().build();
        Rig.Move move = rig.move("x", "X", 10).key(0, Rig.Ease.SMOOTH, "").key(10, Rig.Ease.LINEAR, "ROOT 0 90 0").build();
        Vector3f front = rig.bones(move, 10)[rig.bone("CHEST")].transformPosition(new Vector3f(0, 1.4f, 1));
        assertEquals(1f, front.x, 1e-4f, "a positive yaw turns the front to the left (+x)");
        assertEquals(0f, front.z, 1e-4f);
    }

    @Test
    @DisplayName("A part that changes hands rides its own bone at 0, its place on the other at 100, and goes straight between")
    void carriedPartChangesHands() {
        // A blade on the chest that is taken a block forward of the hand.
        Rig rig = trunk().bone("HAND", "ROOT", new Vector3f(1, 1, 0)).bone("BLADE", "CHEST", new Vector3f(0, 2, 0)).bone("DIAL", "ROOT", new Vector3f())
                .carry("BLADE", "HAND", "DIAL", new Matrix4f().translation(1, -1, 1)).build();
        Rig.Move move = rig.move("x", "X", 10).key(0, Rig.Ease.LINEAR, "").key(10, Rig.Ease.LINEAR, "DIAL 100 0 0; HAND 0 90 0").build();
        Vector3f grip = new Vector3f(0, 2, 0);
        int blade = rig.bone("BLADE");
        assertEquals(0f, rig.bones(move, 0)[blade].transformPosition(new Vector3f(grip)).distance(grip), 1e-5f, "at 0 it has not left the chest");
        // At 100 it is where the hand, turned a quarter to the left about (1, 1, 0), carries the held place (1, 1, 1): at (2, 1, 0).
        assertEquals(0f, rig.bones(move, 10)[blade].transformPosition(new Vector3f(grip)).distance(2, 1, 0), 1e-4f);
        Vector3f half = rig.bones(move, 5)[blade].transformPosition(new Vector3f(grip));
        Vector3f there = rig.bones(move, 5)[rig.bone("HAND")].transformPosition(new Vector3f(1, 1, 1));
        assertEquals(0f, half.distance(new Vector3f(grip).lerp(there, .5f)), 1e-4f, "half way, its pivot is half way between the two");
    }

    @Test
    @DisplayName("A move aims until two ticks before its first blow unless it says otherwise")
    void aimStopsBeforeTheBlow() {
        Rig rig = trunk().build();
        assertEquals(10, rig.move("x", "X", 30).key(0, Rig.Ease.SMOOTH, "").trail(12, 20).build().aim());
        assertEquals(0, rig.move("x", "X", 30).key(0, Rig.Ease.SMOOTH, "").build().aim());
        assertEquals(4, rig.move("x", "X", 30).key(0, Rig.Ease.SMOOTH, "").cue(20, "boom").aim(4).build().aim());
    }

    @Test
    @DisplayName("A piece that did not move is not sent again")
    void unmovedPiecesAreSkipped() {
        DisplaySculpt.Piece piece = new DisplaySculpt.Piece("ROOT", org.bukkit.Material.STONE, new Vector3f(0, 1, 0), new Quaternionf(), new Vector3f(1, 1, 1), false);
        Transformation rest = Rig.posed(piece, new Matrix4f());
        assertTrue(RigBody.same(rest, Rig.posed(piece, new Matrix4f())));
        assertTrue(RigBody.same(rest, Rig.posed(piece, new Matrix4f().translate(0, .00001f, 0))), "a move too small to see");
        assertFalse(RigBody.same(rest, Rig.posed(piece, new Matrix4f().translate(0, .01f, 0))));
        assertFalse(RigBody.same(rest, Rig.posed(piece, new Matrix4f().rotateY(.05f))));
    }

    @Test
    @DisplayName("The boss turns the short way round, never faster than its rate")
    void turnsTheShortWay() {
        assertEquals(170f + 14f, RiggedBoss.turn(170f, -170f, 14f), 1e-4f, "through 180, not back through 0");
        assertEquals(10f, RiggedBoss.turn(0f, 10f, 14f), 1e-4f);
        assertEquals(-14f, RiggedBoss.turn(0f, -90f, 14f), 1e-4f);
        assertEquals(90f, RiggedBoss.yawOf(new org.bukkit.util.Vector(-1, 0, 0)), 1e-4f, "west is yaw 90");
    }
}
