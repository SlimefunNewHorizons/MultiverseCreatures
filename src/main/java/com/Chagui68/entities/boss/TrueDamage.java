package com.Chagui68.entities.boss;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * The bosses' true damage, the same kind the Obsidian Sentinel deals: armour and Protection are
 * ignored, the hit is capped at {@code max-damage-dealt}, and Resistance keeps only part of its
 * effect ({@code pierce} is the share of it that is ignored). It lands as {@code OUT_OF_WORLD}
 * damage, which the engine does not reduce again, with the boss as the cause, so death messages
 * and kill credit still name it.
 *
 * <p>The Sentinel reaches the same numbers from inside its damage event, where the engine has
 * already applied the armour; JackStar, NIX and DIO know the raw amount up front and use
 * {@link #apply} directly.
 */
public final class TrueDamage {

    /** Default ceiling on a single true-damage hit, the Sentinel's {@code max-damage-dealt}. */
    public static final double DEFAULT_CAP = 15.0;
    /** Default share of Resistance's protection that true damage ignores. */
    public static final double DEFAULT_PIERCE = 0.2;

    private TrueDamage() {
    }

    /** The damage a true hit of {@code amount} deals: capped, then Resistance partly pierced. */
    public static double dealt(double amount, int resistanceAmplifier, double pierce, double cap) {
        double raw = cap > 0 ? Math.min(Math.max(0, amount), cap) : Math.max(0, amount);
        return ArmorStandBoss.penetratingDamage(raw, resistanceAmplifier, pierce);
    }

    /**
     * Hits {@code target} with true damage from {@code source}.
     *
     * @return the damage dealt
     */
    public static double apply(Player target, LivingEntity source, double amount, double pierce, double cap) {
        return apply(target, source, amount, pierce, cap, true);
    }

    /**
     * Hits {@code target} with true damage from {@code source}.
     *
     * @param scaled whether the hit grows with the player's investment ({@link BossDamageScaling});
     *               false for damage that is not the boss's own attack, like a share of a hit the
     *               players dealt
     * @return the damage dealt
     */
    public static double apply(Player target, LivingEntity source, double amount, double pierce, double cap, boolean scaled) {
        if (AttackPreview.isActor(source) || target.isDead()) return 0;
        PotionEffect resistance = target.getPotionEffect(PotionEffectType.RESISTANCE);
        // The hit and its cap grow with what the player has invested (BossDamageScaling).
        double scale = scaled ? BossDamageScaling.factor(target, true) : 1.0;
        double dealt = dealt(amount * scale, resistance == null ? -1 : resistance.getAmplifier(), pierce, cap * scale);
        if (dealt <= 0) return 0;
        // Back-to-back boss hits must not be swallowed by the vanilla invulnerability window.
        target.setNoDamageTicks(0);
        target.damage(dealt, DamageSource.builder(DamageType.OUT_OF_WORLD)
                .withDirectEntity(source)
                .withCausingEntity(source)
                .build());
        return dealt;
    }
}
