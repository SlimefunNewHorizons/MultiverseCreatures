package com.Chagui68.entities.boss.fx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The particle budget that keeps boss fights from flooding the players' connections. */
class ParticleBudgetTest {

    @Test
    @DisplayName("Under budget everything is kept; over it, the kept share shrinks in proportion")
    void keepShareIsProportional() {
        assertEquals(1.0, ParticleBudget.keepShare(100, 150), 1e-9);
        assertEquals(0.5, ParticleBudget.keepShare(300, 150), 1e-9);
        assertEquals(0.1, ParticleBudget.keepShare(1500, 150), 1e-9);
    }

    @Test
    @DisplayName("A tick never sends more than twice the budget, and a flood is thinned the tick after")
    void floodsAreThinnedAndCapped() {
        ParticleBudget.load(null);
        int budget = ParticleBudget.DEFAULT_BUDGET;
        // First tick of a flood: the share from the quiet tick before is 1, the hard ceiling holds.
        int sent = 0;
        for (int i = 0; i < budget * 10; i++) if (ParticleBudget.allow(1_000_000, 0.0)) sent++;
        assertEquals(budget * 2, sent);
        // Next tick: the share is budget / demand, so a roll above it is dropped.
        double share = ParticleBudget.keepShare(budget * 10, budget);
        assertTrue(ParticleBudget.allow(1_000_001, share * 0.5));
        assertTrue(!ParticleBudget.allow(1_000_001, share * 1.5));
    }
}
