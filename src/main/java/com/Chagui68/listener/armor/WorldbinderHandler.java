package com.Chagui68.listener.armor;

import com.Chagui68.items.armor.Worldbinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worldbinder full-set bonus and abilities. Attributes follow {@link ObsidianBastionHandler}; the
 * damage rules run at HIGHEST and skip cancelled hits, so a boss that cancels a hit to re-deal it as
 * true damage is only reduced once, on the true hit.
 */
public class WorldbinderHandler implements Listener {

    /** The set's max-health modifier; the bosses' damage scaling leaves it out (BossDamageScaling). */
    public static final NamespacedKey HP_MOD_KEY =
            new NamespacedKey("multiversecreatures", "worldbinder_health_modifier");
    private static final NamespacedKey SPEED_MOD_KEY =
            new NamespacedKey("multiversecreatures", "worldbinder_speed_modifier");
    private static final NamespacedKey WATER_MOD_KEY =
            new NamespacedKey("multiversecreatures", "worldbinder_water_speed_modifier");

    private static final Set<EntityPotionEffectEvent.Cause> POTION_CAUSES = Set.of(
            EntityPotionEffectEvent.Cause.POTION_DRINK,
            EntityPotionEffectEvent.Cause.POTION_SPLASH,
            EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD,
            EntityPotionEffectEvent.Cause.ARROW);

    private final JavaPlugin plugin;
    private final Set<UUID> hasSetBonus = ConcurrentHashMap.newKeySet();
    private final Set<UUID> reflecting = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> invulnerableUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> deathEvadeReady = new ConcurrentHashMap<>();
    private final Map<UUID, Long> vanishReady = new ConcurrentHashMap<>();
    /** Wearers in Unseen, until when: bosses and mobs do not pick them as a target. */
    private static final Map<UUID, Long> unseenUntil = new ConcurrentHashMap<>();
    /** The set-bonus refresh loop; held so the plugin can end it on shutdown. */
    private BukkitTask ticker;

