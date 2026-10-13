package com.Chagui68.entities.monster;

import org.joml.Vector3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The blocks round the Monster, as its planning sees them: which ones a body cannot be in, and how
 * tall each one stands. The game reads them from the world; the model viewer and the tests from a
 * few boxes of blocks, so the same plan runs in both.
 */
public interface MonsterSpace {

    /** True when a body cannot be in the block at x, y, z. */
    boolean solid(int x, int y, int z);

    /** How tall the block at x, y, z stands above its own y: 1 for a full block, 0.875 for a chest. */
    double height(int x, int y, int z);

    /** One number for a block's position, for sets of blocks to leave out of a check. */
    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** The block a {@link #key} stands for: x, y, z. */
    static int[] unkey(long key) {
        int x = (int) ((key >>> 38) & 0x3FFFFFF), z = (int) ((key >>> 12) & 0x3FFFFFF), y = (int) (key & 0xFFF);
        return new int[]{(x << 6) >> 6, (y << 20) >> 20, (z << 6) >> 6};
    }

    /**
     * True when every block the box touches, turned by {@code yaw} radians and standing on
     * {@code anchor}, is free, the ones in {@code ignore} aside. The box is shrunk a little so a hand
     * brushing a wall or the floor does not count.
     */
    static boolean fits(MonsterSpace space, Vector3d anchor, float yaw, float[] box, Set<Long> ignore) {
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        for (int c = 0; c < 4; c++) {
            double x = (c & 1) == 0 ? box[0] : box[3], z = (c & 2) == 0 ? box[2] : box[5];
            double wx = x * cos + z * sin, wz = -x * sin + z * cos;
            minX = Math.min(minX, wx);
            maxX = Math.max(maxX, wx);
            minZ = Math.min(minZ, wz);
            maxZ = Math.max(maxZ, wz);
        }
        int x0 = floor(anchor.x + minX + 0.2), x1 = floor(anchor.x + maxX - 0.2);
        int y0 = floor(anchor.y + box[1] + 0.15), y1 = floor(anchor.y + box[4] - 0.15);
        int z0 = floor(anchor.z + minZ + 0.2), z1 = floor(anchor.z + maxZ - 0.2);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (space.solid(x, y, z) && (ignore == null || !ignore.contains(key(x, y, z)))) return false;
                }
            }
        }
        return true;
    }

    /**
     * The height to stand at in the column round {@code x, z}: the top of the highest block with room
     * above it, searching from {@code top} down to {@code bottom}; {@code null} when there is none.
     */
    static Double floor(MonsterSpace space, double x, double z, int top, int bottom) {
        int bx = floor(x), bz = floor(z);
        for (int y = top; y >= bottom; y--) {
            if (space.solid(bx, y - 1, bz) && !space.solid(bx, y, bz)) {
                return y - 1 + space.height(bx, y - 1, bz);
            }
        }
        return null;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    /**
     * A handful of blocks: a floor under {@code y = 0} (with holes in it), and boxes of blocks on top.
     * What the model viewer draws round a pose, and what the tests plan in.
     */
    final class Blocks implements MonsterSpace {
        private final Map<Long, Double> solid = new HashMap<>();
        private final Set<Long> holes = new HashSet<>();
        private final boolean floor;

        public Blocks(boolean floor) {
            this.floor = floor;
        }

        /** Fills the blocks from (x0, y0, z0) up to, not including, (x1, y1, z1). */
        public Blocks fill(int x0, int y0, int z0, int x1, int y1, int z1) {
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    for (int z = z0; z < z1; z++) {
                        solid.put(key(x, y, z), 1.0);
                        holes.remove(key(x, y, z));
                    }
                }
            }
            return this;
        }

        /** Takes one block out of the floor. */
        public Blocks hole(int x, int y, int z) {
            holes.add(key(x, y, z));
            solid.remove(key(x, y, z));
            return this;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            long k = key(x, y, z);
            if (solid.containsKey(k)) return true;
            return floor && y < 0 && !holes.contains(k);
        }

        @Override
        public double height(int x, int y, int z) {
            return solid(x, y, z) ? 1.0 : 0.0;
        }
    }
}
