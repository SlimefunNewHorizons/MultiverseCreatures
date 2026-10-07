package com.Chagui68.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The health and hitbox numbers a boss is spawned with, kept inside what the server accepts. */
class MscEntityUtilsHealthTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("A boss's health is clamped to the server's attribute cap and never to zero")
    void safeHealth() {
        assertEquals(1024.0, MscEntityUtils.calculateSafeHealth(3200.0, 1024.0), EPS, "Sentinel on a default server");
        assertEquals(3200.0, MscEntityUtils.calculateSafeHealth(3200.0, 5000.0), EPS, "a server with raised limits");
        assertEquals(450.0, MscEntityUtils.calculateSafeHealth(450.0, 1024.0), EPS, "under the cap is untouched");
        assertEquals(0.1, MscEntityUtils.calculateSafeHealth(0.0, 1024.0), EPS, "no instant death on spawn");
        assertEquals(0.1, MscEntityUtils.calculateSafeHealth(-10.0, 1024.0), EPS);
    }

    @Test
    @DisplayName("Virtual health drives the bar from 0 to 1 and scales onto the physical health, dying at zero")
    void virtualHealth() {
        assertEquals(0.5, MscEntityUtils.calculateVirtualProgress(1600.0, 3200.0), EPS);
        assertEquals(1.0, MscEntityUtils.calculateVirtualProgress(4000.0, 3200.0), EPS, "overheal is a full bar");
        assertEquals(0.0, MscEntityUtils.calculateVirtualProgress(-50.0, 3200.0), EPS);
        assertEquals(0.0, MscEntityUtils.calculateVirtualProgress(0.0, 0.0), EPS, "no division by zero");

        assertEquals(1024.0, MscEntityUtils.calculateScaledPhysicalHealth(3200.0, 3200.0, 1024.0), EPS);
        assertEquals(512.0, MscEntityUtils.calculateScaledPhysicalHealth(1600.0, 3200.0, 1024.0), EPS);
        assertEquals(0.32, MscEntityUtils.calculateScaledPhysicalHealth(1.0, 3200.0, 1024.0), EPS, "one point left still lives");
        assertEquals(0.0, MscEntityUtils.calculateScaledPhysicalHealth(0.0, 3200.0, 1024.0), EPS);
        assertEquals(0.0, MscEntityUtils.calculateScaledPhysicalHealth(-10.0, 3200.0, 1024.0), EPS);
    }

    @Test
    @DisplayName("Every requested hitbox scale lands inside the usable range, and a broken value falls back to 1")
    void hitboxScale() {
        double[][] cases = {{1.0, 1.0}, {7.5, 7.5}, {0.25, 0.25}, {8.0, 8.0}, {0.0, 0.25}, {-3.0, 0.25}, {99.0, 8.0},
                {Double.NaN, 1.0}, {Double.POSITIVE_INFINITY, 1.0}, {Double.NEGATIVE_INFINITY, 1.0}};
        for (double[] c : cases) {
            assertEquals(c[1], MscEntityUtils.clampHitboxScale(c[0]), EPS, "scale " + c[0]);
        }
    }
}
