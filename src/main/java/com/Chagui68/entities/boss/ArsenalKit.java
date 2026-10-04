package com.Chagui68.entities.boss;

import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.LiveStage;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * What {@link JackArsenal} and {@link NixArsenal} share: the view of the boss an attack works
 * through, the running attack the boss ticks, small projectiles, and the floor warnings.
 */
final class ArsenalKit {

    private ArsenalKit() {
    }

    /** The boss as an arsenal attack sees it. */
    interface Host {
        ArmorStand stand();

        /** The body's current size; only JackStar ever changes his. */
        default float scale() {
            return 1f;
        }

        /** Players the fight can reach: alive, in survival or adventure, close to the boss. */
        List<Player> players();

        void deal(Player target, double amount, String source);

        Random random();

        /** Restores health to the boss (NIX's blood pool). */
        default void heal(double amount) {
        }

        /** Makes the boss faster for a while (JackStar's overclock). */
        default void empower(int ticks) {
        }
    }

    /**
     * An arsenal attack in progress. The boss is rooted and posed while it channels; the timeline
     * keeps playing after that for whatever lingers (puddles, falling bolts, closing rings).
     */
    static final class Running {
        final String key;
        final BossGesture gesture;
        final int channel;
        final Timeline timeline;

        Running(String key, BossGesture gesture, int channel, Timeline timeline) {
            this.key = key;
            this.gesture = gesture;
            this.channel = Math.max(1, channel);
            this.timeline = timeline;
        }

        boolean channeling() {
            return timeline.now() < channel;
        }
    }

    /** Everything one cast needs, captured when it starts. */
    static final class Cast {
        final Host host;
        final ArmorStand stand;
        final World world;
        final Fx fx;
        final Random random;
        final Player target;
        final double power;
        final String source;

        Cast(Host host, Player target, double power, String source) {
            this.host = host;
            this.stand = host.stand();
            this.world = stand.getWorld();
            this.fx = LiveStage.fxIn(world);
            this.random = host.random();
            this.target = target;
            this.power = power;
            this.source = source;
        }

        Vector feet() {
            return stand.getLocation().toVector();
        }

        Vector chest() {
            return feet().add(new Vector(0, 1.2 * host.scale(), 0));
        }

        boolean targetHere() {
            return valid(target) && target.getWorld().equals(world);
        }

        /** Where the target stands, or a few blocks ahead of the boss when it is gone. */
        Vector aim() {
            if (targetHere()) return target.getLocation().toVector();
            return feet().add(Shapes.flat(stand.getLocation().getDirection()).multiply(6));
        }

        Vector facing() {
            return Shapes.flat(stand.getLocation().getDirection());
        }

        /** The boss's right hand, a little in front of him. */
        Vector hand() {
            float s = host.scale();
            return chest().add(rotateFlat(facing(), Math.PI / 2).multiply(0.5 * s))
                    .add(facing().multiply(0.5 * s)).add(new Vector(0, 0.3 * s, 0));
        }

        void face(Vector point) {
            Location loc = stand.getLocation();
            Vector d = point.clone().subtract(loc.toVector()).setY(0);
            if (d.lengthSquared() < 0.01) return;
            loc.setDirection(d);
            stand.teleport(loc);
        }

        List<Player> players() {
            return host.players();
        }

        Vector ground(Vector at) {
            return groundAt(world, at);
        }

        void hit(Player p, double base) {
            host.deal(p, base * power, source);
        }

