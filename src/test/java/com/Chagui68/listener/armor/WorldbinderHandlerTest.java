package com.Chagui68.listener.armor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorldbinderHandlerTest {

    @Test
    @DisplayName("No hit takes more than 35% of a wearer's max health; a smaller one lands whole")
    void hitCap() {
        assertEquals(10.5, WorldbinderHandler.capped(22.5, 30.0), 1e-9);
        assertEquals(5.0, WorldbinderHandler.capped(5.0, 30.0), 1e-9);
        assertEquals(7.0, WorldbinderHandler.capped(100.0, 20.0), 1e-9, "three hits at least, even from a full-health one-shot");
    }

    @Test
    @DisplayName("Nobody is Unseen until the ability fires")
    void unseenStartsOff() {
        assertFalse(WorldbinderHandler.isUnseen(null));
    }
}
