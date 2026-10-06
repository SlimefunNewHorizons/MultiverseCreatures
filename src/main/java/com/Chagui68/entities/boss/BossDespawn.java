package com.Chagui68.entities.boss;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Sends away a boss nobody is fighting: with no player within {@code radius} blocks (the larger
 * {@code large-radius} for colossal bosses) for {@code delay-ticks}, the boss's own tick removes it,
 * so an abandoned boss stops costing the server. Settings in {@code boss-balance.despawn}.
 *
 * <p>Each boss calls {@link #abandoned} from the loop it already runs, at whatever rate; the idle
 * time is measured in server ticks, so the rate does not matter.
 */
public final class BossDespawn {

    /** From {@code boss-balance.despawn}. */
    public record Settings(boolean enabled, double radius, double largeRadius, int delayTicks) {
        public static final Settings DEFAULT = new Settings(true, 50.0, 100.0, 100);
    }

    /** A check further apart than this from the last one starts the count again. */
    static final int GAP_TICKS = 60;

    private static Settings settings = Settings.DEFAULT;
    /** Boss -> {tick it was first seen alone, tick of the last check}. */
    private static final Map<UUID, long[]> idle = new HashMap<>();

    private BossDespawn() {
    }

    /** Reads the settings; called on enable and on {@code /msc reload}. */
    public static void load(ConfigurationSection config) {
        Settings d = Settings.DEFAULT;
        if (config == null) {
            settings = d;
            return;
        }
        String p = "boss-balance.despawn.";
        settings = new Settings(
                config.getBoolean(p + "enabled", d.enabled()),
                Math.max(1.0, config.getDouble(p + "radius", d.radius())),
                Math.max(1.0, config.getDouble(p + "large-radius", d.largeRadius())),
                Math.max(0, config.getInt(p + "delay-ticks", d.delayTicks())));
        idle.clear();
    }

    public static Settings settings() {
        return settings;
    }

    /** {@link #abandoned(Entity, boolean)} for a boss of normal size. */
    public static boolean abandoned(Entity boss) {
        return abandoned(boss, false);
    }

    /**
     * Whether {@code boss} has had no player near for the configured delay and should go now. Once
     * it says yes it forgets the boss, which the caller removes.
     *
     * @param large the boss is colossal, so players count from {@code large-radius}
     */
    public static boolean abandoned(Entity boss, boolean large) {
        if (boss == null || !settings.enabled()) return false;
        UUID id = boss.getUniqueId();
        long now = Bukkit.getCurrentTick();
        if (playerNear(boss.getLocation(), large ? settings.largeRadius() : settings.radius())) {
            idle.remove(id);
            return false;
        }
        prune(now);
        long[] alone = idle.get(id);
        if (alone == null || now - alone[1] > GAP_TICKS) {
            alone = new long[]{now, now};
            idle.put(id, alone);
        }
        alone[1] = now;
        if (!expired(alone[0], now, settings.delayTicks())) return false;
        idle.remove(id);
        return true;
    }

    /** Drops what is known about a boss that left some other way. */
    public static void forget(UUID boss) {
        if (boss != null) idle.remove(boss);
    }

    static boolean expired(long aloneSince, long now, int delayTicks) {
        return now - aloneSince >= delayTicks;
    }

    /** Whether a player who can fight (not a spectator) is within {@code radius} of {@code at}. */
    public static boolean playerNear(Location at, double radius) {
        World world = at == null ? null : at.getWorld();
        if (world == null) return false;
        double r2 = radius * radius;
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || !p.isValid()) continue;
            if (p.getLocation().distanceSquared(at) <= r2) return true;
        }
        return false;
    }

    /** Forgets bosses not checked for a while (killed or unloaded while alone). */
    private static void prune(long now) {
        if (idle.size() < 16) return;
        for (Iterator<long[]> it = idle.values().iterator(); it.hasNext(); ) {
            if (now - it.next()[1] > GAP_TICKS) it.remove();
        }
    }
}
