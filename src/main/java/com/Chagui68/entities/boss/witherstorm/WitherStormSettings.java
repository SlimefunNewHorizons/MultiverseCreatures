package com.Chagui68.entities.boss.witherstorm;

import com.Chagui68.entities.boss.TrueDamage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything {@code entities.wither-storm} configures, read once per load so a tick never goes back
 * to the config. Missing keys fall back to the values the section ships with.
 */
record WitherStormSettings(boolean enabled, double health, double maxDamagePerHit, double maxDamageDealt,
                           double pierce, double biteDamage, double tentacleDamage, double reviveDamage,
                           boolean griefBlocks, double aggroRange, boolean attackMobs, int roarMin, int roarMax,
                           int injuryTicks, int escapeTicks, int exposedTicks, double exposedMultiplier,
                           double playDeadThreshold, int playDeadTicks, int maxPerWorld, boolean highDetail,
                           Beam beam, Specials specials, Summons summons,
                           boolean sickness, boolean sickenMobs, int maxSickened, int maxPhantoms,
                           int pointsBlock, int pointsMob, int pointsPlayer, int pointsItem,
                           boolean summonEnabled, Material coreBlock, int formingTicks, float spawnExplosion,
                           List<String> deathMessages, Map<WitherStormForm, Form> forms) {

    /** The knobs of one form. */
    record Form(double scale, double healthMultiplier, int evolveAt, double beamRange, double flyHeight) {
    }

    /**
     * The tractor beam's rhythm: it lights up thin for {@code chargeTicks}, pulls for at most
     * {@code holdTicks}, then goes dark for {@code restTicks}. {@code pullSpeed} is the mod's
     * tractorPullSpeedModifier: blocks per tick, slower the closer the victim gets.
     */
    record Beam(double pullSpeed, int chargeTicks, int holdTicks, int restTicks) {
    }

    /** The storm's big attacks: one every {@code cooldownTicks}, and how hard each one hits. */
    record Specials(boolean enabled, int cooldownTicks, double roarDamage, double debrisDamage,
                    double singularityDamage, double eruptionDamage) {
    }

    /** What it calls to its side: at most {@code max} at once, one call every {@code cooldownTicks}. */
    record Summons(boolean enabled, int cooldownTicks, int max, double symbiontHealth, int hordeSize) {
    }

    Form form(WitherStormForm form) {
        return forms.get(form);
    }

    static WitherStormSettings load(ConfigurationSection c) {
        Map<WitherStormForm, Form> forms = new EnumMap<>(WitherStormForm.class);
        int[] evolve = {200, 600, 1500, 6000, 0};
        for (WitherStormForm f : WitherStormForm.values()) {
            String base = "entities.wither-storm.forms." + f.key() + ".";
            forms.put(f, new Form(
                    clamp(c.getDouble(base + "scale", 1.0), 0.1, 4.0),
                    Math.max(0.1, c.getDouble(base + "health-multiplier", f.healthMultiplier())),
                    Math.max(1, c.getInt(base + "evolve-at", evolve[f.ordinal()] == 0 ? Integer.MAX_VALUE : evolve[f.ordinal()])),
                    Math.max(4, c.getDouble(base + "beam-range", f.beamRange())),
                    Math.max(0, c.getDouble(base + "fly-height", f.flyHeight()))));
        }
        Material core = Material.matchMaterial(c.getString("entities.wither-storm.summon.core-block", "CRYING_OBSIDIAN"));
        if (core == null || !core.isBlock()) {
            core = Material.CRYING_OBSIDIAN;
        }
        int roarMin = Math.max(20, c.getInt("entities.wither-storm.roar-interval-min-ticks", 400));
        Beam beam = new Beam(
                clamp(c.getDouble("entities.wither-storm.beam.pull-speed", 0.2), 0.05, 1.0),
                Math.max(0, c.getInt("entities.wither-storm.beam.charge-ticks", 30)),
                Math.max(20, c.getInt("entities.wither-storm.beam.hold-ticks", 120)),
                Math.max(0, c.getInt("entities.wither-storm.beam.rest-ticks", 100)));
        Specials specials = new Specials(
                c.getBoolean("entities.wither-storm.specials.enabled", true),
                Math.max(100, c.getInt("entities.wither-storm.specials.cooldown-ticks", 500)),
                c.getDouble("entities.wither-storm.specials.roar-damage", 10.0),
                c.getDouble("entities.wither-storm.specials.debris-damage", 8.0),
                c.getDouble("entities.wither-storm.specials.singularity-damage", 16.0),
                c.getDouble("entities.wither-storm.specials.eruption-damage", 12.0));
        Summons summons = new Summons(
                c.getBoolean("entities.wither-storm.summons.enabled", true),
                Math.max(100, c.getInt("entities.wither-storm.summons.cooldown-ticks", 700)),
                Math.max(0, c.getInt("entities.wither-storm.summons.max", 8)),
                Math.max(20, c.getDouble("entities.wither-storm.summons.symbiont-health", 220.0)),
                Math.max(1, c.getInt("entities.wither-storm.summons.horde-size", 4)));
        return new WitherStormSettings(
                c.getBoolean("entities.wither-storm.enabled", true),
                Math.max(1, c.getDouble("entities.wither-storm.health", 800.0)),
                c.getDouble("entities.wither-storm.max-damage-per-hit", 150.0),
                c.getDouble("entities.wither-storm.max-damage-dealt", 20.0),
                c.getDouble("entities.wither-storm.true-damage-pierce", TrueDamage.DEFAULT_PIERCE),
                c.getDouble("entities.wither-storm.bite-damage", 14.0),
                c.getDouble("entities.wither-storm.tentacle-damage", 10.0),
                c.getDouble("entities.wither-storm.revive-damage", 12.0),
                c.getBoolean("entities.wither-storm.grief-blocks", true),
                Math.max(16, c.getDouble("entities.wither-storm.aggro-range", 64.0)),
                c.getBoolean("entities.wither-storm.attack-mobs", true),
                roarMin,
                Math.max(roarMin + 1, c.getInt("entities.wither-storm.roar-interval-max-ticks", 1000)),
                Math.max(20, c.getInt("entities.wither-storm.injury-ticks", 200)),
                Math.max(0, c.getInt("entities.wither-storm.escape-ticks", 800)),
                Math.max(20, c.getInt("entities.wither-storm.exposed-ticks", 160)),
                Math.max(1, c.getDouble("entities.wither-storm.exposed-damage-multiplier", 2.0)),
                clamp(c.getDouble("entities.wither-storm.play-dead-threshold", 0.15), 0, 0.9),
                Math.max(20, c.getInt("entities.wither-storm.play-dead-ticks", 200)),
                Math.max(1, c.getInt("entities.wither-storm.max-per-world", 1)),
                c.getBoolean("entities.wither-storm.high-detail", false),
                beam, specials, summons,
                c.getBoolean("entities.wither-storm.sickness.enabled", true),
                c.getBoolean("entities.wither-storm.sickness.sicken-mobs", true),
                Math.max(0, c.getInt("entities.wither-storm.sickness.max-sickened-mobs", 12)),
                Math.max(0, c.getInt("entities.wither-storm.max-phantoms", 4)),
                Math.max(0, c.getInt("entities.wither-storm.consume-points.block", 1)),
                Math.max(0, c.getInt("entities.wither-storm.consume-points.mob", 6)),
                Math.max(0, c.getInt("entities.wither-storm.consume-points.player", 10)),
                Math.max(0, c.getInt("entities.wither-storm.consume-points.item", 1)),
                c.getBoolean("entities.wither-storm.summon.enabled", true),
                core,
                Math.max(20, c.getInt("entities.wither-storm.summon.forming-ticks", 220)),
                (float) Math.max(0, c.getDouble("entities.wither-storm.summon.explosion-power", 7.0)),
                c.getStringList("entities.wither-storm.death-messages"),
                forms);
    }

    private static double clamp(double v, double min, double max) {
        return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : min;
    }
}
