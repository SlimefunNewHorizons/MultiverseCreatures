package com.Chagui68.entities.boss.witherstorm;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeamFacingTest {

    @Test
    @DisplayName("The cone faces any direction, including the heads' own -z, with its sides level")
    void facesDirection() {
        Vector3f[] dirs = {new Vector3f(0, 0, -1), new Vector3f(0.3f, -0.4f, -1), new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0), new Vector3f(0, -1, 0), new Vector3f(-0.2f, 0.1f, 0.9f)};
        for (Vector3f d : dirs) {
            Vector3f want = new Vector3f(d).normalize();
            Quaternionf q = WitherStormBody.facing(d);
            Vector3f z = q.transform(new Vector3f(0, 0, 1));
            assertEquals(0, z.distance(want), 1e-4, "z axis for " + d);
            Vector3f x = q.transform(new Vector3f(1, 0, 0));
            assertEquals(0, x.y, 1e-4, "side stays level for " + d);
        }
    }

    @Test
    @DisplayName("A tiny change of aim near -z changes the facing a tiny bit (no flip)")
    void continuous() {
        Quaternionf a = WitherStormBody.facing(new Vector3f(0.001f, 0, -1));
        Quaternionf b = WitherStormBody.facing(new Vector3f(-0.001f, 0, -1));
        float dot = Math.abs(a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w);
        assertEquals(1, dot, 1e-3);
    }
}
