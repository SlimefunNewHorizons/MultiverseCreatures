package com.Chagui68.entities.boss;

import org.bukkit.boss.BarColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the Sentinel's phase ladder down.
 *
 * The thresholds, the bar colours and the boss bar titles come from one list, so the ladder has to
 * keep reproducing the comparison chain the fight was balanced on, or the fight changes difficulty.
 */
class SentinelPhaseTest {

    private static final String BOSS_NAME = "THE OBSIDIAN SENTINEL";
    private static final int DEFAULT_PHASES = 5;

    /** The ladder the boss used to hardcode, kept as the oracle the configurable one must match. */
    private static int oldComparisonChain(double healthPercent) {
        if (healthPercent > 0.8) return 0;
        if (healthPercent > 0.6) return 1;
        if (healthPercent > 0.4) return 2;
        if (healthPercent > 0.2) return 3;
        return 4;
    }

    @Test
    @DisplayName("The default ladder has five phases, matches the old chain everywhere, only grows and reaches every phase")
    void theLadder() {
        List<Double> ladder = SentinelPhase.DEFAULT_THRESHOLDS;
        assertEquals(DEFAULT_PHASES, SentinelPhase.phaseCount(ladder));
        int previous = 0;
        boolean[] seen = new boolean[DEFAULT_PHASES];
        for (int step = 1050; step >= -50; step--) {
            double health = step / 1000.0;
            int phase = SentinelPhase.phaseFor(health, ladder);
            assertEquals(oldComparisonChain(health), phase, "phase differs at health " + health);
            assertTrue(phase >= previous, "phase went backwards at health " + health);
            previous = phase;
            seen[phase] = true;
        }
        for (int phase = 0; phase < DEFAULT_PHASES; phase++) assertTrue(seen[phase], "phase " + phase + " is unreachable");
        assertEquals(1, SentinelPhase.phaseFor(0.8, ladder), "at exactly 80% the fight has moved on");
        assertEquals(4, SentinelPhase.phaseFor(-0.5, ladder), "a nonsense fraction is clamped");
        assertEquals(0, SentinelPhase.phaseFor(2.0, ladder), "overheal keeps the opening phase");

        List<Double> halves = SentinelPhase.sanitizeThresholds(List.of(0.5));
        assertEquals(2, SentinelPhase.phaseCount(halves), "a custom ladder rescales the fight");
        assertEquals(0, SentinelPhase.phaseFor(0.9, halves));
        assertEquals(1, SentinelPhase.phaseFor(0.5, halves));
    }

    @Test
    @DisplayName("Thresholds are sorted, deduplicated and cleaned, fall back to the defaults when unusable, and stay immutable")
    void sanitising() {
        assertEquals(List.of(0.9, 0.5), SentinelPhase.sanitizeThresholds(
                Arrays.asList(0.5, 1.5, -2.0, 0.9, Double.NaN, Double.POSITIVE_INFINITY)));
        assertEquals(List.of(0.5), SentinelPhase.sanitizeThresholds(List.of(0.5, 0.5, 0.5)), "no zero-width phase");
        assertEquals(List.of(1.0), SentinelPhase.sanitizeThresholds(List.of(1.0)), "a phase may start at full health");
        assertEquals(SentinelPhase.DEFAULT_THRESHOLDS, SentinelPhase.sanitizeThresholds(null));
        assertEquals(SentinelPhase.DEFAULT_THRESHOLDS, SentinelPhase.sanitizeThresholds(List.of()));
        assertEquals(SentinelPhase.DEFAULT_THRESHOLDS, SentinelPhase.sanitizeThresholds(List.of(0.0, 1.4, -1.0)),
                "a typo in config.yml must not leave the boss with a single phase");
        List<Double> sanitised = SentinelPhase.sanitizeThresholds(List.of(0.5, 0.25));
        assertThrows(UnsupportedOperationException.class, () -> sanitised.add(0.1));
    }

    @Test
    @DisplayName("The boss bar shows one square per phase in the phase's colour, whatever the ladder's length")
    void barTitleAndColour() {
        assertEquals("§4§lTHE OBSIDIAN SENTINEL §c■■■■■",
                SentinelPhase.title(BOSS_NAME, 0, DEFAULT_PHASES));
        assertEquals("§e§lTHE OBSIDIAN SENTINEL §c■■■§7■■",
                SentinelPhase.title(BOSS_NAME, 2, DEFAULT_PHASES));
        assertEquals("§9§lTHE OBSIDIAN SENTINEL §c■§7■■■■",
                SentinelPhase.title(BOSS_NAME, 4, DEFAULT_PHASES));
        assertEquals("§e§lOBSIDIAN §c■§7■■", SentinelPhase.title("OBSIDIAN", 2, 3));
        assertEquals("§4§lOBSIDIAN §c■", SentinelPhase.title("OBSIDIAN", 0, 1));
        for (int pastTheLadder : new int[]{3, 4, 99}) {
            assertEquals(3, SentinelPhase.title("OBSIDIAN", pastTheLadder, 3).chars().filter(c -> c == '■').count(),
                    "phase " + pastTheLadder + " of three must still show three squares");
        }

        BarColor[] colours = {BarColor.RED, BarColor.RED, BarColor.YELLOW, BarColor.GREEN, BarColor.BLUE};
        for (int phase = 0; phase < colours.length; phase++) assertEquals(colours[phase], SentinelPhase.barColor(phase));
        assertEquals(BarColor.BLUE, SentinelPhase.barColor(9), "a longer ladder reuses the last colour");
        assertEquals(BarColor.RED, SentinelPhase.barColor(-1));
    }
}
