package com.Chagui68.entities.boss;

import com.Chagui68.utils.MscEntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a boss as hard for a group as for one player. Health, damage taken and enrage are used by
 * DIO, Nix, the Sentinel and JackStar; crowding covers every boss hit {@link BossDamageScaling} scales.
 * <ul>
 *     <li><b>health</b> — the virtual max health grows by a share for every extra player within
 *     {@code radius}, keeping the boss's health percentage, so joining mid-fight counts too;</li>
 *     <li><b>damage taken</b> — every extra player in the same radius shrinks the hits the boss takes;</li>
 *     <li><b>crowding</b> — a boss hit on a player grows for every other player standing next to them
 *     (applied in {@link BossDamageScaling#factor(Player, boolean)}), so a group has to spread out;</li>
 *     <li><b>enrage</b> — a boss a group takes too much health off too fast leaves every player nearby
 *     at a sliver of health, throws them away and takes reduced damage for a while.</li>
 * </ul>
 * A solo fight is unchanged: every term counts players beyond the first.
 */
public final class BossGroupScaling {

    public record Settings(boolean enabled, double radius, double healthPerPlayer, double damageReductionPerPlayer,
                           double maxDamageReduction, double crowdRadius, double crowdDamagePerPlayer,
                           int crowdMaxPlayers, boolean enrageEnabled, double enrageThreshold, long enrageWindowMs,
                           long enrageDurationMs, double enrageDamageTaken, int enrageMinPlayers,
                           double enrageHealthLeft, double enrageLaunchDistance) {
        public static final Settings DEFAULT = new Settings(true, 100.0, 0.30, 0.15, 0.75,
                5.0, 0.15, 4, true, 0.10, 8000, 8000, 0.5, 3, 2.0, 20.0);
    }

    /** The configured max health before any group scaling, stored with the virtual health. */
    public static final org.bukkit.NamespacedKey KEY_BASE_MAX_HEALTH =
            new org.bukkit.NamespacedKey("multiversecreatures", "virtual_base_max_health");

    private static Settings settings = Settings.DEFAULT;
    private static final Map<UUID, Rage> rages = new ConcurrentHashMap<>();

    private BossGroupScaling() {
    }

    /** Reads the settings; called on enable and on {@code /msc reload}. */
    public static void load(ConfigurationSection config) {
        Settings d = Settings.DEFAULT;
        if (config == null) {
            settings = d;
            return;
        }
        String p = "boss-balance.group-scaling.";
        settings = new Settings(
                config.getBoolean(p + "enabled", d.enabled()),
                config.getDouble(p + "radius", d.radius()),
                config.getDouble(p + "health-per-player", d.healthPerPlayer()),
                config.getDouble(p + "damage-reduction-per-player", d.damageReductionPerPlayer()),
                config.getDouble(p + "max-damage-reduction", d.maxDamageReduction()),
                config.getDouble(p + "crowd.radius", d.crowdRadius()),
                config.getDouble(p + "crowd.damage-per-player", d.crowdDamagePerPlayer()),
                config.getInt(p + "crowd.max-players", d.crowdMaxPlayers()),
                config.getBoolean(p + "enrage.enabled", d.enrageEnabled()),
                config.getDouble(p + "enrage.health-lost", d.enrageThreshold()),
                Math.round(config.getDouble(p + "enrage.window-seconds", d.enrageWindowMs() / 1000.0) * 1000),
                Math.round(config.getDouble(p + "enrage.duration-seconds", d.enrageDurationMs() / 1000.0) * 1000),
                config.getDouble(p + "enrage.damage-taken", d.enrageDamageTaken()),
                config.getInt(p + "enrage.min-players", d.enrageMinPlayers()),
                config.getDouble(p + "enrage.health-left", d.enrageHealthLeft()),
                config.getDouble(p + "enrage.launch-distance", d.enrageLaunchDistance()));
    }

    // ------------------------------------------------------------------ the maths, testable offline

    /** Max health multiplier for a group of {@code party} players: +share per player beyond the first. */
    public static double healthMultiplier(int party, Settings s) {
        if (!s.enabled()) return 1.0;
        return 1.0 + Math.max(0, s.healthPerPlayer()) * Math.max(0, party - 1);
    }

    /** Multiplier on the damage the boss takes: -share per player beyond the first, floored by the max. */
    public static double damageTakenMultiplier(int party, Settings s) {
        if (!s.enabled()) return 1.0;
        double cut = Math.max(0, s.damageReductionPerPlayer()) * Math.max(0, party - 1);
        return 1.0 - Math.min(Math.max(0, Math.min(1, s.maxDamageReduction())), cut);
    }

    /** Multiplier on a boss hit for a player with {@code others} other players right next to them. */
    public static double crowdMultiplier(int others, Settings s) {
        if (!s.enabled()) return 1.0;
        return 1.0 + Math.max(0, s.crowdDamagePerPlayer()) * Math.min(Math.max(0, others), Math.max(0, s.crowdMaxPlayers()));
    }

    /** The damage a boss lost recently and whether it is enraged. Not thread-safe: main thread only. */
    static final class Rage {
        private record Hit(long at, double fraction) {
        }

        private final Deque<Hit> hits = new ArrayDeque<>();
        private double recent;
        private long enragedUntil;

        boolean enraged(long now) {
            return now < enragedUntil;
        }

        /**
         * Records a hit that cost {@code fraction} of the boss's max health.
         *
         * @return true when this hit made the boss enraged
         */
        boolean record(long now, double fraction, Settings s) {
            while (!hits.isEmpty() && hits.peekFirst().at() <= now - s.enrageWindowMs()) {
                recent -= hits.pollFirst().fraction();
            }
            if (!s.enrageEnabled() || enraged(now)) return false;
            hits.addLast(new Hit(now, fraction));
            recent += fraction;
            if (recent < s.enrageThreshold()) return false;
            enragedUntil = now + s.enrageDurationMs();
            hits.clear();
            recent = 0;
            return true;
        }

        boolean stale(long now, Settings s) {
            return !enraged(now) && (hits.isEmpty() || hits.peekLast().at() <= now - s.enrageWindowMs());
        }
    }

    // ------------------------------------------------------------------ live server

    /** Players who count for the group: alive, not in creative or spectator, within {@code radius}. */
    static int playersNear(Location at, double radius, Player except) {
        if (at.getWorld() == null) return 0;
        double r2 = radius * radius;
        int count = 0;
        for (Player p : at.getWorld().getPlayers()) {
            if (p == except || !MscEntityUtils.isValidTarget(p)) continue;
            if (p.getLocation().distanceSquared(at) <= r2) count++;
        }
        return count;
    }

    /**
     * Scales one hit on a boss for the group fighting it, and the boss's max health with it.
     * Call it with the hit after the boss's own defences and cap.
     *
     * @return the damage to take off the boss's virtual health
     */
    public static double incoming(LivingEntity boss, double damage) {
        Settings s = settings;
        if (!s.enabled() || boss == null || damage <= 0) return damage;
        int party = Math.max(1, playersNear(boss.getLocation(), s.radius(), null));
        rescaleHealth(boss, party, s);
        double scaled = damage * damageTakenMultiplier(party, s);

        long now = System.currentTimeMillis();
        rages.entrySet().removeIf(e -> !e.getKey().equals(boss.getUniqueId()) && e.getValue().stale(now, s));
        Rage rage = rages.computeIfAbsent(boss.getUniqueId(), id -> new Rage());
        if (rage.enraged(now)) return scaled * Math.max(0, s.enrageDamageTaken());
        // Only a group can enrage the boss: a solo player or a pair never records toward it.
        if (party >= s.enrageMinPlayers()
                && rage.record(now, scaled / Math.max(1, MscEntityUtils.getVirtualMaxHealth(boss)), s)) {
            enrage(boss, s);
        }
        return scaled;
    }

    /** The crowding multiplier for a boss hit on {@code victim}. */
    public static double crowd(Player victim) {
        Settings s = settings;
        if (!s.enabled() || victim == null) return 1.0;
        return crowdMultiplier(playersNear(victim.getLocation(), s.crowdRadius(), victim), s);
    }

    /** Sets the virtual max health for {@code party} players, keeping the boss's health percentage. */
    private static void rescaleHealth(LivingEntity boss, int party, Settings s) {
        var pdc = boss.getPersistentDataContainer();
        Double base = pdc.get(KEY_BASE_MAX_HEALTH, PersistentDataType.DOUBLE);
        double max = MscEntityUtils.getVirtualMaxHealth(boss);
        if (base == null || base <= 0) {
            // A boss spawned before this release: its current max is the unscaled one.
            base = max;
            pdc.set(KEY_BASE_MAX_HEALTH, PersistentDataType.DOUBLE, base);
        }
        double target = base * healthMultiplier(party, s);
        if (Math.abs(target - max) < 1e-6) return;
        double health = MscEntityUtils.getVirtualHealth(boss);
        pdc.set(MscEntityUtils.KEY_VIRTUAL_MAX_HEALTH, PersistentDataType.DOUBLE, target);
        MscEntityUtils.setVirtualHealth(boss, health * target / max);
    }

    /**
     * Horizontal launch speed that carries a player about {@code distance} blocks: with vertical
     * speed {@link #LAUNCH_UP} a player stays airborne ~25 ticks, and air drag (0.91 a tick) makes
     * that ~10.3 blocks per unit of speed.
     */
    static double launchSpeed(double distance) {
        return Math.max(0, distance) / 10.3;
    }

    static final double LAUNCH_UP = 0.8;

    /** Players launched by an enrage, mapped to when their fall damage stops being forgiven. */
    private static final Map<UUID, Long> launched = new ConcurrentHashMap<>();

    /** Leaves every player in the radius at a sliver of health and throws them away from the boss. */
    private static void enrage(LivingEntity boss, Settings s) {
        Location feet = boss.getLocation();
        Location at = feet.clone().add(0, 1.5, 0);
        boss.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, at, 20, 1.0, 1.0, 1.0, 0.1);
        boss.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 1);
        boss.getWorld().playSound(at, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.7f);
        Component name = boss.customName() != null ? boss.customName() : Component.text("The boss");
        Component line = Component.text("⚠ ", NamedTextColor.RED).append(name)
                .append(Component.text(" is enraged and throws everyone back!", NamedTextColor.RED));
        long forgiveUntil = System.currentTimeMillis() + 6000;
        double r2 = s.radius() * s.radius();
        for (Player p : boss.getWorld().getPlayers()) {
            if (!MscEntityUtils.isValidTarget(p) || p.getLocation().distanceSquared(feet) > r2) continue;
            p.sendMessage(line);
            p.setAbsorptionAmount(0);
            p.setHealth(Math.min(p.getHealth(), Math.max(0.5, s.enrageHealthLeft())));
            // Thrown out to launch-distance from the boss; anyone already beyond it only hops.
            double away = s.enrageLaunchDistance() - horizontalDistance(p.getLocation(), feet);
            Vector push = MscEntityUtils.horizontalDirection(feet, p.getLocation()).multiply(launchSpeed(away));
            p.setVelocity(push.setY(LAUNCH_UP));
            launched.put(p.getUniqueId(), forgiveUntil);
        }
    }

    private static double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Forgives the landing of an enrage launch: at a sliver of health the fall would kill. */
    public static final class FallGuard implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onFall(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
            Long until = launched.remove(event.getEntity().getUniqueId());
            if (until != null && until > System.currentTimeMillis()) event.setCancelled(true);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            launched.remove(event.getPlayer().getUniqueId());
        }
    }
}
