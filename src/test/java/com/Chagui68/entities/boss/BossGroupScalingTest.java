package com.Chagui68.entities.boss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The group scaling maths: health, damage taken, crowding and enrage. */
class BossGroupScalingTest {

    private static final double EPS = 1e-9;
    private static final BossGroupScaling.Settings S = BossGroupScaling.Settings.DEFAULT;

    @Test
    @DisplayName("A solo fight is unchanged")
    void soloIsUnscaled() {
        assertEquals(1.0, BossGroupScaling.healthMultiplier(1, S), EPS);
        assertEquals(1.0, BossGroupScaling.damageTakenMultiplier(1, S), EPS);
        assertEquals(1.0, BossGroupScaling.crowdMultiplier(0, S), EPS);
    }

    @Test
    @DisplayName("Each extra player adds 30% health and takes 15% off the damage, capped")
    void groupScales() {
        assertEquals(1.6, BossGroupScaling.healthMultiplier(3, S), EPS);
        assertEquals(0.7, BossGroupScaling.damageTakenMultiplier(3, S), EPS);
        assertEquals(0.25, BossGroupScaling.damageTakenMultiplier(20, S), EPS);
    }

    @Test
    @DisplayName("Crowding grows per neighbour up to the max")
    void crowdingCaps() {
        assertEquals(1.30, BossGroupScaling.crowdMultiplier(2, S), EPS);
        assertEquals(1.60, BossGroupScaling.crowdMultiplier(10, S), EPS);
    }

    @Test
    @DisplayName("Losing 10% of its health within 8 seconds enrages the boss for a while")
    void enrage() {
        var rage = new BossGroupScaling.Rage();
        assertFalse(rage.record(0, 0.06, S));
        assertFalse(rage.record(9000, 0.06, S), "the first hit left the window");
        assertTrue(rage.record(10000, 0.05, S));
        assertTrue(rage.enraged(10000 + S.enrageDurationMs() - 1));
        assertFalse(rage.enraged(10000 + S.enrageDurationMs()));
    }

    @Test
    @DisplayName("The launch speed grows with the distance still to cover, never backwards")
    void launchSpeed() {
        assertEquals(20 / 10.3, BossGroupScaling.launchSpeed(20), EPS);
        assertEquals(0.0, BossGroupScaling.launchSpeed(-5), EPS);
    }

    @Test
    @DisplayName("Disabled settings change nothing")
    void disabled() {
        var off = new BossGroupScaling.Settings(false, 100, 0.3, 0.15, 0.75, 5, 0.15, 4, true, 0.10, 8000, 8000, 0.5, 3, 2.0, 20.0);
        assertEquals(1.0, BossGroupScaling.healthMultiplier(5, off), EPS);
        assertEquals(1.0, BossGroupScaling.damageTakenMultiplier(5, off), EPS);
        assertEquals(1.0, BossGroupScaling.crowdMultiplier(4, off), EPS);
    }
}
