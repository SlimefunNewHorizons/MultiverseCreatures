package com.Chagui68.entities.boss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boss balance against invested players: the adaptive damage factor and which bosses the
 * Infinity rules cover.
 */
class BossBalanceTest {

    private static final double EPS = 1e-9;
    private static final BossDamageScaling.Weights W = BossDamageScaling.Weights.DEFAULT;

    /** A plain 20-HP player with nothing on. */
    private static BossDamageScaling.Snapshot fresh() {
        return new BossDamageScaling.Snapshot(20, 0, -1, 0, 0, 0, 0, 0);
    }

    @Test
    @DisplayName("A fresh player takes the boss's damage unchanged")
    void freshPlayerIsUnscaled() {
        assertEquals(1.0, BossDamageScaling.factor(fresh(), W, true), EPS);
        assertEquals(1.0, BossDamageScaling.factor(fresh(), W, false), EPS);
    }

    @Test
    @DisplayName("Extra max health scales the hit so it takes the same share of the bar")
    void healthScalesProportionally() {
        var doubled = new BossDamageScaling.Snapshot(40, 0, -1, 0, 0, 0, 0, 0);
        assertEquals(2.0, BossDamageScaling.factor(doubled, W, true), EPS);
        // Less than 20 HP never makes a boss weaker.
        var frail = new BossDamageScaling.Snapshot(10, 0, -1, 0, 0, 0, 0, 0);
        assertEquals(1.0, BossDamageScaling.factor(frail, W, true), EPS);
    }

    @Test
    @DisplayName("Rank tier, rebirths, Resistance and absorption each raise the factor")
    void upgradesRaiseTheFactor() {
        double base = BossDamageScaling.factor(fresh(), W, true);
        assertTrue(BossDamageScaling.factor(new BossDamageScaling.Snapshot(20, 0, -1, 0, 0, 0, 100, 0), W, true) > base);
        assertTrue(BossDamageScaling.factor(new BossDamageScaling.Snapshot(20, 0, -1, 0, 0, 0, 0, 5), W, true) > base);
        assertEquals(1.30, BossDamageScaling.factor(new BossDamageScaling.Snapshot(20, 0, 1, 0, 0, 0, 0, 0), W, true), EPS);
        assertEquals(1.20, BossDamageScaling.factor(new BossDamageScaling.Snapshot(20, 4, -1, 0, 0, 0, 0, 0), W, true), EPS);
    }

    @Test
    @DisplayName("Gear only counts for hits that armour still reduces")
    void gearOnlyForArmouredHits() {
        var geared = new BossDamageScaling.Snapshot(20, 0, -1, 24, 15, 28, 0, 0);
        assertEquals(1.0, BossDamageScaling.factor(geared, W, true), EPS);
        assertEquals(1 + 0.24 + 0.225 + 0.28, BossDamageScaling.factor(geared, W, false), EPS);
    }

    @Test
    @DisplayName("The factor is capped and can be switched off")
    void cappedAndSwitchable() {
        var maxed = new BossDamageScaling.Snapshot(200, 40, 4, 30, 20, 40, 100, 20);
        assertEquals(W.maxFactor(), BossDamageScaling.factor(maxed, W, false), EPS);
        var off = new BossDamageScaling.Weights(false, 1, 1, 1, 1, 1, 1, 1, 1, 5);
        assertEquals(1.0, BossDamageScaling.factor(maxed, off, false), EPS);
    }

    @Test
    @DisplayName("The Infinity rules cover every boss but Mahoraga")
    void infinityRulesSkipMahoraga() {
        assertTrue(BossInfinityRules.BOSS_TAGS.contains(ArmorStandBoss.TAG));
        assertTrue(BossInfinityRules.BOSS_TAGS.contains(JackStarBoss.TAG));
        assertTrue(BossInfinityRules.BOSS_TAGS.contains(NixBoss.TAG));
        assertTrue(BossInfinityRules.BOSS_TAGS.contains(DioBoss.TAG));
        assertFalse(BossInfinityRules.BOSS_TAGS.contains("MSC_Mahoraga"));
        assertTrue(BossInfinityRules.BOSS_TAGS.containsAll(BossInfinityRules.ARMORED_HIT_TAGS));
    }
}
