package com.Chagui68.entities.boss.witherstorm;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitherStormChaseTest {

    @Test
    @DisplayName("Left behind, even the Devourer flies faster than a sprint to catch up; near its spot it keeps its pace")
    void catchesUp() {
        assertEquals(1.0, WitherStorm.catchUp(new Vector(5, 10, 0)), 1e-9, "near its spot, height aside");
        assertEquals(3.0, WitherStorm.catchUp(new Vector(60, 0, 0)), 1e-9, "far behind: three times");
        double sprint = 0.28;
        assertTrue(WitherStormForm.DEVOURER.moveSpeed() * WitherStorm.catchUp(new Vector(40, 0, 0)) > sprint,
                "a Devourer 40 blocks behind outruns a sprinting player");
    }
}
