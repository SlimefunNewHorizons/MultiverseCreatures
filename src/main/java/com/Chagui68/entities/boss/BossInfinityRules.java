package com.Chagui68.entities.boss;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.integration.DrakesBossesIntegration;
import com.Chagui68.integration.SlimefunArmorAdaptation;
import com.Chagui68.entities.Kinger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void beforeCaps(EntityDamageByEntityEvent event) {
        Entity attacker = attacker(event.getDamager());

        // The Infinity sword deals half to every boss but Mahoraga.
        if (isBoss(event.getEntity()) && attacker instanceof Player player
                && SlimefunArmorAdaptation.isInfinitySingularityWeapon(player.getInventory().getItemInMainHand())) {
            double multiplier = plugin.getConfig().getDouble("boss-balance.infinity-weapon-damage-multiplier", 0.5);
            event.setDamage(event.getDamage() * Math.max(0, multiplier));
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
