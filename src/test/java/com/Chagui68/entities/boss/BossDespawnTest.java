package com.Chagui68.entities.boss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossDespawnTest {

    @AfterEach
    void restore() {
        BossDespawn.load(null);
    }

    @Test
    @DisplayName("A boss alone goes once the delay has passed, not a tick before")
    void expiresAfterDelay() {
        assertFalse(BossDespawn.expired(1000, 1099, 100));
        assertTrue(BossDespawn.expired(1000, 1100, 100));
        assertTrue(BossDespawn.expired(1000, 1000, 0), "0 means as soon as nobody is near");
    }

    @Test
    @DisplayName("Defaults: 50 blocks, 100 for colossal bosses, 5 seconds")
    void defaults() {
        BossDespawn.load(null);
        assertEquals(new BossDespawn.Settings(true, 50.0, 100.0, 100), BossDespawn.settings());
        BossDespawn.load(new YamlConfiguration());
        assertEquals(BossDespawn.Settings.DEFAULT, BossDespawn.settings());
    }

    @Test
    @DisplayName("Settings are read from boss-balance.despawn and clamped")
    void readsConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("boss-balance.despawn.enabled", false);
        config.set("boss-balance.despawn.radius", 0.0);
        config.set("boss-balance.despawn.large-radius", 80.0);
        config.set("boss-balance.despawn.delay-ticks", -5);
        BossDespawn.load(config);
        assertEquals(new BossDespawn.Settings(false, 1.0, 80.0, 0), BossDespawn.settings());
    }

    @Test
    @DisplayName("A disabled rule never removes a boss")
    void disabledKeepsBoss() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("boss-balance.despawn.enabled", false);
        BossDespawn.load(config);
        assertFalse(BossDespawn.abandoned(null, true));
        assertFalse(BossDespawn.playerNear(null, 50));
    }
}
