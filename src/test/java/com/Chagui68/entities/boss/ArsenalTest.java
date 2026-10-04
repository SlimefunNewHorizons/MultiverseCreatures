package com.Chagui68.entities.boss;

import com.Chagui68.utils.MscLimb;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure side of the arsenal attacks: the gestures the bosses cast them with, JackStar's phase
 * pools and their rotation, and the damage caps.
 */
class ArsenalTest {

    private static final String[] GROUPS = {"HEAD", "TORSO_UPPER", "TORSO_LOWER", "LEG_RIGHT", "LEG_LEFT", "ARM_RIGHT", "ARM_LEFT"};

    @ParameterizedTest
    @EnumSource(BossGesture.class)
    void gesturesMoveSmoothlyAndEndAtRest(BossGesture gesture) {
        for (int duration : new int[]{16, 24, 56}) {
            for (String group : GROUPS) {
                Quaternionf previous = gesture.limb(group, 0, duration);
                assertTrue(angle(previous, new Quaternionf()) < 1e-4, gesture + " " + group + " starts away from rest");
                for (int t = 0; t < duration; t++) {
                    Quaternionf q = gesture.limb(group, t, duration);
                    assertTrue(Math.abs(q.lengthSquared() - 1) < 1e-3, gesture + " " + group + " is not a rotation");
                    assertTrue(angle(previous, q) <= 1.2, gesture + " " + group + " jumps at tick " + t);
                    previous = q;
                }
                assertTrue(angle(previous, new Quaternionf()) < 1e-4, gesture + " " + group + " ends away from rest");
            }
            for (int t = 0; t < duration; t++) {
                assertTrue(Math.abs(gesture.bend(true, t, duration)) <= MscLimb.MAX_BEND);
                assertTrue(Math.abs(gesture.bend(false, t, duration)) <= MscLimb.MAX_BEND);
            }
        }
    }

    @Test
    void everyPhaseOwnsFourAttacks() {
        Map<Integer, Integer> perPhase = new java.util.HashMap<>();
        for (JackAbility a : JackAbility.values()) {
            if (!a.destructive()) perPhase.merge(a.phase, 1, Integer::sum);
        }
        assertEquals(23, JackAbility.values().length);
        assertEquals(3, JackAbility.cataclysms().size());
        for (int phase = 1; phase <= 5; phase++) assertEquals(4, perPhase.get(phase), "phase " + phase);
    }

    @Test
    void aPhasePicksItsOwnAttacksMostOfTheTime() {
        Random random = new Random(7);
        for (int phase = 1; phase <= 5; phase++) {
            int own = 0;
            int total = 0;
            for (int i = 0; i < 2000; i++) {
                JackAbility pick = JackAbility.pick(phase, 5, List.of(), random);
                assertNotNull(pick);
                assertTrue(pick.phase == phase || pick.phase == phase - 1, pick + " in phase " + phase);
                if (pick.phase == phase) own++;
                total++;
            }
            assertTrue(own > total * 0.65, "phase " + phase + " cast its own attacks " + own + "/" + total);
        }
    }

    @Test
    void picksNeverRepeatTheLastFewWhileOthersAreInRange() {
        Random random = new Random(11);
        for (int phase = 1; phase <= 5; phase++) {
            ArrayDeque<JackAbility> recent = new ArrayDeque<>();
            Map<JackAbility, Integer> seen = new EnumMap<>(JackAbility.class);
            for (int i = 0; i < 200; i++) {
                JackAbility pick = JackAbility.pick(phase, 5, recent, random);
                assertNotNull(pick);
                // At 5 blocks every phase has at least four attacks in reach, one more than MEMORY.
                assertTrue(!recent.contains(pick), pick + " repeated in phase " + phase);
                recent.addLast(pick);
                while (recent.size() > JackAbility.MEMORY) recent.removeFirst();
                seen.merge(pick, 1, Integer::sum);
            }
            for (JackAbility a : JackAbility.values()) {
                if (a.phase == phase && a.fits(5)) assertTrue(seen.containsKey(a), a + " never cast in phase " + phase);
            }
        }
    }

    @Test
    void outOfReachPicksNothing() {
        assertNull(JackAbility.pick(1, 500, Set.of(), new Random(1)));
    }

    @Test
    void destructiveAttacksStayOutOfThePhasePools() {
        for (int phase = 1; phase <= 5; phase++) {
            for (JackAbility a : JackAbility.pool(phase)) assertTrue(!a.destructive(), a + " in the pool of phase " + phase);
        }
    }

    @Test
    void trueDamageIgnoresArmourButNotAllOfResistance() {
        // No Resistance: the hit lands whole, up to the cap.
        assertEquals(12.0, TrueDamage.dealt(12.0, -1, 0.2, 15.0), 1e-9);
        assertEquals(15.0, TrueDamage.dealt(40.0, -1, 0.2, 15.0), 1e-9);
        // Resistance I (20%) keeps 80% of its effect: 16% of the hit is stopped.
        assertEquals(15.0 * 0.84, TrueDamage.dealt(40.0, 0, 0.2, 15.0), 1e-9);
        // The same numbers the Sentinel's handler produces.
        assertEquals(ArmorStandBoss.penetratingDamage(15.0, 0, 0.2), TrueDamage.dealt(40.0, 0, 0.2, 15.0), 1e-9);
        // A cap of 0 turns the cap off.
        assertEquals(40.0, TrueDamage.dealt(40.0, -1, 0.2, 0), 1e-9);
    }

    @Test
    void damageCapsClampOnlyAboveTheCap() {
        assertEquals(60.0, JackStarBoss.capIncomingDamage(250.0, 60.0));
        assertEquals(42.0, JackStarBoss.capIncomingDamage(42.0, 60.0));
        assertEquals(250.0, JackStarBoss.capIncomingDamage(250.0, 0));
    }

    @Test
    void nixSignatureMovesStillCoverEveryDistance() {
        for (double dist : new double[]{0, 4, 6, 12, 18, 25}) {
            boolean any = false;
            for (NixMoves.Move m : NixMoves.Move.values()) any |= NixBoss.fits(m, dist);
            assertTrue(any, "no signature move at " + dist);
        }
        assertEquals(13, NixAbility.values().length);
        int destructive = 0;
        for (NixAbility a : NixAbility.values()) if (a.destructive) destructive++;
        assertEquals(3, destructive);
    }

    private static double angle(Quaternionf a, Quaternionf b) {
        double dot = Math.abs(a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w);
        return 2 * Math.acos(Math.min(1, dot));
    }
}
