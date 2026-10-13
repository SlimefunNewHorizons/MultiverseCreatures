package com.Chagui68.utils;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Spanish messages of version 3 give way to the English ones, unless an operator edited them. */
class MscRetiredDefaultsTest {

    @Test
    @DisplayName("An untouched Spanish list takes the English default; an edited one is kept")
    void replacesOnlyUntouchedLists() {
        String path = "vampire.sun-death-messages";
        List<String> english = List.of("&6%player% &cturned to ashes under the sun");
        MemoryConfiguration defaults = new MemoryConfiguration();
        defaults.set(path, english);
        defaults.set("entities.dio-brando.death-messages", List.of("english"));

        MemoryConfiguration config = new MemoryConfiguration();
        config.set(path, MscConfigMigration.RETIRED_DEFAULTS.get(path));
        List<String> edited = List.of("&6%player% &cmi propio mensaje");
        config.set("entities.dio-brando.death-messages", edited);

        Set<String> replaced = MscConfigMigration.replaceRetiredDefaults(config, defaults);

        assertEquals(Set.of(path), replaced);
        assertEquals(english, config.getStringList(path));
        assertEquals(edited, config.getStringList("entities.dio-brando.death-messages"));
    }

    @Test
    @DisplayName("An untouched old boss health takes the new default; an edited one is kept")
    void replacesOnlyUntouchedNumbers() {
        MemoryConfiguration defaults = new MemoryConfiguration();
        defaults.set("entities.nix-executioner.health", 2000.0);
        defaults.set("entities.dio-brando.health", 1500.0);
        defaults.set("entities.dio-brando.max-damage-per-hit", 75.0);

        MemoryConfiguration config = new MemoryConfiguration();
        config.set("entities.nix-executioner.health", 450.0);
        config.set("entities.dio-brando.health", 1200.0);
        // YAML reads "100" without a decimal point as an int: it still counts as the old default.
        config.set("entities.dio-brando.max-damage-per-hit", 100);

        Set<String> replaced = MscConfigMigration.replaceRetiredDefaults(config, defaults);

        assertEquals(Set.of("entities.nix-executioner.health", "entities.dio-brando.max-damage-per-hit"), replaced);
        assertEquals(2000.0, config.getDouble("entities.nix-executioner.health"));
        assertEquals(1200.0, config.getDouble("entities.dio-brando.health"));
        assertEquals(75.0, config.getDouble("entities.dio-brando.max-damage-per-hit"));
    }

    @Test
    @DisplayName("JackStar's old 8 s pause between specials becomes the 30% faster one")
    void retiresJackSpecialGap() {
        MemoryConfiguration defaults = new MemoryConfiguration();
        defaults.set("entities.jackstar-architect.special-attack-gap-ticks", 112);
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("entities.jackstar-architect.special-attack-gap-ticks", 160);

        MscConfigMigration.replaceRetiredDefaults(config, defaults);

        assertEquals(112, config.getInt("entities.jackstar-architect.special-attack-gap-ticks"));
    }

    @Test
    @DisplayName("Every boss's old cap on the damage of a single hit becomes 10; an edited one is kept")
    void retiresTheOldHitCaps() {
        MemoryConfiguration defaults = new MemoryConfiguration();
        for (String boss : List.of("armor-stand-boss", "mahoraga", "dio-brando", "nix-executioner",
                "jackstar-architect", "wither-storm")) {
            defaults.set("entities." + boss + ".max-damage-per-hit", 10.0);
        }
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("entities.armor-stand-boss.max-damage-per-hit", 50.0);
        config.set("entities.mahoraga.max-damage-per-hit", 100.0);
        // DIO's cap shipped as 100 first and 75 later: both are retired.
        config.set("entities.dio-brando.max-damage-per-hit", 75.0);
        config.set("entities.nix-executioner.max-damage-per-hit", 100.0);
        config.set("entities.wither-storm.max-damage-per-hit", 150.0);
        config.set("entities.jackstar-architect.max-damage-per-hit", 35.0);

        Set<String> replaced = MscConfigMigration.replaceRetiredDefaults(config, defaults);

        for (String boss : List.of("armor-stand-boss", "mahoraga", "dio-brando", "nix-executioner", "wither-storm")) {
            assertEquals(10.0, config.getDouble("entities." + boss + ".max-damage-per-hit"), boss);
        }
        assertEquals(35.0, config.getDouble("entities.jackstar-architect.max-damage-per-hit"),
                "an operator's own cap stays");
        assertEquals(5, replaced.size());
    }
}
