package com.Chagui68.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the world-allowlist semantics down.
 *
 * config.yml documents an empty {@code general.allowed-creature-worlds} as "every world", but the
 * implementation used to treat it as a hardcoded allowlist of the five default world names. On a
 * server with a custom world name that meant no conversions at all, and worse, the periodic
 * recount deleted any MSC creature it found there.
 */
class MscWorldPolicyTest {

    @ParameterizedTest(name = "Empty allowlist allows \"{0}\"")
    @ValueSource(strings = {"world", "survival", "creative_plots", "evento_temporal"})
    @DisplayName("Verify an empty allowlist means every world, as config.yml documents")
    void testEmptyAllowlistAllowsEveryWorld(String worldName) {
        assertTrue(MscWorldPolicy.isAllowed(List.of(), worldName));
        assertTrue(MscWorldPolicy.isAllowed(null, worldName));
    }


    @Test
    @DisplayName("Verify a populated allowlist restricts and normalizes world names")
    void testAllowlistRestrictsWorlds() {
        List<String> configured = List.of("  SURVIVAL ", "world_nether");

        assertTrue(MscWorldPolicy.isAllowed(configured, "survival"), "Case and blanks must not matter");
        assertTrue(MscWorldPolicy.isAllowed(configured, "WORLD_NETHER"));
        assertFalse(MscWorldPolicy.isAllowed(configured, "creative_plots"));
    }

    @ParameterizedTest(name = "Plugin world \"{0}\" stays allowed")
    @ValueSource(strings = {"boss_dimension", "drakes_bosses", "DRAKES_BOSSES"})
    @DisplayName("Verify the plugin's own worlds always stay allowed")
    void testInternalWorldsAreAlwaysAllowed(String worldName) {
        // Even behind an explicit allowlist that does not mention them, the boss dimension the
        // plugin creates itself must keep working.
        assertTrue(MscWorldPolicy.isAllowed(List.of("survival"), worldName));
    }


    @Test
    @DisplayName("Verify a null world name is never allowed")
    void testNullWorldIsRejected() {
        assertFalse(MscWorldPolicy.isAllowed(List.of(), null));
        assertFalse(MscWorldPolicy.isAllowed(List.of("survival"), null));
        assertFalse(MscWorldPolicy.isAllowed(List.of("survival"), ""));
    }

    @ParameterizedTest(name = "Denylist blocks \"{0}\" even though the allowlist is empty")
    @ValueSource(strings = {"clasico", "CLASICO", " clasico ", "clasico_nether", "clasico_the_end"})
    @DisplayName("Verify the denylist overrides the documented 'empty allowlist = every world'")
    void testDenylistBlocksWorldsDespiteEmptyAllowlist(String worldName) {
        List<String> disallowed = List.of("clasico", "clasico_nether", "clasico_the_end");
        assertFalse(MscWorldPolicy.isAllowed(List.of(), disallowed, worldName),
                "An empty allowlist must not let the denylist through");
    }

    @Test
    @DisplayName("Verify the denylist does not affect worlds it does not name")
    void testDenylistOnlyBlocksNamedWorlds() {
        List<String> disallowed = List.of("clasico");
        assertTrue(MscWorldPolicy.isAllowed(List.of(), disallowed, "survival"));
        assertTrue(MscWorldPolicy.isAllowed(List.of(), disallowed, "world_nether"));
    }

    @Test
    @DisplayName("Verify the denylist is also checked against a populated allowlist")
    void testDenylistWinsOverAllowlist() {
        // A world named in both lists is blocked: the denylist is checked first, so a stale or
        // conflicting allowlist entry can never resurrect a world an operator explicitly excluded.
        List<String> allowed = List.of("clasico", "survival");
        List<String> disallowed = List.of("clasico");
        assertFalse(MscWorldPolicy.isAllowed(allowed, disallowed, "clasico"));
        assertTrue(MscWorldPolicy.isAllowed(allowed, disallowed, "survival"));
    }

    @ParameterizedTest(name = "Plugin world \"{0}\" stays allowed even when denylisted")
    @ValueSource(strings = {"boss_dimension", "drakes_bosses", "DRAKES_BOSSES"})
    @DisplayName("Verify INTERNAL_WORLDS override the denylist")
    void testInternalWorldsOverrideDenylist(String worldName) {
        List<String> disallowed = List.of("boss_dimension", "drakes_bosses");
        assertTrue(MscWorldPolicy.isAllowed(List.of(), disallowed, worldName));
    }

    @Test
    @DisplayName("Verify the two-arg overload keeps its old behaviour (no denylist)")
    void testTwoArgOverloadHasNoDenylist() {
        assertTrue(MscWorldPolicy.isAllowed(List.of(), "clasico"));
    }
}
