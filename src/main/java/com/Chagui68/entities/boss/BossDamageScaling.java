package com.Chagui68.entities.boss;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * How hard a boss hits a given player: its damage scales with what that player has invested, so a
 * boss is as dangerous to an endgame player as to a fresh one. Mahoraga keeps its own adaptation.
 *
 * <p>The factor multiplies both the hit and the per-hit cap, and is built from:
 * <ul>
 *     <li><b>health</b> — max health over 20: the hit takes the same share of a 44-HP player's bar
 *     as of a 20-HP one's (kits and ranks raise max health);</li>
 *     <li><b>rank</b> — DrakesRankup tier and rebirths, which shrink incoming damage by up to ~24%
 *     on their side;</li>
 *     <li><b>effects</b> — Resistance (true damage only ignores part of it) and absorption hearts;</li>
 *     <li><b>gear</b> — armour, toughness and Protection levels, only for bosses whose hits armour
 *     still reduces (true-damage bosses ignore armour already).</li>
 * </ul>
 * The result is clamped to {@code [1, max-factor]}. Everything is read from the player and from
 * DrakesRankup by reflection, so nothing breaks when that plugin is missing.
 */
public final class BossDamageScaling {

    /** What the factor is computed from, read off a player at the moment of the hit. */
    public record Snapshot(double maxHealth, double absorption, int resistanceAmplifier, double armor,
                           double toughness, int protectionLevels, int rankTier, int rebirths) {
    }

    /** The weights, from {@code boss-balance.adaptive-damage}. */
    public record Weights(boolean enabled, double health, double perTier, double perRebirth,
                          double perResistanceLevel, double absorption, double perArmor, double perToughness,
                          double perProtection, double maxFactor) {
        public static final Weights DEFAULT = new Weights(true, 1.0, 0.003, 0.03, 0.15, 1.0, 0.01, 0.015, 0.01, 5.0);
    }

    private static Weights weights = Weights.DEFAULT;

    private BossDamageScaling() {
    }

    /** Reads the weights; called on enable and on {@code /msc reload}. */
    public static void load(ConfigurationSection config) {
        Weights d = Weights.DEFAULT;
        if (config == null) {
            weights = d;
            return;
        }
        String p = "boss-balance.adaptive-damage.";
        weights = new Weights(
                config.getBoolean(p + "enabled", d.enabled()),
                config.getDouble(p + "health-weight", d.health()),
                config.getDouble(p + "per-rank-tier", d.perTier()),
                config.getDouble(p + "per-rebirth", d.perRebirth()),
                config.getDouble(p + "per-resistance-level", d.perResistanceLevel()),
                config.getDouble(p + "absorption-weight", d.absorption()),
                config.getDouble(p + "per-armor-point", d.perArmor()),
                config.getDouble(p + "per-toughness-point", d.perToughness()),
                config.getDouble(p + "per-protection-level", d.perProtection()),
                config.getDouble(p + "max-factor", d.maxFactor()));
    }

    /**
     * The damage multiplier for a hit on a player in {@code s}.
     *
     * @param armorIgnored the hit is true damage, so armour, toughness and Protection do not count
     */
    public static double factor(Snapshot s, Weights w, boolean armorIgnored) {
        if (!w.enabled()) return 1.0;
        double health = Math.pow(Math.max(1.0, s.maxHealth() / 20.0), Math.max(0, w.health()));
        double rank = 1.0 + w.perTier() * Math.max(0, s.rankTier()) + w.perRebirth() * Math.max(0, s.rebirths());
        double effects = 1.0 + (s.resistanceAmplifier() >= 0 ? w.perResistanceLevel() * (s.resistanceAmplifier() + 1) : 0)
                + w.absorption() * Math.max(0, s.absorption()) / Math.max(20.0, s.maxHealth());
        double gear = armorIgnored ? 1.0
                : 1.0 + w.perArmor() * s.armor() + w.perToughness() * s.toughness() + w.perProtection() * s.protectionLevels();
        double total = health * rank * effects * gear;
        return Math.max(1.0, Math.min(Math.max(1.0, w.maxFactor()), total));
    }

    /** The factor for a hit on {@code player} with the configured weights. */
    public static double factor(Player player, boolean armorIgnored) {
        if (player == null || !weights.enabled()) return 1.0;
        return factor(snapshot(player), weights, armorIgnored);
    }

    /** Reads everything the factor needs off a live player. */
    public static Snapshot snapshot(Player player) {
        PotionEffect resistance = player.getPotionEffect(PotionEffectType.RESISTANCE);
        int protection = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece != null && !piece.getType().isAir()) protection += piece.getEnchantmentLevel(Enchantment.PROTECTION);
        }
        int[] rank = rankOf(player.getUniqueId());
        return new Snapshot(attribute(player, Attribute.MAX_HEALTH, 20.0), player.getAbsorptionAmount(),
                resistance == null ? -1 : resistance.getAmplifier(), attribute(player, Attribute.ARMOR, 0),
                attribute(player, Attribute.ARMOR_TOUGHNESS, 0), protection, rank[0], rank[1]);
    }

    private static double attribute(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    // ------------------------------------------------------------------ DrakesRankup, by reflection

    private static Method getRankManager;
    private static Method getPlayerRank;
    private static Method getRebirthCount;
    private static Method getTier;
    private static boolean rankLookupFailed;

    /** The player's DrakesRankup {tier, rebirths}, or {0, 0} when the plugin is not there. */
    static int[] rankOf(UUID player) {
        if (rankLookupFailed) return new int[]{0, 0};
        Plugin rankup = Bukkit.getPluginManager().getPlugin("DrakesRankup");
        if (rankup == null || !rankup.isEnabled()) return new int[]{0, 0};
        try {
            if (getRankManager == null) getRankManager = rankup.getClass().getMethod("getRankManager");
            Object manager = getRankManager.invoke(rankup);
            if (manager == null) return new int[]{0, 0};
            if (getPlayerRank == null) getPlayerRank = manager.getClass().getMethod("getPlayerRank", UUID.class);
            if (getRebirthCount == null) getRebirthCount = manager.getClass().getMethod("getRebirthCount", UUID.class);
            Object rank = getPlayerRank.invoke(manager, player);
            int tier = 0;
            if (rank != null) {
                if (getTier == null) getTier = rank.getClass().getMethod("getTier");
                tier = ((Number) getTier.invoke(rank)).intValue();
            }
            int rebirths = ((Number) getRebirthCount.invoke(manager, player)).intValue();
            return new int[]{tier, rebirths};
        } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException e) {
            rankLookupFailed = true;
            Bukkit.getLogger().warning("[MultiverseCreatures] Could not read DrakesRankup ranks for boss damage scaling: " + e);
            return new int[]{0, 0};
        }
    }
}
