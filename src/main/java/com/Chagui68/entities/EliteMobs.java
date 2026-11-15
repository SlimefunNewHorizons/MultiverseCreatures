package com.Chagui68.entities;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.utils.MscEntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Elite versions of the plugin's natural mobs: the same creature, bigger, tougher, faster and
 * harder hitting, worth more experience.
 *
 * <p>An elite is not a class of its own. {@code MobHandler} rolls for one beside every natural
 * spawn, at {@code general.elite.spawn-chance-multiplier} (0.7: 30% less) of that entity's own
 * chance, and spawns the entity through {@link #spawn}: whatever the entity's class brings into the
 * world while it runs is promoted once the class has finished dressing it.</p>
 */
public class EliteMobs implements Listener {

    public static final String TAG = "MSC_Elite";

    private final boolean enabled;
    private final double share, healthMultiplier, damageMultiplier, speedMultiplier, scale, experienceMultiplier;
    /** What an entity's class has spawned since {@link #spawn} began, or null outside one. */
    private List<LivingEntity> born;

    public EliteMobs(MultiverseCreatures plugin) {
        var config = plugin.getConfig();
        enabled = config.getBoolean("general.elite.enabled", true);
        share = config.getDouble("general.elite.spawn-chance-multiplier", 0.7);
        healthMultiplier = config.getDouble("general.elite.health-multiplier", 2.5);
        damageMultiplier = config.getDouble("general.elite.damage-multiplier", 1.5);
        speedMultiplier = config.getDouble("general.elite.speed-multiplier", 1.15);
        scale = config.getDouble("general.elite.scale", 1.25);
        experienceMultiplier = config.getDouble("general.elite.experience-multiplier", 3.0);
    }

    /** The share of an entity's own spawn chance its elite spawns with; 0 when elites are off. */
    public double share() {
        return enabled ? share : 0;
    }

    /**
     * What a roll of {@code r} in [0, 1) brings for an entity that spawns with {@code chance}: 2 an
     * elite (the first {@code chance * share} of the range), 1 the entity itself (the next {@code
     * chance}), 0 nothing.
     */
    public static int band(double r, double chance, double share) {
        double elite = chance * Math.max(0, share);
        return r < elite ? 2 : r < elite + chance ? 1 : 0;
    }

    /** Runs {@code spawner}, an entity's own spawn, and promotes what it brought into the world. */
    public void spawn(Runnable spawner) {
        born = new ArrayList<>();
        try {
            spawner.run();
        } finally {
            List<LivingEntity> spawned = born;
            born = null;
            spawned.forEach(this::promote);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (born != null) born.add(event.getEntity());
    }

    private void promote(LivingEntity entity) {
        if (!entity.isValid() || entity.getScoreboardTags().stream().noneMatch(tag -> tag.startsWith("MSC_"))) return;
        entity.addScoreboardTag(TAG);
        MscEntityUtils.initVirtualHealth(entity, MscEntityUtils.getVirtualMaxHealth(entity) * healthMultiplier);
        multiply(entity, Attribute.MOVEMENT_SPEED, speedMultiplier);
        multiply(entity, Attribute.SCALE, scale);
        Component name = entity.customName();
        entity.customName(Component.text("★ Elite ", NamedTextColor.GOLD).append(name != null ? name : Component.text(entity.getName())));
        entity.setCustomNameVisible(true);
    }

    private static void multiply(LivingEntity entity, Attribute attribute, double by) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getBaseValue() * by);
    }

    /** An elite's blows, and whatever it shoots, hit players harder. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) damager = shooter;
        if (damager.getScoreboardTags().contains(TAG)) event.setDamage(event.getDamage() * damageMultiplier);
    }

    /** After the entity's own class has set what it drops. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getScoreboardTags().contains(TAG)) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * experienceMultiplier));
        }
    }
}
