package com.Chagui68.entities.boss;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.integration.DrakesBossesIntegration;
import com.Chagui68.integration.SlimefunArmorAdaptation;
import com.Chagui68.entities.Kinger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * The SlimeTinker Infinity set and sword against every boss but Mahoraga, which has its own
 * adaptation.
 *
 * <ul>
 *     <li>A boss's hit on a player wearing the full Infinity set pierces it, the way Mahoraga's
 *     does: the Infinity trait caps the hit at NORMAL priority, and the damage recorded at LOWEST
 *     is put back at HIGH, before the bosses' own HIGHEST handlers read it. Absorption is
 *     cleared first. Players with DrakesBosses' UltraGod are left alone.</li>
 *     <li>A hit with an Infinity Singularity weapon on a boss deals
 *     {@code boss-balance.infinity-weapon-damage-multiplier} (0.5) of its damage.</li>
 *     <li>A sword's swing on a boss other than Kinger loses 2% for every five levels of Sharpness,
 *     50% at most ({@code boss-balance.sword-sharpness-resistance}), after the boss's cap on a hit.
 *     The Wither Storm takes its hits apart from this event and applies it itself.</li>
 * </ul>
 */
public class BossInfinityRules implements Listener {

    /** Tags of the bosses and of the pieces their bodies are hit through. Mahoraga is not here. */
    static final Set<String> BOSS_TAGS = Set.of(
            ArmorStandBoss.TAG, JackStarBoss.TAG, JackStarBoss.PART_TAG, NixBoss.TAG, NixBoss.PART_TAG,
            DioBoss.TAG, DioBoss.STAND_TAG, Kinger.TAG, Kinger.PART_TAG, "MSC_Garou",
            com.Chagui68.entities.boss.witherstorm.WitherStormBoss.TAG);

    /** Bosses whose hits armour still reduces: their damage scales with the player's gear too. */
    static final Set<String> ARMORED_HIT_TAGS = Set.of(Kinger.TAG, "MSC_Garou");

    private final MultiverseCreatures plugin;
    /** The boss hit being pierced right now and the damage it had before the Infinity cap. */
    private EntityDamageByEntityEvent piercing;
    private double piercingDamage;

    public BossInfinityRules(MultiverseCreatures plugin) {
        this.plugin = plugin;
    }

    static boolean isBoss(Entity entity) {
        if (entity == null) return false;
        for (String tag : entity.getScoreboardTags()) {
            if (BOSS_TAGS.contains(tag)) return true;
        }
        return false;
    }

    /** The entity behind a hit: the shooter of a projectile, or the damager itself. */
    private static Entity attacker(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) return shooter;
        return damager;
    }

    /**
     * Share of a sword's hit a boss takes: {@code boss-balance.sword-sharpness-resistance.per-5-levels}
     * (0.02) less for every five levels of Sharpness, {@code max} (0.5) less at most. 1 for anything
     * that is not a sword.
     */
    public static double swordShare(ItemStack hand, FileConfiguration config) {
        if (hand == null || !hand.getType().name().endsWith("_SWORD")) return 1.0;
        int sharpness = hand.getEnchantmentLevel(Enchantment.SHARPNESS);
        double per = config.getDouble("boss-balance.sword-sharpness-resistance.per-5-levels", 0.02);
        double max = config.getDouble("boss-balance.sword-sharpness-resistance.max", 0.5);
        return 1.0 - Math.max(0, Math.min(max, (sharpness / 5) * per));
    }

    /** The cap on a single hit of the boss behind {@code entity}, from its config; 0 for none. */
    private double capOf(Entity entity) {
        Set<String> tags = entity.getScoreboardTags();
        String key = null;
        if (tags.contains(ArmorStandBoss.TAG)) key = "armor-stand-boss";
        else if (tags.contains(JackStarBoss.TAG) || tags.contains(JackStarBoss.PART_TAG)) key = "jackstar-architect";
        else if (tags.contains(NixBoss.TAG) || tags.contains(NixBoss.PART_TAG)) key = "nix-executioner";
        else if (tags.contains(DioBoss.TAG) || tags.contains(DioBoss.STAND_TAG)) key = "dio-brando";
        else if (tags.contains("MSC_Garou")) key = "garou";
        return key == null ? 0 : plugin.getConfig().getDouble("entities." + key + ".max-damage-per-hit", 0);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beforeCaps(EntityDamageByEntityEvent event) {
        Entity attacker = attacker(event.getDamager());

        if (isBoss(event.getEntity()) && attacker instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            // The Infinity sword deals half to every boss but Mahoraga.
            if (SlimefunArmorAdaptation.isInfinitySingularityWeapon(hand)) {
                double multiplier = plugin.getConfig().getDouble("boss-balance.infinity-weapon-damage-multiplier", 0.5);
                event.setDamage(event.getDamage() * Math.max(0, multiplier));
            }
            // A sharp sword's swing loses its share after the boss's cap: the hit is capped here first,
            // so the boss's own cap leaves it as it is. Kinger keeps whatever a sword deals.
            boolean kinger = event.getEntity().getScoreboardTags().contains(Kinger.TAG)
                    || event.getEntity().getScoreboardTags().contains(Kinger.PART_TAG);
            boolean swing = event.getDamager() == player
                    && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK);
            double share = swordShare(hand, plugin.getConfig());
            if (!kinger && swing && share < 1.0) {
                double cap = capOf(event.getEntity());
                double damage = cap > 0 ? Math.min(event.getDamage(), cap) : event.getDamage();
                event.setDamage(damage * share);
            }
            return;
        }

        // Garou and Kinger hit with plain damage: scale it with the player's investment here. The
        // true-damage bosses scale inside TrueDamage and the Sentinel's handler instead.
        if (event.getEntity() instanceof Player target && attacker != null
                && attacker.getScoreboardTags().stream().anyMatch(ARMORED_HIT_TAGS::contains)) {
            event.setDamage(event.getDamage() * BossDamageScaling.factor(target, false));
        }

        // A boss hitting a player in the full Infinity set: remember the hit before the cap.
        if (event.getEntity() instanceof Player victim && isBoss(attacker)
                && plugin.getConfig().getBoolean("boss-balance.pierce-infinity-armor", true)
                && SlimefunArmorAdaptation.isInfinitySingularityLinksSet(victim)
                && !DrakesBossesIntegration.isUltraGod(victim)) {
            victim.setAbsorptionAmount(0.0D);
            piercing = event;
            piercingDamage = event.getDamage();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void afterInfinityCap(EntityDamageByEntityEvent event) {
        if (event != piercing) return;
        piercing = null;
        if (event.isCancelled()) return;
        event.setDamage(Math.max(event.getDamage(), piercingDamage));
    }
}