        void afflict(Player p, PotionEffectType type, int ticks, int amplifier) {
            p.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true));
        }

        void push(Player p, Vector velocity) {
            p.setVelocity(p.getVelocity().add(velocity));
        }
    }

    /** Ticks every running attack once and drops the finished ones. */
    static void tick(List<Running> running) {
        running.removeIf(r -> !r.timeline.tick());
    }

    /** The attack the boss is channelling now, or {@code null} when it is free to move. */
    static Running channeling(List<Running> running) {
        for (Running r : running) {
            if (r.channeling()) return r;
        }
        return null;
    }

    /** A small projectile: moves {@code velocity} per tick until it hits a player, a wall or runs out. */
    static final class Shot {
        Vector at;
        Vector velocity;
        int life;
        boolean spent;

        Shot(Vector at, Vector velocity, int life) {
            this.at = at.clone();
            this.velocity = velocity.clone();
            this.life = life;
        }
    }

    /**
     * Advances every live shot one tick: draws it, ends it in a wall, and hands the first player
     * within {@code radius} of it to {@code onHit}.
     */
    static void fly(List<Shot> shots, World world, List<Player> players, double radius,
                    Consumer<Shot> draw, BiConsumer<Shot, Player> onHit) {
        for (Shot shot : shots) {
            if (shot.spent) continue;
            shot.at.add(shot.velocity);
            if (--shot.life <= 0 || world.getBlockAt(shot.at.getBlockX(), shot.at.getBlockY(), shot.at.getBlockZ()).getType().isSolid()) {
                shot.spent = true;
                continue;
            }
            draw.accept(shot);
            for (Player p : players) {
                Vector chest = p.getLocation().toVector().add(new Vector(0, 1.0, 0));
                if (chest.distanceSquared(shot.at) > radius * radius) continue;
                shot.spent = true;
                onHit.accept(shot, p);
                break;
            }
        }
    }

    // ------------------------------------------------------------------ geometry

    /** The floor under a location, as a point on its top face. */
    static Vector groundAt(Location at) {
        Location probe = at.clone().add(0, 3, 0);
        double y = BossArena.findFloorY(probe, 12);
        Vector out = at.toVector();
        if (!Double.isNaN(y)) out.setY(y);
        return out;
    }

    static Vector groundAt(World world, Vector at) {
        return groundAt(at.toLocation(world));
    }

    /** Horizontal unit vector from {@code from} towards {@code to}; +z when they share a column. */
    static Vector towards(Vector from, Vector to) {
        Vector d = to.clone().subtract(from).setY(0);
        return d.lengthSquared() < 1e-6 ? new Vector(0, 0, 1) : d.normalize();
    }

    static Vector rotateFlat(Vector dir, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        Vector out = new Vector(dir.getX() * c - dir.getZ() * s, 0, dir.getX() * s + dir.getZ() * c);
        return out.lengthSquared() < 1e-9 ? new Vector(0, 0, 1) : out.normalize();
    }

    static double flatDistance(Vector a, Vector b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    static boolean valid(Player p) {
        return p != null && p.isOnline() && p.isValid() && !p.isDead();
    }

    /** The players of {@code world} that can be hit and stand within {@code range} of {@code center}. */
    static List<Player> playersNear(World world, Vector center, double range) {
        List<Player> out = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (!BossArena.isValidTarget(p)) continue;
            if (p.getLocation().toVector().distanceSquared(center) > range * range) continue;
            out.add(p);
        }
        return out;
    }

    // ------------------------------------------------------------------ warnings

    private static Color heat(double progress) {
        return Palette.mix(Palette.WARNING, Palette.WARNING_HOT, progress * progress);
    }

    /** A warning circle on the floor, red turning hot yellow as the blow comes. */
    static void warnCircle(Fx fx, Vector center, double radius, double progress) {
        Vector floor = center.clone().add(new Vector(0, 0.15, 0));
        fx.ring(floor, radius, 0.7, progress * 3, fx.dust(heat(progress), 1.3f));
        if (radius * progress > 0.8) fx.ring(floor, radius * progress, 1.1, 0, fx.dust(heat(progress), 0.9f));
    }

    /** A warning strip on the floor from {@code a} to {@code b}. */
    static void warnLine(Fx fx, Vector a, Vector b, double width, double progress) {
        Vector dir = towards(a, b);
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).multiply(width / 2);
        Vector lift = new Vector(0, 0.15, 0);
        Fx.Brush edge = fx.dust(heat(progress), 1.2f);
        fx.line(a.clone().add(side).add(lift), b.clone().add(side).add(lift), 0.8, edge);
        fx.line(a.clone().subtract(side).add(lift), b.clone().subtract(side).add(lift), 0.8, edge);
        Vector reach = a.clone().add(b.clone().subtract(a).multiply(progress));
        fx.line(a.clone().add(lift), reach.add(lift), 0.9, fx.dust(heat(progress), 0.9f).sometimes(0.6));
    }

    /** A warning fan on the floor: the cone {@code halfAngle} radians each side of {@code direction}. */
    static void warnCone(Fx fx, Vector apex, Vector direction, double halfAngle, double length, double progress) {
        Vector floor = apex.clone().add(new Vector(0, 0.15, 0));
        double base = Math.atan2(direction.getZ(), direction.getX());
        Fx.Brush edge = fx.dust(heat(progress), 1.2f);
        fx.line(floor, floor.clone().add(Shapes.heading(base - halfAngle).multiply(length)), 0.8, edge);
        fx.line(floor, floor.clone().add(Shapes.heading(base + halfAngle).multiply(length)), 0.8, edge);
        int points = Math.max(6, (int) (halfAngle * 2 * length / 0.8));
        fx.draw(Shapes.arc(floor, length, base - halfAngle, base + halfAngle, points, Shapes.FLAT_U, Shapes.FLAT_V), edge);
        double filled = length * progress;
        if (filled > 1) {
            int fill = Math.max(4, (int) (halfAngle * 2 * filled / 1.2));
            fx.draw(Shapes.arc(floor, filled, base - halfAngle, base + halfAngle, fill, Shapes.FLAT_U, Shapes.FLAT_V),
                    fx.dust(heat(progress), 0.9f));
        }
    }

    // ------------------------------------------------------------------ destructive attacks

    /** A warning across the screen of every player within {@code range} of {@code center}. */
    static void hud(World world, Vector center, double range, String title, String subtitle) {
        for (Player p : world.getPlayers()) {
            if (p.getLocation().toVector().distanceSquared(center) > range * range) continue;
            p.sendTitle(org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + title,
                    org.bukkit.ChatColor.GOLD + subtitle, 0, 30, 8);
        }
    }

    /** A targeting grid on the floor: a lattice clipped to the circle, its ring and a closing crosshair. */
    static void reticle(Fx fx, Vector floor, double radius, double progress, Color color) {
        Vector at = floor.clone().add(new Vector(0, 0.2, 0));
        Color hot = Palette.mix(color, Palette.WARNING_HOT, progress * progress);
        Fx.Brush edge = fx.dust(hot, 1.5f);
        fx.ring(at, radius, 0.7, progress * 4, edge);
        fx.ring(at, Math.max(0.6, radius * (1 - progress)), 0.6, -progress * 6, fx.dust(Palette.WARNING_HOT, 1.2f));
        fx.line(at.clone().add(new Vector(-radius, 0, 0)), at.clone().add(new Vector(radius, 0, 0)), 0.6, edge);
        fx.line(at.clone().add(new Vector(0, 0, -radius)), at.clone().add(new Vector(0, 0, radius)), 0.6, edge);
        Fx.Brush grid = fx.dust(color, 0.9f).sometimes(0.6);
        double step = Math.max(1.5, radius / 4);
        for (double u = -radius + step; u < radius; u += step) {
            double half = Math.sqrt(Math.max(0, radius * radius - u * u));
            fx.line(at.clone().add(new Vector(u, 0, -half)), at.clone().add(new Vector(u, 0, half)), 1.0, grid);
            fx.line(at.clone().add(new Vector(-half, 0, u)), at.clone().add(new Vector(half, 0, u)), 1.0, grid);
        }
    }

    /**
     * The blast of a destructive attack: a flash and a column of light at {@code start}, where
     * {@code impact} deals the damage, then a dome swelling to {@code radius} over {@code ticks} and
     * smoke settling after it.
     */
    static void dome(Timeline t, Fx fx, int start, int ticks, Vector floor, double radius, Color color, Runnable impact) {
        t.at(start, () -> {
            fx.flash(floor.clone().add(new Vector(0, 2, 0)), color);
            fx.line(floor, floor.clone().add(new Vector(0, 40, 0)), 0.4, fx.dust(Palette.HOLY, 2.6f).and(fx.dust(color, 2.2f, 0.4, 1)));
            fx.impact(floor.clone().add(new Vector(0, 1, 0)), color, Math.min(6, radius / 2));
            fx.sound(floor, com.Chagui68.entities.boss.fx.Sfx.EXPLODE, 3f, 0.5f);
            fx.sound(floor, com.Chagui68.entities.boss.fx.Sfx.WARDEN_SONIC_BOOM, 2.5f, 0.6f);
            impact.run();
        });
        t.span(start, start + ticks, (tick, p) -> {
            double r = Math.max(0.5, radius * com.Chagui68.entities.boss.fx.Ease.at(com.Chagui68.entities.boss.fx.Ease.OUT, p));
            Fx.Brush shell = fx.dust(Palette.mix(Palette.WARNING_HOT, color, p), 2.4f);
            for (Vector point : Shapes.sphere(floor, r, (int) (60 + r * 12))) {
                if (point.getY() >= floor.getY()) shell.at(point);
            }
            fx.ring(floor.clone().add(new Vector(0, 0.3, 0)), r, 0.8, tick, fx.dust(color, 2.0f));
        });
        t.span(start + ticks, start + ticks + 30, (tick, p) -> {
            if (tick % 3 == 0) fx.disc(floor.clone().add(new Vector(0, 0.4, 0)), radius, 2.5,
                    fx.particle(org.bukkit.Particle.LARGE_SMOKE).sometimes(0.3));
        });
    }

    /** A standing figure sketched in dust: a clone, a ghost, a hologram. */
    static void drawFigure(Fx fx, Vector feet, Vector facing, Color color, double height) {
        Fx.Brush brush = fx.dust(color, 1.1f);
        Vector up = new Vector(0, height, 0);
        Vector side = rotateFlat(facing, Math.PI / 2).multiply(0.35 * height / 1.8);
        Vector hip = feet.clone().add(up.clone().multiply(0.45));
        Vector neck = feet.clone().add(up.clone().multiply(0.78));
        fx.line(hip, neck, 0.2, brush);
        fx.line(hip, feet.clone().add(side), 0.25, brush);
        fx.line(hip, feet.clone().subtract(side), 0.25, brush);
        fx.line(neck.clone().add(side), neck.clone().subtract(side), 0.2, brush);
        fx.line(neck.clone().add(side), hip.clone().add(side.clone().multiply(1.3)), 0.25, brush);
        fx.line(neck.clone().subtract(side), hip.clone().subtract(side.clone().multiply(1.3)), 0.25, brush);
        fx.draw(Shapes.circle(feet.clone().add(up.clone().multiply(0.9)), 0.12 * height, 8, side.clone().normalize(), Shapes.UP, 0), brush);
    }
}
