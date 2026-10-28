package com.Chagui68.ritual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;

import java.util.ArrayList;
import java.util.List;

/**
 * The Scarlet Bloom: the 5x5 structure that summons Malenia in the Boss Dimension.
 *
 * <pre>
 *   P . . . P      P = corner pillar: crimson hyphae, a shroomlight on top
 *   . . c . .      c = magenta candle
 *   . c W c .      W = central nether wart block (the rot's heart)
 *   . . c . .
 *   P . . . P
 * </pre>
 *
 * Lighting the four candles wakes the scarlet rot; dropping a torchflower, the Scarlet Aeonia, on the
 * heart makes it bloom and Malenia rises out of it.
 */
public class MaleniaInvocationStructure {

    public static final Material CORE = Material.NETHER_WART_BLOCK;
    public static final Material CANDLE = Material.MAGENTA_CANDLE;
    public static final Material PILLAR = Material.CRIMSON_HYPHAE;
    public static final Material PILLAR_TOP = Material.SHROOMLIGHT;
    /** The offering: a torchflower stands in for the Scarlet Aeonia. */
    public static final Material OFFERING = Material.TORCHFLOWER;

    /** Candles around the heart at (2, 0, 2). */
    public static final int[][] CANDLE_OFFSETS = {
            {2, 0, 1},
            {2, 0, 3},
            {1, 0, 2},
            {3, 0, 2}
    };

    /** The four corner pillars. */
    public static final int[][] CORNER_OFFSETS = {
            {0, 0},
            {4, 0},
            {0, 4},
            {4, 4}
    };

    public static boolean isStructureComplete(Location origin) {
        if (origin.getWorld() == null) return false;
        if (getBlockAt(origin, 2, 0, 2).getType() != CORE) return false;
        for (int[] offset : CANDLE_OFFSETS) {
            if (getBlockAt(origin, offset[0], offset[1], offset[2]).getType() != CANDLE) return false;
        }
        for (int[] corner : CORNER_OFFSETS) {
            if (getBlockAt(origin, corner[0], 0, corner[1]).getType() != PILLAR) return false;
            if (getBlockAt(origin, corner[0], 1, corner[1]).getType() != PILLAR_TOP) return false;
        }
        return true;
    }

    public static boolean areAllCandlesLit(Location origin) {
        if (origin.getWorld() == null) return false;
        for (int[] offset : CANDLE_OFFSETS) {
            Block candle = getBlockAt(origin, offset[0], offset[1], offset[2]);
            if (candle.getType() != CANDLE) return false;
            if (candle.getBlockData() instanceof Candle data && !data.isLit()) return false;
        }
        return true;
    }

    public static void extinguishAllCandles(Location origin) {
        if (origin.getWorld() == null) return;
        for (int[] offset : CANDLE_OFFSETS) {
            Block candle = getBlockAt(origin, offset[0], offset[1], offset[2]);
            if (candle.getType() == CANDLE && candle.getBlockData() instanceof Candle data) {
                data.setLit(false);
                candle.setBlockData(data);
            }
        }
    }

    public static boolean containsCandle(Location origin, Location candle) {
        for (int[] offset : CANDLE_OFFSETS) {
            if (origin.getBlockX() + offset[0] == candle.getBlockX()
                    && origin.getBlockY() + offset[1] == candle.getBlockY()
                    && origin.getBlockZ() + offset[2] == candle.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    /** The centre of the heart's top face, where the flower is offered and Malenia rises. */
    public static Location getHeartLocation(Location origin) {
        return origin.clone().add(2.5, 1.0, 2.5);
    }

    /** Just above each corner's shroomlight, where the rot's threads come from. */
    public static List<Location> getPillarTops(Location origin) {
        List<Location> list = new ArrayList<>();
        for (int[] corner : CORNER_OFFSETS) {
            list.add(origin.clone().add(corner[0] + 0.5, 2.2, corner[1] + 0.5));
        }
        return list;
    }

    /** How close to the heart the flower has to land. */
    public static double getRadius() {
        return 3.0;
    }

    private static Block getBlockAt(Location origin, int x, int y, int z) {
        return origin.getWorld().getBlockAt(origin.getBlockX() + x, origin.getBlockY() + y, origin.getBlockZ() + z);
    }
}
