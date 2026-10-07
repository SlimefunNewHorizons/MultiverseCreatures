package com.Chagui68.commands;

import com.Chagui68.testsupport.ProjectPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tables behind {@code /msc spawn}, {@code /msc give} and {@code /msc attack}, and the page maths
 * of their help menus. Each table is the one source the executor, the help and tab completion read,
 * so what is checked is that they agree: one lowercase name per thing, every name the help shows
 * actually works, the retired shortcuts are gone, and every page of every menu has a title and lines.
 */
class CommandCatalogueTest {

    // ------------------------------------------------------------------ /msc spawn

    @Test
    @DisplayName("spawn: one name per kind, every documented kind listed once, Jack kept out of the help")
    void spawnTable() {
        List<String> names = SpawnCatalogue.names();
        assertOneNameEach(names, SpawnCatalogue.types().size());
        for (SpawnCatalogue.Type type : SpawnCatalogue.types()) {
            assertEquals(type.id(), SpawnCatalogue.find(type.id().toUpperCase()).id(), "case-insensitive lookup");
            long listed = helpOf(SpawnCatalogue.pages(), SpawnCatalogue::helpLines).stream()
                    .filter(line -> line.startsWith(" &e• " + type.id() + " ")).count();
            assertEquals(type.documented() ? 1 : 0, listed, type.id() + " is listed " + listed + " times");
        }
        assertFalse(SpawnCatalogue.find("jack").documented(), "Jack is spawnable but stays out of the help");
        assertNull(SpawnCatalogue.find("not-a-mob"));
        assertNull(SpawnCatalogue.find(null));
        assertNull(SpawnCatalogue.find(""));
        for (String retired : List.of("army", "rogue", "flame", "frost", "void", "storm", "bone", "venom",
                "obsidian", "reaper", "chaos", "ender", "armorstandboss", "executioner", "nixelverdugo",
                "diobrando", "theworld", "tormentawither", "archer", "standarrow", "jackstar", "arquitecto")) {
            assertNull(SpawnCatalogue.find(retired), "retired alias still resolves: " + retired);
        }
    }

    @Test
    @DisplayName("spawn: every kind builds its success and failure message")
    void spawnMessages() {
        assertEquals("NIX - The Executioner", SpawnCatalogue.find("nix").spawnedName());
        assertEquals("trap", SpawnCatalogue.find("zombietrap").failureName());
        for (SpawnCatalogue.Type type : SpawnCatalogue.types()) {
            assertNotNull(type.spawner(), type.id() + " has no spawner");
            assertFalse(type.spawnedName().isBlank(), type.id() + " has no spawned name");
            assertFalse(type.failureName().isBlank(), type.id() + " has no failure name");
        }
    }

    // ------------------------------------------------------------------ /msc give

    @Test
    @DisplayName("give: one name per item, every item the help names is givable, the old shortcuts are gone")
    void giveTable() {
        List<String> names = GiveCatalogue.names();
        assertOneNameEach(names, GiveCatalogue.entries().size());
        for (GiveCatalogue.Entry entry : GiveCatalogue.entries()) {
            assertNotNull(entry.template(), entry.name() + " has no item factory");
        }
        // Help lines read "a &8/ &eb &8- description": every name before the dash must be givable.
        for (String line : helpOf(GiveCatalogue.pages(), GiveCatalogue::helpLines)) {
            for (String token : line.split(" &8- ", 2)[0].split(" &8/ &e")) {
                String named = (token.startsWith("&e") ? token.substring(2) : token).trim();
                assertTrue(names.contains(named), "the help advertises '" + named + "', which is not givable");
            }
        }
        assertNull(GiveCatalogue.find("not-an-item"));
        assertNull(GiveCatalogue.find(null));
        for (String retired : List.of("cookie", "sword", "crown", "lantern", "star", "mold", "claws", "mine",
                "heart", "pullshot", "scythe", "dagger", "grimoire", "wheel", "aegis", "mantle", "kernel",
                "warrant", "deathwarrant", "condensed", "blood", "elixir", "edge", "guillotine", "deployer")) {
            assertFalse(names.contains(retired), "retired alias still completes: " + retired);
        }
    }

    // ------------------------------------------------------------------ /msc attack

