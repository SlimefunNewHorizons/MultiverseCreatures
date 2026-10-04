package com.Chagui68.entities.boss.attack;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A summoning rite: the Sentinel raises its spear, a rune circle burns under every spawn point, and
 * when the columns of light come down the minions step out of them.
 *
 * <p>Minions are bound to the fight. They carry the summon tag (so they never hurt the boss or each
 * other), count towards the cap on live minions, and dissolve when their time runs out or the
 * Sentinel falls — the choreography keeps playing after the cast to drive their abilities, and its
 * end is what dismisses them.
 */
public abstract class SummoningAttack extends ChoreographedAttack {

    /** The tag every summoned minion carries; friendly fire between tagged entities is off. */
    public static final String SUMMON_TAG = "MSC_ArmorBossSummoned";
    /** Ticks of casting before the minions appear. */
    protected static final int RITE = 30;

    protected SummoningAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return !instance.isFlying && !instance.hoverBarrageActive;
    }

    /** Where the minions appear. */
    protected abstract List<Vector> points(Stage stage);

    /** Spawns the minions of one point. */
    protected abstract List<LivingEntity> summon(World world, Stage stage, Vector at, int index);

    /** How long the minions stay, in ticks after they appear. */
    protected int lifetime() {
        return 900;
    }

    /** The colour of the runes and the columns of light. */
    protected Color color() {
        return Palette.EMBER;
    }

    /** A minion's own ability, run every tick while it lives; {@code tick} counts from its arrival. */
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
    }

    @Override
    public Timeline choreograph(Stage stage) {
        List<Vector> points = points(stage);
        if (points.isEmpty()) return null;
        BossInstance instance = stage.instance();
        Timeline t = new Timeline();
        List<LivingEntity> minions = new ArrayList<>();

        rite(t, stage, points, color());
        t.at(RITE, () -> stage.onServer(world -> {
            if (instance != null && instance.preview) return;
            for (int i = 0; i < points.size(); i++) {
                for (LivingEntity minion : summon(world, stage, stage.onGround(points.get(i)), i)) {
                    bind(instance, minion);
                    minions.add(minion);
                }
            }
        }));
        sustain(t, stage, minions, RITE + 1, lifetime());
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return RITE + 18;
    }

    // ------------------------------------------------------------------ the rite

    /** The cast: spear raised, a burning rune circle under each point, a column of light at the end. */
    protected static void rite(Timeline t, Stage stage, List<Vector> points, Color color) {
        Fx fx = stage.fx();
        tweenTo(t, stage, 0, RITE, Poses.SPEAR_RAISED.withLeftArm(-120, 0, -40), Ease.OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.EVOKER_PREPARE_SUMMON, 3f, 0.6f));
        t.span(0, RITE, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (Vector point : points) {
                Vector floor = stage.onGround(point).add(new Vector(0, 0.1, 0));
                fx.draw(Shapes.star(floor, 2.4 * p + 0.3, 5, 2, tick * 0.05, 0.4, Shapes.FLAT_U, Shapes.FLAT_V), fx.dust(color, 1.3f));
                fx.ring(floor, 2.8 * p + 0.3, 0.6, -tick * 0.04, fx.dust(Palette.GOLD, 1.0f));
                if (tick % 6 == 0) fx.gather(floor.clone().add(new Vector(0, 1, 0)), 2.5, 4, color, 8);
            }
            fx.line(stage.body().spearTip(), stage.body().spearTip().add(new Vector(0, 8, 0)), 0.6, fx.dust(color, 1.2f).sometimes(0.5));
        });
        t.at(RITE, () -> {
            for (Vector point : points) {
                Vector floor = stage.onGround(point);
                fx.line(floor, floor.clone().add(new Vector(0, 22, 0)), 0.4, fx.dust(Palette.HOLY, 2.0f));
                fx.flash(floor.clone().add(new Vector(0, 1, 0)), color);
                fx.flatBurst(floor, Particle.FLAME, 24, 0.4);
            }
            fx.sound(stage.feet(), Sfx.EVOKER_CAST, 2.5f, 0.7f);
            fx.sound(stage.feet(), Sfx.WITHER_SPAWN, 0.8f, 1.6f);
        });
        recover(t, stage, RITE + 4, RITE + 18, Poses.GUARD);
    }

    /**
     * Keeps the minions playing their abilities for {@code lifetime} ticks from {@code start}; the
     * choreography ends early once they are all dead, and its end dismisses whoever is left.
     */
    protected void sustain(Timeline t, Stage stage, List<LivingEntity> minions, int start, int lifetime) {
        Fx fx = stage.fx();
        t.span(start, start + lifetime, (tick, p) -> {
            boolean anyAlive = false;
            for (int i = 0; i < minions.size(); i++) {
                LivingEntity minion = minions.get(i);
                if (!minion.isValid() || minion.isDead()) continue;
                anyAlive = true;
                if (tick % 40 == 0) retarget(minion);
                behave(stage, minion, i, tick);
            }
            if (!anyAlive && tick > 5) t.stop();
        });
        t.onFinish(() -> {
            for (LivingEntity minion : minions) {
                if (!minion.isValid() || minion.isDead()) continue;
                Vector at = minion.getLocation().toVector().add(new Vector(0, 1, 0));
                fx.cloud(Particle.LARGE_SMOKE, at, 20, 0.5, 0.05);
                fx.cloud(Particle.PORTAL, at, 30, 0.6, 0.3);
                minion.remove();
            }
        });
    }

    /** Tags a minion as the boss's and counts it towards the cap on live minions. */
    protected static void bind(BossInstance instance, LivingEntity minion) {
        minion.addScoreboardTag(SUMMON_TAG);
        minion.setRemoveWhenFarAway(false);
        if (instance != null) instance.summons.add(minion.getUniqueId());
        retarget(minion);
    }

    /** Points a minion at the nearest player within forty blocks. */
    protected static void retarget(LivingEntity minion) {
        if (!(minion instanceof Mob mob)) return;
        Player nearest = null;
        double best = 40 * 40;
        for (Player p : minion.getWorld().getPlayers()) {
            if (!com.Chagui68.entities.boss.BossArena.isValidTarget(p)) continue;
            double d = p.getLocation().distanceSquared(minion.getLocation());
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest != null && mob.getTarget() != nearest) mob.setTarget(nearest);
    }

    /** Names a minion, gives it its health and keeps it from wandering off with the chunk. */
    protected static <T extends LivingEntity> T prepare(T entity, Component name, double health) {
        entity.customName(name);
        entity.setCustomNameVisible(true);
        entity.setPersistent(true);
        setAttribute(entity, Attribute.MAX_HEALTH, health);
        entity.setHealth(health);
        return entity;
    }

    protected static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    /**
     * Spawns one of the plugin's own creatures through its usual {@code trySpawn} and returns what it
     * made: the living entities near the spot that were not there before.
     */
    protected static List<LivingEntity> spawnExisting(World world, Vector at, Predicate<Location> trySpawn) {
        Location location = at.toLocation(world);
        Set<UUID> before = new HashSet<>();
        for (Entity e : world.getNearbyEntities(location, 6, 8, 6)) before.add(e.getUniqueId());
        if (!trySpawn.test(location)) return List.of();
        List<LivingEntity> made = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(location, 6, 8, 6)) {
            if (e instanceof LivingEntity living && !(e instanceof Player) && !before.contains(e.getUniqueId())) {
                made.add(living);
            }
        }
        return made;
    }

    /** {@code count} points on a circle of {@code radius} around {@code center}, starting at a random angle. */
    protected static List<Vector> around(Stage stage, Vector center, double radius, int count) {
        List<Vector> out = new ArrayList<>();
        double start = stage.random().nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            out.add(center.clone().add(Shapes.heading(start + 2 * Math.PI * i / count).multiply(radius)));
        }
        return out;
    }
}
