package com.Chagui68.entities.boss.radahn;

import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.RigForm;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadahnBossTest {

    @Test
    @DisplayName("He rides at a point that leaves his target on his sword hand, and never turns away from one he is level with")
    void passesWithTheTargetOnHisRight() {
        // Facing south (+z), his right is west (-x): the point he rides at must be east of the target.
        Vector far = RadahnBoss.passing(new Vector(0, 0, 1), 10);
        assertEquals(RadahnBoss.ASIDE, far.getX(), 1e-9);
        assertEquals(10, far.getZ(), 1e-9);
        // Close in the point stays ahead of him: half the way aside at most.
        Vector near = RadahnBoss.passing(new Vector(0, 0, 1), 2);
        assertEquals(1, near.getX(), 1e-9);
        assertTrue(Math.toDegrees(Math.atan2(near.getX(), near.getZ())) < 30);
        // Whichever way he comes from.
        Vector west = RadahnBoss.passing(new Vector(-1, 0, 0), 10);
        assertEquals(-10, west.getX(), 1e-9);
        assertEquals(RadahnBoss.ASIDE, west.getZ(), 1e-9);
    }

    @Test
    @DisplayName("An arrow turns after its quarry only so fast, and keeps its speed")
    void arrowsTurnSlowly() {
        Vector velocity = new Vector(0, 0, 2);
        Arrow.steer(velocity, new Vector(5, 0, 0), Math.toRadians(9));
        assertEquals(2, velocity.length(), 1e-9);
        assertEquals(9, Math.toDegrees(Math.atan2(velocity.getX(), velocity.getZ())), 1e-6);
        // Within its turn of the way there, it flies straight at it.
        Arrow.steer(velocity, new Vector(1, 0, 6), Math.toRadians(9));
        assertEquals(Math.toDegrees(Math.atan2(1, 6)), Math.toDegrees(Math.atan2(velocity.getX(), velocity.getZ())), 1e-6);
        // Dead astern it still finds a way round.
        Vector away = new Vector(0, 0, 1);
        for (int tick = 0; tick < 40; tick++) Arrow.steer(away, new Vector(0, 0, -1), Math.toRadians(9));
        assertTrue(away.getZ() < -.99, "it came about in two seconds");
        assertEquals(1, away.length(), 1e-9);
    }

    /**
     * Whether the blade of {@code id} passes through a player who stands {@code aside} blocks to his
     * right and {@code ahead} blocks up his path when the move begins: the boss's own test, a point
     * of the weapon inside the player's box grown by 0.3.
     */
    private static boolean cuts(String id, double aside, double ahead) {
        RigForm form = RadahnModel.FORM;
        Rig.Move move = form.move(id);
        for (int[] trail : move.trails()) {
            for (float t = trail[0]; t <= trail[1]; t += .5f) {
                Matrix4f[] world = form.rig().bones(move, t);
                for (int k = 0; k <= 4; k++) {
                    Vector3f p = world[form.weaponBone()].transformPosition(form.weaponPoint(k / 4f));
                    if (Math.abs(p.x + aside) <= .6 && Math.abs(p.z - ahead) <= .6 && p.y >= -.3 && p.y <= 2.1) return true;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("Riding by, the sweep and the quick cut meet a player up his path from wherever he begins them")
    void ridingCutsLand() {
        // He turns to his target as he winds up, so it is ahead of him or a block to his sword hand when the blade comes.
        for (double aside : new double[]{0, .5, 1}) {
            for (double ahead = RadahnBoss.SLASH_FROM; ahead <= RadahnBoss.SWEEP_FROM; ahead += .5) {
                assertTrue(cuts("slash", aside, ahead), "the quick cut misses a player " + aside + " aside, " + ahead + " ahead");
            }
            for (double ahead = RadahnBoss.SWEEP_FROM; ahead <= RadahnBoss.CUT; ahead += .5) {
                assertTrue(cuts("sweep", aside, ahead), "the sweep misses a player " + aside + " aside, " + ahead + " ahead");
            }
        }
    }

    @Test
    @DisplayName("With the bow drawn his swords are on his back and the bow is in his left hand; otherwise each is where it is worn")
    void weaponsChangeHands() {
        Rig rig = RadahnModel.RIG;
        Rig.Move shot = RadahnModel.FORM.move("shot");
        Vector3f handle = new Vector3f(rig.bones().get(rig.bone("BOW")).pivot());
        Vector3f hilt = new Vector3f(rig.bones().get(rig.bone("SWORD_R")).pivot());
        Matrix4f[] riding = rig.bones(shot, 0), drawn = rig.bones(shot, 30);
        // He rides on while he shoots: how far he has come by then is not the bow's doing.
        float ridden = Rig.sample(shot.travel(), 30)[2];
        assertTrue(riding[rig.bone("BOW")].transformPosition(new Vector3f(handle)).z < 0, "riding, the bow is behind him");
        assertTrue(riding[rig.bone("SWORD_R")].transformPosition(new Vector3f(hilt)).z > 0, "and the sword in front, in his hand");
        Vector3f held = drawn[rig.bone("BOW")].transformPosition(new Vector3f(handle));
        Vector3f hand = drawn[rig.bone("HAND_L")].transformPosition(new Vector3f(rig.bones().get(rig.bone("SWORD_L")).pivot()));
        assertEquals(0f, held.distance(hand), 1e-3f, "drawn, the bow's handle is in the grip of his left hand");
        assertTrue(held.z - ridden > .3f, "which is out ahead of him");
        assertTrue(drawn[rig.bone("SWORD_R")].transformPosition(new Vector3f(hilt)).z - ridden < -.2f, "and the sword is on his back");
        // The change is undone before the move ends: the bow is back on his back where it hung as he rode in
        // (seen from his chest, since the horse under him is in another beat of its gallop).
        Matrix4f[] end = rig.bones(shot, shot.ticks());
        Vector3f after = new Matrix4f(end[rig.bone("CHEST")]).invert().transformPosition(end[rig.bone("BOW")].transformPosition(new Vector3f(handle)));
        Vector3f before = new Matrix4f(riding[rig.bone("CHEST")]).invert().transformPosition(riding[rig.bone("BOW")].transformPosition(new Vector3f(handle)));
        assertEquals(0f, after.distance(before), 1e-4f);
    }

    @Test
    @DisplayName("The falling star's move calls the star, then lands it with him back on the ground")
    void cataclysmComesBackDown() {
        Rig.Move fall = RadahnModel.FORM.move("cataclysm");
        int meteor = -1, impact = -1, vanish = -1;
        for (Rig.Cue cue : fall.cues()) {
            if (cue.name().equals("meteor")) meteor = cue.tick();
            if (cue.name().equals("impact")) impact = cue.tick();
            if (cue.name().equals("vanish")) vanish = cue.tick();
        }
        assertTrue(vanish > 0 && vanish < meteor && meteor < impact);
        assertTrue(impact - meteor >= 160, "the star is seen coming for eight seconds or more");
        assertTrue(Rig.sample(fall.travel(), vanish)[1] > 10, "he is far up when he goes from sight");
        assertEquals(0f, Rig.sample(fall.travel(), impact)[1], 1e-5f, "and on the ground when the star lands");
    }
}