    @Test
    @DisplayName("attack: every registered attack plus the boss's mechanics, one name each, on real pages")
    void attackTable() {
        List<String> all = AttackCatalogue.commandNames();
        assertOneNameEach(all, AttackCatalogue.names().size() + AttackCatalogue.mechanics().size());
        assertEquals(all.size(), helpOf(AttackCatalogue.pages(), AttackCatalogue::helpLines).size(),
                "every help line is one name the command accepts");
        for (AttackCatalogue.Entry entry : AttackCatalogue.entries()) {
            assertTrue(entry.page() >= 1 && entry.page() <= AttackCatalogue.pages(), entry.name() + " is on a missing page");
            assertTrue(entry.description().startsWith("&7"), "descriptions share the &7 body colour: " + entry.name());
        }

        String boss = ProjectPaths.read(ProjectPaths.source("com", "Chagui68", "entities", "boss", "ArmorStandBoss.java"));
        Matcher block = Pattern.compile("COMMAND_MECHANICS = List\\.of\\(([^)]*)\\)").matcher(boss);
        assertTrue(block.find(), "COMMAND_MECHANICS is gone from ArmorStandBoss");
        List<String> declared = new ArrayList<>();
        Matcher name = Pattern.compile("\"([a-z]+)\"").matcher(block.group(1));
        while (name.find()) declared.add(name.group(1));
        assertEquals(declared, AttackCatalogue.mechanics(), "the help and the boss list different mechanics");

        for (String retired : List.of("slam", "call", "rain", "takeoff", "descend", "barrier", "heal",
                "doombeamer", "resetpose", "crossbarrage")) {
            assertFalse(all.contains(retired), "retired alias still accepted: " + retired);
        }
    }

    // ------------------------------------------------------------------ help pages

    @Test
    @DisplayName("Page numbers clamp into range and the page count covers every line with at least one page")
    void pageMaths() {
        int[][] clamp = {{-5, 3, 1}, {0, 3, 1}, {2, 3, 2}, {3, 3, 3}, {99, 3, 3}};
        for (int[] c : clamp) assertEquals(c[2], CommandMenu.clampPage(c[0], c[1]), Arrays.toString(c));
        int[][] count = {{0, 12, 1}, {12, 12, 1}, {13, 12, 2}, {25, 12, 3}, {19, 19, 1}};
        for (int[] c : count) assertEquals(c[2], CommandMenu.pageCount(c[0], c[1]), Arrays.toString(c));

        List<String> lines = IntStream.range(0, 37).mapToObj(i -> "l" + i).toList();
        int pages = CommandMenu.pageCount(lines.size(), CommandMenu.LINES_PER_PAGE);
        List<String> seen = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            List<String> slice = CommandMenu.pageSlice(lines, page, CommandMenu.LINES_PER_PAGE);
            assertFalse(slice.isEmpty(), "page " + page + " is empty");
            assertTrue(seen.stream().noneMatch(slice::contains), "page " + page + " repeats a line");
            seen.addAll(slice);
        }
        assertEquals(lines, seen, "the pages show every line exactly once");
        assertEquals(CommandMenu.pageSlice(lines, pages, 12), CommandMenu.pageSlice(lines, 99, 12), "past the end is the last page");
        assertEquals(CommandMenu.pageSlice(lines, 1, 12), CommandMenu.pageSlice(lines, -3, 12), "before the start is the first");
    }

    @Test
    @DisplayName("Every page of every menu has a title and at least one line")
    void everyPageHasATitleAndLines() {
        assertPages("spawn", SpawnCatalogue.pages(), SpawnCatalogue::pageTitle, SpawnCatalogue::helpLines);
        assertPages("give", GiveCatalogue.pages(), GiveCatalogue::pageTitle, GiveCatalogue::helpLines);
        assertPages("attack", AttackCatalogue.pages(), AttackCatalogue::pageTitle, AttackCatalogue::helpLines);
    }

    // ------------------------------------------------------------------ helpers

    private static void assertOneNameEach(List<String> names, int expected) {
        assertEquals(expected, names.size(), "one name per entry");
        assertEquals(names.size(), new HashSet<>(names).size(), "a name is declared twice in " + names);
        for (String name : names) {
            assertFalse(name.isBlank(), "blank name");
            assertEquals(name.toLowerCase(), name, "names stay lowercase for tab completion: " + name);
            assertFalse(name.contains(" "), "names are single words: " + name);
        }
    }

    private static List<String> helpOf(int pages, java.util.function.IntFunction<List<String>> lines) {
        List<String> all = new ArrayList<>();
        for (int page = 1; page <= pages; page++) all.addAll(lines.apply(page));
        return all;
    }

    private static void assertPages(String menu, int pages, java.util.function.IntFunction<String> title,
                                    java.util.function.IntFunction<List<String>> lines) {
        for (int page = 1; page <= pages; page++) {
            assertFalse(title.apply(page).isBlank(), menu + " page " + page + " has no title");
            assertFalse(lines.apply(page).isEmpty(), menu + " page " + page + " has no entries");
        }
    }
}
