package com.Chagui68.entities.boss;

import org.bukkit.Location;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaleniaBossTest {

    @Test
    @DisplayName("The carry draws her displays, standing at their anchor, where she stands and facing her way")
    void carryPlacesTheBody() {
        Location anchor = new Location(null, 10, 64, -3, 37f, 0f);
        Location at = new Location(null, 13.5, 65, 1.25, -110f, 0f);
        for (Vector3f p : new Vector3f[]{new Vector3f(), new Vector3f(0, 2.2f, 0), new Vector3f(-.25f, 1.3f, 1.1f)}) {
            Location drawn = MaleniaRig.toWorld(anchor, anchor.getYaw(), MaleniaBoss.carry(anchor, at).transformPosition(new Vector3f(p)));
            Location meant = MaleniaRig.toWorld(at, at.getYaw(), new Vector3f(p));
            assertEquals(meant.getX(), drawn.getX(), 1e-4, "x of " + p);
            assertEquals(meant.getY(), drawn.getY(), 1e-4, "y of " + p);
            assertEquals(meant.getZ(), drawn.getZ(), 1e-4, "z of " + p);
        }
    }
}
