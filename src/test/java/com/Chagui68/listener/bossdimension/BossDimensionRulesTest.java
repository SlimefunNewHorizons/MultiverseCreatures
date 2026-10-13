package com.Chagui68.listener.bossdimension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** How a fall in the boss dimension is split between ordinary and true damage. */
class BossDimensionRulesTest {

    @Test
    @DisplayName("A fifth of a fall is true damage, the rest is reduced as usual")
    void aFifthOfAFallIsTrue() {
        assertArrayEquals(new double[]{13.6, 3.4}, BossDimensionRules.splitFall(10 * 1.7, 0.2), 1e-9);
    }

    @Test
    @DisplayName("The share is kept between none and all of it, and no fall hurts below zero")
    void theShareIsClamped() {
        assertArrayEquals(new double[]{10, 0}, BossDimensionRules.splitFall(10, -1), 1e-9);
        assertArrayEquals(new double[]{0, 10}, BossDimensionRules.splitFall(10, 3), 1e-9);
        assertArrayEquals(new double[]{0, 0}, BossDimensionRules.splitFall(-4, 0.2), 1e-9);
    }
}
