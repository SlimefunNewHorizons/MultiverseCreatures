package com.Chagui68.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What {@link MscText} builds, read back as the exact {@code §} codes a player's client renders.
 *
 * Every item name and lore line goes through these builders, and a line that drifts by a space, a
 * colour code or a lost bold flag shows immediately in game, so the output is checked mechanically
 * rather than by eye.
 */
class MscTextTest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    @Test
    @DisplayName("title, line, quote, footer, blank and plain produce the exact codes of a name or lore line")
    void eachBuilderSerialisesExactly() {
        assertEquals("§6§lScooby Cookie", legacy(MscText.title(GOLD, "Scooby Cookie")));
        assertEquals("§7A mysterious cookie pulsating", legacy(MscText.line(GRAY, "A mysterious cookie pulsating")));
        assertEquals("§5§o\"Scooby-Dooby-Doo...\"", legacy(MscText.quote(DARK_PURPLE, "\"Scooby-Dooby-Doo...\"")));
        assertEquals("§8✦ §7Mystery Inc.§8 ✦", legacy(MscText.footer("Mystery Inc.")));
        assertEquals("", legacy(MscText.blank()));
        // Vanilla-looking names ("Bone Wall", ...) carry no colour at all.
        assertEquals("Soul Reaper's Scythe", legacy(MscText.plain("Soul Reaper's Scythe")));
    }

    @Test
    @DisplayName("rich() switches colour mid-line without one segment inheriting another's, and rejects bad arguments")
    void richSwitchesColourPerSegment() {
        assertEquals("§e  ▸ §7Resistance VI §8(10 seconds)",
                legacy(MscText.rich(YELLOW, "  ▸ ", GRAY, "Resistance VI ", DARK_GRAY, "(10 seconds)")));
        assertEquals("§7Blocking absorbs §c50% §7of incoming",
                legacy(MscText.rich(GRAY, "Blocking absorbs ", RED, "50% ", GRAY, "of incoming")));
        assertEquals("§6✦ §eSpecial§6 ✦", legacy(MscText.rich(GOLD, "✦ ", YELLOW, "Special", GOLD, " ✦")));
        assertEquals(Component.empty(), MscText.rich());
        assertThrows(IllegalArgumentException.class, () -> MscText.rich(GRAY, "text", RED), "odd argument count");
        assertThrows(IllegalArgumentException.class, () -> MscText.rich("not a colour", "text"), "a pair must start with a colour");
    }

    @Test
    @DisplayName("A full lore list serialises line for line")
    void aFullLoreListSerialisesLineForLine() {
        List<Component> lore = List.of(
                MscText.line(GRAY, "Bouncy and wobbly, yet strangely tasty."),
                MscText.blank(),
                MscText.rich(YELLOW, "  ▸ ", GRAY, "Head Slime Immunity ", DARK_GRAY, "(10 seconds)"),
                MscText.rich(AQUA, "Food: ", WHITE, "4 ", AQUA, "Saturation: ", WHITE, "2.4"),
                MscText.quote(DARK_PURPLE, "\"Slimy yet satisfying!\""),
                MscText.footer("Slime Kingdom"));

        assertEquals(List.of(
                "§7Bouncy and wobbly, yet strangely tasty.",
                "",
                "§e  ▸ §7Head Slime Immunity §8(10 seconds)",
                "§bFood: §f4 §bSaturation: §f2.4",
                "§5§o\"Slimy yet satisfying!\"",
                "§8✦ §7Slime Kingdom§8 ✦"),
                lore.stream().map(MscTextTest::legacy).toList());
    }

    @Test
    @DisplayName("A bold prefix stops at the next colour, and plainText() reads any name back without formatting")
    void boldResetAndPlainText() {
        // Garou's tag: a bold dark-purple prefix, then a light-purple bracket that is NOT bold. Built as
        // siblings; appended to the bold component, the bracket would inherit bold.
        Component garou = Component.empty()
                .append(MscText.title(DARK_PURPLE, "Garou "))
                .append(MscText.line(LIGHT_PURPLE, "[Hero Hunter]"));
        assertEquals("§5§lGarou §d[Hero Hunter]", legacy(garou));
        Component leaked = MscText.title(DARK_PURPLE, "Garou ").append(MscText.line(LIGHT_PURPLE, "[Hero Hunter]"));
        assertNotEquals(legacy(garou), legacy(leaked));

        assertEquals("Garou [Hero Hunter]", MscText.plainText(garou));
        assertEquals("Obsidian Guard: Face me!", MscText.plainText(MscText.rich(DARK_GRAY, "Obsidian Guard: ", GRAY, "Face me!")));
        assertEquals("", MscText.plainText(null), "a nameless entity");
        assertEquals("", MscText.plainText(Component.empty()));
    }
}
