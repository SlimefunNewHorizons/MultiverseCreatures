package com.Chagui68.wiki;

import com.Chagui68.ritual.JackInvocationStructure;
import com.Chagui68.wiki.StructurePreview.Cell;
import com.Chagui68.wiki.StructurePreview.Part;
import com.Chagui68.wiki.StructurePreview.Structure;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StructurePreviewTest {

    @Test
    @DisplayName("The portal's stairs face its middle and meet at the corners the way Minecraft joins them")
    void portalStairsConnect() {
        Map<Cell, BlockFace> stairs = new HashMap<>();
        for (Part part : StructurePreview.parts(Structure.PORTAL, Material.CRYING_OBSIDIAN)) {
            if (StructurePreview.isStairs(part.material())) {
                stairs.put(new Cell(part.x(), part.y(), part.z()), StructurePreview.inwards(part.x(), part.z(), 7, 7));
            }
        }
        assertEquals(24, stairs.size());
        assertEquals(BlockFace.EAST, stairs.get(new Cell(0, 0, 3)));
        assertEquals(BlockFace.WEST, stairs.get(new Cell(6, 0, 3)));
        assertEquals(BlockFace.SOUTH, stairs.get(new Cell(3, 0, 0)));
        assertEquals(BlockFace.NORTH, stairs.get(new Cell(3, 0, 6)));

        Map<Cell, Stairs.Shape> corners = Map.of(
                new Cell(1, 0, 0), Stairs.Shape.OUTER_LEFT,
                new Cell(5, 0, 0), Stairs.Shape.OUTER_RIGHT,
                new Cell(1, 0, 6), Stairs.Shape.OUTER_RIGHT,
                new Cell(5, 0, 6), Stairs.Shape.OUTER_LEFT);
        for (Cell cell : stairs.keySet()) {
            assertEquals(corners.getOrDefault(cell, Stairs.Shape.STRAIGHT), StructurePreview.shape(stairs, cell),
                    "stair at " + cell);
        }
    }

    @Test
    @DisplayName("Every structure has its blocks in distinct cells and is found by its id")
    void everyStructureIsWellFormed() {
        Set<String> ids = new HashSet<>();
        for (Structure structure : Structure.values()) {
            assertTrue(ids.add(structure.id()), "duplicate id " + structure.id());
            assertSame(structure, Structure.byId(structure.id().toUpperCase()));
            List<Part> parts = StructurePreview.parts(structure, Material.CRYING_OBSIDIAN);
            assertFalse(parts.isEmpty(), structure.id());
            Set<Cell> cells = new HashSet<>();
            for (Part part : parts) {
                assertTrue(cells.add(new Cell(part.x(), part.y(), part.z())), structure.id() + " repeats " + part);
                assertNotEquals(Material.AIR, part.material(), structure.id());
            }
        }
        assertNull(Structure.byId("hand"));
    }

    @Test
    @DisplayName("Jack's preview uses blocks his ritual accepts, and the Wither Storm stands on the configured core")
    void blocksMatchTheRituals() {
        for (Part part : StructurePreview.parts(Structure.JACK, Material.CRYING_OBSIDIAN)) {
            if (part.x() == 2 && part.z() == 2) assertTrue(JackInvocationStructure.isValidCore(part.material()));
            else if (part.y() == 0 && (part.x() == 0 || part.x() == 4)) assertTrue(JackInvocationStructure.isValidBase(part.material()));
            else if (part.y() == 0) assertTrue(JackInvocationStructure.isValidCandle(part.material()));
            else assertEquals(Material.LIGHTNING_ROD, part.material());
        }
        assertTrue(StructurePreview.parts(Structure.WITHERSTORM, Material.OBSIDIAN)
                .contains(new Part(1, 0, 0, Material.OBSIDIAN)));
    }

    @Test
    @DisplayName("Looking east or west swaps x and z, so the Wither's T stands across the view")
    void turnsForEastAndWest() {
        List<Part> parts = List.of(new Part(2, 1, 0, Material.SOUL_SAND));
        assertEquals(parts, StructurePreview.turnedFor(parts, BlockFace.NORTH));
        assertEquals(List.of(new Part(0, 1, 2, Material.SOUL_SAND)), StructurePreview.turnedFor(parts, BlockFace.EAST));
    }
}
