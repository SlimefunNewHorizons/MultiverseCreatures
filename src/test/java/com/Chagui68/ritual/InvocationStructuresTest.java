package com.Chagui68.ritual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every summoning structure in one place: the four 5x5 altars (NIX, DIO, Jack Star and the pantheon
 * altar) share one layout, and each structure answers where its centre is, which blocks count as its
 * candles and which blocks it is made of.
 */
class InvocationStructuresTest {

    static Stream<Arguments> fiveByFiveAltars() {
        return Stream.of(
                Arguments.of("NIX", NixInvocationStructure.CANDLE_OFFSETS, NixInvocationStructure.CORNER_OFFSETS),
                Arguments.of("DIO", DioInvocationStructure.CANDLE_OFFSETS, DioInvocationStructure.CORNER_OFFSETS),
                Arguments.of("Jack Star", JackInvocationStructure.CANDLE_OFFSETS, JackInvocationStructure.CORNER_OFFSETS),
                Arguments.of("Pantheon", PantheonAltarStructure.CANDLE_OFFSETS, PantheonAltarStructure.CORNER_OFFSETS));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fiveByFiveAltars")
    @DisplayName("Four candles on the ground right next to the centre, four pillars on the corners of the 5x5")
    void fiveByFiveLayout(String name, int[][] candles, int[][] corners) {
        assertEquals(4, candles.length, name + " must have four candles");
        for (int[] candle : candles) {
            assertEquals(0, candle[1], name + "'s candles sit on the ground layer");
            assertEquals(1, Math.abs(candle[0] - 2) + Math.abs(candle[2] - 2), name + "'s candles sit next to the centre");
        }
        assertEquals(4, corners.length, name + " must have four pillars");
        for (int[] corner : corners) {
            assertTrue((corner[0] == 0 || corner[0] == 4) && (corner[1] == 0 || corner[1] == 4),
                    name + "'s pillars stand on the corners of the 5x5");
        }
    }

    @Test
    @DisplayName("Each structure puts its centre, offering spot and pillar effects where the ritual expects them")
    void centresAndMarkers() {
        Location origin = new Location(null, 10, 50, -20);
        assertLocation(12.5, 50.5, -17.5, NixInvocationStructure.getAnvilLocation(origin), "NIX's anvil");
        assertLocation(12.5, 51.0, -17.5, DioInvocationStructure.getThroneLocation(origin), "DIO's throne");
        assertLocation(12.5, 51.0, -17.5, PantheonAltarStructure.getAltarLocation(origin), "the pantheon altar");

        Location ground = new Location(null, 0, 60, 0);
        List<Location> gallows = NixInvocationStructure.getCornerGallowsLocations(ground);
        assertEquals(4, gallows.size());
        for (Location loc : gallows) assertEquals(61.8, loc.getY(), 0.001, "NIX's gallows glow near skull height");
        List<Location> tops = DioInvocationStructure.getPillarTops(ground);
        assertEquals(4, tops.size());
        for (Location top : tops) assertEquals(62.6, top.getY(), 1e-9, "DIO's light starts above the head");

        assertLocation(103.0, 70.0, 203.0, RitualStructure.getCenterLocation(new Location(null, 100, 70, 200)),
                "the overworld ritual's centre");
        assertEquals(5.0, RitualStructure.getRadius(), 0.001);
        assertLocation(52.0, 10.0, -48.0, BossInvocationStructure.getCenterLocation(new Location(null, 50, 10, -50)),
                "the Sentinel circle's centre");
    }

    @Test
    @DisplayName("Only a structure's candle spots count as its candles: never the centre, a pillar or outside")
    void onlyCandleSpotsAreCandles() {
        Location origin = new Location(null, 100, 10, 100);
        for (int[] spot : new int[][]{{102, 101}, {102, 103}, {101, 102}, {103, 102}}) {
            Location candle = new Location(null, spot[0], 10, spot[1]);
            assertTrue(NixInvocationStructure.containsCandle(origin, candle), "NIX candle " + candle);
            assertTrue(JackInvocationStructure.containsCandle(origin, candle), "Jack candle " + candle);
        }
        for (Location not : List.of(new Location(null, 102, 10, 102), new Location(null, 100, 10, 100),
                new Location(null, 105, 10, 105))) {
            assertFalse(NixInvocationStructure.containsCandle(origin, not), "NIX: " + not);
            assertFalse(JackInvocationStructure.containsCandle(origin, not), "Jack: " + not);
        }

        Location circle = new Location(null, 0, 64, 0);
        for (int x = 1; x <= 3; x++) {
            assertTrue(BossInvocationStructure.containsCandle(circle, new Location(null, x, 64, 0)), "Sentinel edge " + x);
        }
        assertFalse(BossInvocationStructure.containsCandle(circle, new Location(null, 2, 64, 2)), "the Sentinel centre");
        assertFalse(BossInvocationStructure.containsCandle(circle, new Location(null, 5, 64, 5)));
        assertFalse(BossInvocationStructure.containsCandle(circle, new Location(null, -1, 64, 0)));

        Map<Location, Material> ritual = RitualStructure.getCandleLocations();
        assertEquals(12, ritual.size(), "the overworld ritual has twelve candles");
        for (Location loc : ritual.keySet()) {
            assertEquals(1, loc.getBlockY(), "the overworld ritual's candles sit on layer 1");
            assertTrue(loc.getBlockX() != 3 || loc.getBlockZ() != 3, "no candle on the overworld ritual's centre");
        }
    }

    @Test
    @DisplayName("The blocks a structure accepts are the ones its ritual documents, and nothing else")
    void materials() {
        assertTrue(JackInvocationStructure.isValidCore(Material.RESPAWN_ANCHOR));
        assertTrue(JackInvocationStructure.isValidCore(Material.LODESTONE));
        assertFalse(JackInvocationStructure.isValidCore(Material.STONE));
        assertTrue(JackInvocationStructure.isValidCandle(Material.CYAN_CANDLE));
        assertTrue(JackInvocationStructure.isValidCandle(Material.LIGHT_BLUE_CANDLE));
        assertFalse(JackInvocationStructure.isValidCandle(Material.RED_CANDLE));
        assertTrue(JackInvocationStructure.isValidBase(Material.CRYING_OBSIDIAN));
        assertTrue(JackInvocationStructure.isValidBase(Material.POLISHED_BLACKSTONE_BRICKS));
        assertFalse(JackInvocationStructure.isValidBase(Material.DIRT));

        // Any skull or head, standing or on a wall, crowns one of DIO's pillars.
        assertTrue(DioInvocationStructure.isHead(Material.PLAYER_HEAD));
        assertTrue(DioInvocationStructure.isHead(Material.WITHER_SKELETON_WALL_SKULL));
        assertFalse(DioInvocationStructure.isHead(Material.LANTERN));
        assertFalse(DioInvocationStructure.isHead(Material.GOLD_BLOCK));
    }

    private static void assertLocation(double x, double y, double z, Location actual, String what) {
        assertEquals(x, actual.getX(), 0.001, what + " x");
        assertEquals(y, actual.getY(), 0.001, what + " y");
        assertEquals(z, actual.getZ(), 0.001, what + " z");
    }
}
