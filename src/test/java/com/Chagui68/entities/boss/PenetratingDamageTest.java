package com.Chagui68.entities.boss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Sentinel's penetrating hits bypass armour outright, but Resistance is only partially pierced:
 * {@code penetrating-resistance-pierce} (default 0.2) is the share of the potion's mitigation the
 * boss ignores, so the potion keeps protecting with the rest.
 *
 * <p>The maths lives in static helpers because the damage path itself needs a live server; the
 * plugin cancels the original event and re-applies {@code OUT_OF_WORLD} damage, which the engine
 * does <em>not</em> reduce by Resistance, so these helpers are the only place the potion is
 * accounted for — hence the exact expectations below. {@link PenetratingHit} chains them the way
 * the handler does and keeps what {@code /msc debug} reports.
 */
class PenetratingDamageTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("Armour, Protection and Resistance are credited back, shield blocking is not, and nothing goes negative")
    void mitigationsAreCreditedBack() {
        // A 22-damage cleave after full netherite + Resistance I: armour -17.6, resistance -0.88.
        assertEquals(22.0, ArmorStandBoss.unmitigated(3.52, -17.6, 0.0, -0.88), EPS);
        assertEquals(15.0, ArmorStandBoss.unmitigated(15.0, 0.0, 0.0, 0.0), EPS, "no mitigation leaves the hit alone");
        assertEquals(2.2, ArmorStandBoss.unmitigated(2.2), EPS, "shield blocking is damage the player kept");
        assertEquals(0.0, ArmorStandBoss.unmitigated(2.0, 5.0), EPS, "clamped at zero");
        assertEquals(0.0, ArmorStandBoss.unmitigated(0.0), EPS);
    }

    @Test
    @DisplayName("Resistance mitigates 20% per level up to immunity, and the boss pierces 20% of it by default")
    void resistanceAndPierce() {
        assertEquals(0.2, ArmorStandBoss.RESISTANCE_PIERCE, EPS);
        double[] mitigation = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
        for (int amplifier = -1; amplifier <= 4; amplifier++) {
            assertEquals(mitigation[amplifier + 1], ArmorStandBoss.resistanceMitigation(amplifier), EPS, "amplifier " + amplifier);
        }
        assertEquals(1.0, ArmorStandBoss.resistanceMitigation(12), EPS, "still capped");

        for (double pierce : new double[]{0.0, 0.2, 0.5, 1.0}) {
            assertEquals(10.0, ArmorStandBoss.penetratingDamage(10.0, -1, pierce), EPS, "no Resistance, pierce " + pierce);
        }
        // Default pierce: Resistance I blocks 16% of a 10 hit, II 32%, V 80%.
        assertEquals(8.4, ArmorStandBoss.penetratingDamage(10.0, 0, 0.2), EPS);
        assertEquals(6.8, ArmorStandBoss.penetratingDamage(10.0, 1, 0.2), EPS);
        assertEquals(2.0, ArmorStandBoss.penetratingDamage(10.0, 4, 0.2), EPS);
        // Pierce 0 respects the potion fully, pierce 1 ignores it, and anything outside is clamped.
        assertEquals(8.0, ArmorStandBoss.penetratingDamage(10.0, 0, 0.0), EPS);
        assertEquals(10.0, ArmorStandBoss.penetratingDamage(10.0, 4, 1.0), EPS);
        assertEquals(ArmorStandBoss.penetratingDamage(10.0, 1, 0.0), ArmorStandBoss.penetratingDamage(10.0, 1, -5.0), EPS);
        assertEquals(ArmorStandBoss.penetratingDamage(10.0, 1, 1.0), ArmorStandBoss.penetratingDamage(10.0, 1, 3.0), EPS);
    }

    @Test
    @DisplayName("A penetrating hit never heals damage back: it stays inside (mitigated, raw]")
    void damageStaysInsideItsBounds() {
        for (int amplifier = -1; amplifier <= 6; amplifier++) {
            for (double pierce = -1.0; pierce <= 2.0; pierce += 0.05) {
                double dealt = ArmorStandBoss.penetratingDamage(12.5, amplifier, pierce);
                assertTrue(dealt <= 12.5 + EPS, "hit grew to " + dealt + " (amp " + amplifier + ")");
                assertTrue(dealt >= 12.5 * (1.0 - ArmorStandBoss.resistanceMitigation(amplifier)) - EPS,
                        "hit fell below the fully mitigatable floor: " + dealt);
            }
        }
    }

    @Test
    @DisplayName("A hit is restored, capped and pierced exactly like the handler, and a fully absorbed one deals nothing")
    void reproducesTheLivePipeline() {
        // Event as the engine reports it for a fully armoured player with Resistance I.
        PenetratingHit hit = PenetratingHit.of(3.52, -17.6, 0.0, -0.88, 0, 0.2, 15.0, 1_000L);
        assertEquals(3.52, hit.eventDamage(), EPS);
        assertEquals(-17.6, hit.armorCreditedBack(), EPS);
        assertEquals(0.0, hit.protectionCreditedBack(), EPS);
        assertEquals(-0.88, hit.resistanceCreditedBack(), EPS);
        assertEquals(22.0, hit.throughArmor(), EPS, "armour must be credited back before the cap");
        assertEquals(15.0, hit.cap(), EPS);
        assertEquals(15.0, hit.raw(), EPS, "the cap applies before Resistance");
        assertEquals(12.6, hit.dealt(), EPS, "Resistance I at 20% pierce: 15 * 0.84");

        for (double pierce : new double[]{0.0, 0.2, 1.0}) {
            assertEquals(15.0, PenetratingHit.of(20.0, 0.0, 0.0, 0.0, -1, pierce, 15.0, 0L).dealt(), EPS,
                    "pierce " + pierce + " changed an unmitigated hit");
        }

        PenetratingHit absorbed = PenetratingHit.of(0.0, 0.0, 0.0, 0.0, 0, 0.2, 15.0, 0L);
        assertEquals(0.0, absorbed.throughArmor(), EPS);
        assertEquals(0.0, absorbed.raw(), EPS);
        assertEquals(0.0, absorbed.dealt(), EPS, "the report must never print NaN or a negative hit");
    }

    @Test
    @DisplayName("The report shows Resistance as a player-facing level and an age that never goes negative")
    void reportFields() {
        assertEquals(0, PenetratingHit.of(10, 0, 0, 0, -1, 0.2, 15, 0L).resistanceLevel());
        assertEquals(1, PenetratingHit.of(10, 0, 0, 0, 0, 0.2, 15, 0L).resistanceLevel());
        assertEquals(3, PenetratingHit.of(10, 0, 0, 0, 2, 0.2, 15, 0L).resistanceLevel());

        PenetratingHit hit = PenetratingHit.of(10, 0, 0, 0, 0, 0.2, 15, 5_000L);
        assertEquals(1_500L, hit.ageMillis(6_500L));
        assertEquals(0L, hit.ageMillis(5_000L));
        assertEquals(0L, hit.ageMillis(4_000L), "a backwards clock must not produce a negative age");
    }
}