    public WorldbinderHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                checkSetBonus(p);
            }
        }, 20L, 20L);
    }

    /** Stops the refresh loop; the plugin calls this from its own {@code onDisable}. */
    public void stopTasks() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private static boolean isPiece(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

    private static boolean isFullSet(Player p) {
        return isPiece(p.getInventory().getHelmet(), Worldbinder.HELMET_KEY)
                && isPiece(p.getInventory().getChestplate(), Worldbinder.CHEST_KEY)
                && isPiece(p.getInventory().getLeggings(), Worldbinder.LEGS_KEY)
                && isPiece(p.getInventory().getBoots(), Worldbinder.BOOTS_KEY);
    }

    private boolean wears(Entity entity) {
        return entity instanceof Player p && hasSetBonus.contains(p.getUniqueId());
    }

    /**
     * Whether {@code p} is in Unseen: bosses and mobs must not pick them as a target. Their attacks
     * still hit them, and a boss with nobody else near stays where it is, waiting.
     */
    public static boolean isUnseen(Player p) {
        Long until = p == null ? null : unseenUntil.get(p.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    private static boolean roll(double chance) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /** The entity behind a hit: the shooter of a projectile, or the damager itself. */
    private static Entity attacker(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) return shooter;
        return damager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        removeSetBonus(p);
        if (isFullSet(p)) {
            hasSetBonus.add(p.getUniqueId());
            applySetBonus(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        invulnerableUntil.remove(id);
        unseenUntil.remove(id);
        if (hasSetBonus.remove(id)) {
            removeSetBonus(event.getPlayer());
        }
    }

    // ---- Wearer takes damage ----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWearerDamaged(EntityDamageEvent event) {
        if (!wears(event.getEntity())) return;
        Player p = (Player) event.getEntity();
        UUID id = p.getUniqueId();

        Long until = invulnerableUntil.get(id);
        if (until != null && System.currentTimeMillis() < until) {
            event.setCancelled(true);
            return;
        }

        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, DROWNING -> {
                event.setCancelled(true);
                p.setFireTicks(0);
                return;
            }
            default -> { }
        }

        Entity attacker = event instanceof EntityDamageByEntityEvent byEntity ? attacker(byEntity.getDamager()) : null;

        // Rebound: the hit never lands and the attacker takes it back, 20% harder.
        if (attacker instanceof LivingEntity target && target != p && !reflecting.contains(id)
                && roll(Worldbinder.REFLECT_CHANCE)) {
            double reflected = event.getDamage() * Worldbinder.REFLECT_MULTIPLIER;
            event.setCancelled(true);
            reflecting.add(id);
            try {
                target.damage(reflected, DamageSource.builder(DamageType.THORNS)
                        .withCausingEntity(p).withDirectEntity(p).build());
            } finally {
                reflecting.remove(id);
            }
            p.sendActionBar(Component.text("Rebound! The hit was reflected.", NamedTextColor.GOLD));
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.7f);
            return;
        }

        double damage = event.getDamage();
        // Boss true damage lands as OUT_OF_WORLD with the boss as its cause (see TrueDamage).
        if (event.getDamageSource().getDamageType() == DamageType.OUT_OF_WORLD
                && event.getDamageSource().getCausingEntity() != null) {
            damage *= 1 - Worldbinder.BOSS_TRUE_DAMAGE_REDUCTION;
        }
        damage *= 1 - Worldbinder.DAMAGE_RESISTANCE;
        // Potion efficacy: Resistance shields 20% more (an extra 4% per level on top of the vanilla 20%).
        PotionEffect resistance = p.getPotionEffect(PotionEffectType.RESISTANCE);
        if (resistance != null) {
            damage *= Math.max(0, 1 - Worldbinder.POTION_BONUS * 0.2 * (resistance.getAmplifier() + 1));
        }
        event.setDamage(damage);

        if (attacker != null && attacker != p) {
            // Hit cap: no single hit from a boss, a player or a mob takes more than a share of max health.
            double cap = capped(event.getFinalDamage(), maxHealth(p));
            if (cap < event.getFinalDamage()) event.setDamage(event.getDamage() * cap / event.getFinalDamage());
            tryVanish(p);
        }

        // Defy Death: whatever lands the lethal hit, it is survived on half a heart, then 3 s of invulnerability.
        if (event.getFinalDamage() >= p.getHealth() + p.getAbsorptionAmount()) {
            long now = System.currentTimeMillis();
            if (now >= deathEvadeReady.getOrDefault(id, 0L)) {
                event.setCancelled(true);
                p.setHealth(Math.min(1.0, p.getHealth()));
                deathEvadeReady.put(id, now + Worldbinder.DEATH_EVADE_COOLDOWN_MS);
                invulnerableUntil.put(id, now + Worldbinder.DEATH_EVADE_INVULNERABLE_TICKS * 50);
                p.sendActionBar(Component.text("Defy Death! Invulnerable for 3 seconds.", NamedTextColor.LIGHT_PURPLE));
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.2f);
            }
        }
    }

    /** Unseen: hides the wearer, armour and held items included, from every other player for 5 s. */
    private void tryVanish(Player p) {
        long now = System.currentTimeMillis();
        if (now < vanishReady.getOrDefault(p.getUniqueId(), 0L) || !roll(Worldbinder.VANISH_CHANCE)) return;
        vanishReady.put(p.getUniqueId(), now + Worldbinder.VANISH_COOLDOWN_MS);
        unseenUntil.put(p.getUniqueId(), now + Worldbinder.VANISH_TICKS * 50);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer != p) viewer.hidePlayer(plugin, p);
        }
        // Mobs on them now let go; onTargeted keeps any from picking them again.
        for (Entity near : p.getNearbyEntities(48, 32, 48)) {
            if (near instanceof Mob mob && p.equals(mob.getTarget())) mob.setTarget(null);
        }
        // Mobs do not use the player list, so the effect makes them lose track too.
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) Worldbinder.VANISH_TICKS, 0, false, false));
        p.sendActionBar(Component.text("Unseen! You are invisible for 5 seconds.", NamedTextColor.DARK_AQUA));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.showPlayer(plugin, p);
            }
        }, Worldbinder.VANISH_TICKS);
    }

    /** No mob picks a wearer in Unseen. */
    @EventHandler(ignoreCancelled = true)
    public void onTargeted(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player p && isUnseen(p)) event.setCancelled(true);
    }

    /** The damage a hit of {@code damage} may deal to a wearer with {@code maxHealth}: {@link Worldbinder#HIT_CAP} of it at most. */
    static double capped(double damage, double maxHealth) {
        return Math.min(damage, maxHealth * Worldbinder.HIT_CAP);
    }

    private static double maxHealth(Player p) {
        AttributeInstance health = p.getAttribute(Attribute.MAX_HEALTH);
        return health == null ? 20.0 : health.getValue();
    }

    // ---- Wearer deals damage ----

    /**
     * Lifesteal: every hit the wearer lands on something living, a player, a mob or a boss, heals them.
     * A boss cancels the hit to take it off its own health, so a cancelled hit on one still counts.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWearerLifesteal(EntityDamageByEntityEvent event) {
        Entity attacker = attacker(event.getDamager());
        if (!wears(attacker) || !(event.getEntity() instanceof LivingEntity target) || target == attacker) return;
        if (event.getDamageSource().getDamageType() == DamageType.THORNS) return;
        if (event.isCancelled() && target.getScoreboardTags().stream().noneMatch(tag -> tag.startsWith("MSC_"))) return;
        double heal = event.getFinalDamage() * Worldbinder.LIFESTEAL;
        if (heal > 0) ((Player) attacker).heal(heal, EntityRegainHealthEvent.RegainReason.CUSTOM);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWearerAttacks(EntityDamageByEntityEvent event) {
        Entity attacker = attacker(event.getDamager());
        if (!wears(attacker)) return;
        // Rebound damage is already boosted on its own.
        if (event.getDamageSource().getDamageType() == DamageType.THORNS) return;
        double damage = event.getDamage();
        // Potion efficacy: Strength hits 20% harder (+0.6 per level on top of the vanilla +3).
        PotionEffect strength = ((Player) attacker).getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null && event.getDamager() == attacker) {
            damage += 3 * Worldbinder.POTION_BONUS * (strength.getAmplifier() + 1);
        }
        event.setDamage(damage * (1 + Worldbinder.DAMAGE_BONUS));
    }

    // ---- Potions ----

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!wears(event.getEntity()) || !POTION_CAUSES.contains(event.getCause())) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED
                && event.getAction() != EntityPotionEffectEvent.Action.CHANGED) return;
        PotionEffect effect = event.getNewEffect();
        if (effect == null || effect.isInfinite() || effect.getDuration() <= 1) return;
        event.setCancelled(true);
        // Re-added as a PLUGIN effect, so this handler does not see it twice.
        ((Player) event.getEntity()).addPotionEffect(effect.withDuration(
                (int) Math.round(effect.getDuration() * (1 + Worldbinder.POTION_BONUS))));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!wears(event.getEntity())) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC
                || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN) {
            event.setAmount(event.getAmount() * (1 + Worldbinder.POTION_BONUS));
        }
    }

    // ---- Set bonus ----

    private void checkSetBonus(Player p) {
        boolean nowHas = isFullSet(p);
        UUID id = p.getUniqueId();
        boolean had = hasSetBonus.contains(id);

        if (!nowHas) {
            removeSetBonus(p);
            if (had) {
                hasSetBonus.remove(id);
                p.sendMessage(Component.text("Worldbinder set bonus lost.", NamedTextColor.GRAY));
            }
            return;
        }

        if (!had) {
            hasSetBonus.add(id);
            applySetBonus(p);
            p.sendMessage(Component.text("Worldbinder set bonus activated!", NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void applySetBonus(Player p) {
        AttributeInstance health = p.getAttribute(Attribute.MAX_HEALTH);
        if (health != null && health.getModifier(HP_MOD_KEY) == null) {
            double oldMax = health.getValue();
            health.addModifier(new AttributeModifier(HP_MOD_KEY,
                    Worldbinder.MAX_HEALTH_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            p.setHealth(Math.min(p.getHealth() * (health.getValue() / oldMax), health.getValue()));
        }
        addModifier(p.getAttribute(Attribute.MOVEMENT_SPEED), SPEED_MOD_KEY,
                Worldbinder.SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        addModifier(p.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY), WATER_MOD_KEY,
                Worldbinder.SPEED_BONUS, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void addModifier(AttributeInstance attribute, NamespacedKey key, double amount,
                                    AttributeModifier.Operation operation) {
        if (attribute != null && attribute.getModifier(key) == null) {
            attribute.addModifier(new AttributeModifier(key, amount, operation));
        }
    }

    private void removeSetBonus(Player p) {
        AttributeInstance health = p.getAttribute(Attribute.MAX_HEALTH);
        if (health != null && health.getModifier(HP_MOD_KEY) != null) {
            health.removeModifier(HP_MOD_KEY);
            p.setHealth(Math.min(p.getHealth(), health.getValue()));
        }
        removeModifier(p.getAttribute(Attribute.MOVEMENT_SPEED), SPEED_MOD_KEY);
        removeModifier(p.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY), WATER_MOD_KEY);
    }

    private static void removeModifier(AttributeInstance attribute, NamespacedKey key) {
        if (attribute != null && attribute.getModifier(key) != null) {
            attribute.removeModifier(key);
        }
    }
}
