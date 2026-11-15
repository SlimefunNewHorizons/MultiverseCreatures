package com.Chagui68.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteMobsTest {

    @Test
    @DisplayName("An elite spawns 30% less often than its entity, and never takes the entity's own spawns")
    void eliteRollsBesideTheEntity() {
        double chance = 0.1, share = 0.7;
        assertEquals(2, EliteMobs.band(0.0, chance, share));
        assertEquals(2, EliteMobs.band(0.0699, chance, share), "the elite's band is 0.7 of the entity's chance");
        assertEquals(1, EliteMobs.band(0.07, chance, share));
        assertEquals(1, EliteMobs.band(0.1699, chance, share), "the entity keeps its whole chance");
        assertEquals(0, EliteMobs.band(0.17, chance, share));
    }

    @Test
    @DisplayName("With elites off, or an entity that never spawns, there is no elite")
    void noEliteWithoutAShare() {
        assertEquals(1, EliteMobs.band(0.0, 0.1, 0));
        assertEquals(1, EliteMobs.band(0.0, 0.1, -1), "a negative share is no share");
        assertEquals(0, EliteMobs.band(0.0, 0, 0.7));
    }
}
