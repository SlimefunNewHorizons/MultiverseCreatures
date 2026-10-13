package com.Chagui68.listener.bossdimension;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.integration.DrakesBossesIntegration;
import com.Chagui68.ritual.BossDimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

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
 *     <li><b>fall-true-damage-share</b>: that share of a fall's damage is true damage, as
 *     Mahoraga deals it: armour, Protection, Feather Falling, Resistance and other plugins' caps do
 *     not stop it;</li>
 *     <li><b>gravity-multiplier</b>: players fall and jump with that share of the usual gravity
 *     (an attribute modifier, taken off again when they leave);</li>
 *     <li><b>resistance-effectiveness</b>: the Resistance effect blocks that share of what it
 *     usually would (0.9 = 10% weaker), on ordinary hits and on the bosses' true hits alike.</li>
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
    /** The player taking a fall's true damage right now, and how much: see {@link #pierce}. */
    private UUID piercing;
    private double piercingAmount;

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

    /**
     * Share of what the Resistance effect blocks that it still blocks for {@code entity}:
     * {@code boss-dimension.resistance-effectiveness} (0.9) inside the dimension, all of it elsewhere.
     */
    public static double resistanceKept(Entity entity) {
        if (entity == null || !inside(entity)) return 1.0;
        double kept = org.bukkit.plugin.java.JavaPlugin.getPlugin(MultiverseCreatures.class).getConfig()
                .getDouble("boss-dimension.resistance-effectiveness", 0.9);
        return Math.max(0, Math.min(1, kept));
    }

    /**
     * The share of Resistance a boss's true hit on {@code target} ignores: {@code pierce} as it is,
     * and inside the dimension the weaker Resistance on top of it.
     */
    public static double resistancePierce(Entity target, double pierce) {
        double ignored = Math.max(0, Math.min(1, pierce));
        return 1 - (1 - ignored) * resistanceKept(target);
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

    private double fallTrueShare() {
        return Math.max(0, Math.min(1, plugin.getConfig().getDouble("boss-dimension.fall-true-damage-share", 0.2)));
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

    // ------------------------------------------------------------------ resistance

    /**
     * Resistance blocks less inside: its share of an ordinary hit shrinks to
     * {@link #resistanceKept}. The bosses' true hits weaken it themselves ({@link #resistancePierce}),
     * and the Sentinel credits the modifier back to pierce it, so its hits are left alone here.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onResistance(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inside(player)) return;
        if (!event.isApplicable(EntityDamageEvent.DamageModifier.RESISTANCE)) return;
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity
                && byEntity.getDamager().getScoreboardTags().contains(com.Chagui68.entities.boss.ArmorStandBoss.TAG)) {
            return;
        }
        double blocked = event.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE);
        if (blocked >= 0) return;
        event.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, blocked * resistanceKept(player));
    }

    // ------------------------------------------------------------------ falls

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player) || !inside(player)) return;
        double[] split = splitFall(event.getDamage() * fallDamageMultiplier(), fallTrueShare());
        event.setDamage(split[0]);
        if (split[1] > 0) {
            // A tick later: the fall itself has just made the player invulnerable for this one.
            double amount = split[1];
            Bukkit.getScheduler().runTask(plugin, () -> pierce(player, amount));
        }
    }

    /**
     * A fall's damage as {@code [ordinary, true]}: the share {@code trueShare} (0 to 1) of it that
     * nothing stops, and the rest that armour and enchantments reduce as usual.
     */
    static double[] splitFall(double damage, double trueShare) {
        double share = Math.max(0, Math.min(1, trueShare));
        double whole = Math.max(0, damage);
        return new double[]{whole * (1 - share), whole * share};
    }

    /**
     * The true part of a fall, dealt the way Mahoraga pierces armour: {@code OUT_OF_WORLD} damage,
     * which the engine takes through armour, Protection and Resistance, and restored at the end of
     * the event if another plugin capped or cancelled it. Like Mahoraga it leaves DrakesBosses'
     * UltraGod alone and never sets health directly.
     */
    private void pierce(Player player, double amount) {
        if (!player.isOnline() || player.isDead() || !inside(player)) return;
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) return;
        if (DrakesBossesIntegration.isUltraGod(player)) return;
        player.setNoDamageTicks(0);
        piercing = player.getUniqueId();
        piercingAmount = amount;
        try {
            player.damage(amount, DamageSource.builder(DamageType.OUT_OF_WORLD).build());
        } finally {
            piercing = null;
        }
    }

    /** Last word on the true part of a fall: whatever another plugin did to it, it lands whole. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPierce(EntityDamageEvent event) {
        if (piercing == null || !event.getEntity().getUniqueId().equals(piercing)) return;
        event.setCancelled(false);
        event.setDamage(Math.max(event.getDamage(), piercingAmount));
    }

    /** Killed by a fall's true part: the death message says the fall, not the void. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPierceDeath(PlayerDeathEvent event) {
        if (piercing == null || !event.getPlayer().getUniqueId().equals(piercing)) return;
        event.deathMessage(net.kyori.adventure.text.Component.text(event.getPlayer().getName() + " hit the ground too hard"));
    }
}
