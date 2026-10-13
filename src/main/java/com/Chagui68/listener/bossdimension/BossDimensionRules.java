package com.Chagui68.listener.bossdimension;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.ritual.BossDimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * The survival rules of the boss dimension, for players, each a switch under {@code boss-dimension}:
 * <ul>
 *     <li><b>natural-regeneration</b> off: only potion effects heal (instant health and
 *     regeneration); a full food bar, plugins' heals and every other source give nothing;</li>
 *     <li><b>saturation-effect</b> off: the Saturation effect does nothing, and food only comes back
 *     from eating;</li>
 *     <li><b>flight</b> off: nobody flies, whatever gave them flight (another plugin's /fly, creative
 *     mode) and no elytra opens;</li>
 *     <li><b>fall-damage-multiplier</b>: falls hurt that much more;</li>
 *     <li><b>gravity-multiplier</b>: players fall and jump with that share of the usual gravity
 *     (an attribute modifier, taken off again when they leave).</li>
 * </ul>
 * Spectators are left alone: they cannot touch anything, and they have to fly to move at all.
 */
public final class BossDimensionRules implements Listener {

    /** Ticks between two checks that nobody has been given flight back by another plugin. */
    private static final long CHECK_TICKS = 5L;
    /** The gravity modifier this class puts on players inside, and takes off when they leave. */
    private static final NamespacedKey GRAVITY_KEY = new NamespacedKey("multiversecreatures", "boss_dimension_gravity");

    private final MultiverseCreatures plugin;
    /** The loop that takes away flight another plugin handed out. Held so {@link #stopTasks()} can end it. */
    private BukkitTask ticker;

    public BossDimensionRules(MultiverseCreatures plugin) {
        this.plugin = plugin;
        startTicker();
    }

    private void startTicker() {
        if (ticker != null) ticker.cancel();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(BossDimensionManager.WORLD_NAME);
                if (world == null) return;
                for (Player player : world.getPlayers()) {
                    enforce(player);
                }
            }
        }.runTaskTimer(plugin, CHECK_TICKS, CHECK_TICKS);
    }

    public void stopTasks() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        // The modifier is saved with the player: nobody keeps the dimension's gravity while the plugin is off.
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearGravity(player);
        }
    }

    // ------------------------------------------------------------------ the switches

    private static boolean inside(Entity entity) {
        return entity.getWorld().getName().equals(BossDimensionManager.WORLD_NAME);
    }

    private boolean naturalRegeneration() {
        return plugin.getConfig().getBoolean("boss-dimension.natural-regeneration", false);
    }

    private boolean saturationEffect() {
        return plugin.getConfig().getBoolean("boss-dimension.saturation-effect", false);
    }

    private boolean flight() {
        return plugin.getConfig().getBoolean("boss-dimension.flight", false);
    }

    private double fallDamageMultiplier() {
        return Math.max(0, plugin.getConfig().getDouble("boss-dimension.fall-damage-multiplier", 1.7));
    }

    private double gravityMultiplier() {
        return Math.max(0.05, plugin.getConfig().getDouble("boss-dimension.gravity-multiplier", 0.7));
    }

    /** Puts the dimension's gravity on a player, or puts it right after a reload changed it. */
    private void applyGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attribute.GRAVITY);
        if (gravity == null) return;
        double amount = gravityMultiplier() - 1.0;
        AttributeModifier current = gravity.getModifier(GRAVITY_KEY);
        if (current != null && Math.abs(current.getAmount() - amount) < 1e-9) return;
        if (current != null) gravity.removeModifier(current);
        if (Math.abs(amount) > 1e-9) {
            gravity.addModifier(new AttributeModifier(GRAVITY_KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private static void clearGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attribute.GRAVITY);
        if (gravity == null) return;
        AttributeModifier current = gravity.getModifier(GRAVITY_KEY);
        if (current != null) gravity.removeModifier(current);
    }

    /** Takes away what the rules forbid from a player standing in the dimension now. */
    private void enforce(Player player) {
        applyGravity(player);
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (!flight()) {
            if (player.isFlying()) player.setFlying(false);
            if (player.getAllowFlight()) player.setAllowFlight(false);
            if (player.isGliding()) player.setGliding(false);
        }
        if (!saturationEffect() && player.hasPotionEffect(PotionEffectType.SATURATION)) {
            player.removePotionEffect(PotionEffectType.SATURATION);
        }
    }

    // ------------------------------------------------------------------ healing

    /** Only potion effects heal: an instant health potion, or regeneration (golden apples and totems give it too). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inside(player) || naturalRegeneration()) return;
        switch (event.getRegainReason()) {
            case MAGIC, MAGIC_REGEN -> {
            }
            default -> event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ hunger

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inside(player) || saturationEffect()) return;
        if (event.getNewEffect() != null && event.getNewEffect().getType().equals(PotionEffectType.SATURATION)) {
            event.setCancelled(true);
        }
    }

    /** Food only comes back from something eaten: the Saturation effect and other plugins' feeding give nothing. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inside(player) || saturationEffect()) return;
        if (event.getFoodLevel() > player.getFoodLevel() && event.getItem() == null) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ flight

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!inside(player) || flight() || !event.isFlying() || player.getGameMode() == GameMode.SPECTATOR) return;
        event.setCancelled(true);
        player.setAllowFlight(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inside(player) || flight() || !event.isGliding()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (inside(player)) {
            enforce(player);
            return;
        }
        clearGravity(player);
        if (event.getFrom().getName().equals(BossDimensionManager.WORLD_NAME)
                && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
            // Out again: creative flight comes back with the world that allows it.
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Logged out inside and back somewhere else (the dimension was unloaded): the gravity goes.
        if (inside(event.getPlayer())) enforce(event.getPlayer());
        else clearGravity(event.getPlayer());
    }

    /** Switching to creative hands out flight: it is taken back on the next tick. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!inside(player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && inside(player)) enforce(player);
        });
    }

    // ------------------------------------------------------------------ falls

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player) || !inside(player)) return;
        event.setDamage(event.getDamage() * fallDamageMultiplier());
    }
}
