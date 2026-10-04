package com.Chagui68.entities.boss.attack;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

/**
 * A destructive attack: a long, loud charge the whole arena can see coming, then a blow of enormous
 * reach. The Sentinel commits to it — it stands in the charge for its whole length, which is the
 * window to punish it or to run.
 *
 * <p>The look borrows from orbital-strike weapons: a targeting grid that locks onto the impact zone, a
 * warning flashed on every screen in range, beams converging from the sky and a dome of light that
 * swallows everything inside. The world itself is never broken; the craters are made of debris.
 */
public abstract class DestructiveAttack extends ChoreographedAttack.Ground {

    /** The red of the targeting grid and the strike. */
    protected static final Color STRIKE = Color.fromRGB(0xFF1F3A);
    protected static final Color STRIKE_HOT = Color.fromRGB(0xFFB36B);

    protected DestructiveAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return super.ready(instance) && !instance.shieldSealActive;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return timeline.length();
    }

    /** A warning across the screen of every player within {@code range} of {@code center}. */
    protected static void hud(Stage stage, Vector center, double range, String title, String subtitle) {
        stage.onServer(world -> {
            Location at = center.toLocation(world);
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(at) > range * range) continue;
                p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + title, ChatColor.GOLD + subtitle, 0, 30, 8);
            }
        });
    }

    /**
     * The targeting grid over an impact zone: a lattice clipped to the circle, its ring, and a
     * crosshair whose inner ring closes as {@code progress} runs to 1.
     */
    protected static void reticle(Stage stage, Vector center, double radius, double progress, Color color) {
        Fx fx = stage.fx();
        Vector floor = stage.onGround(center).add(new Vector(0, 0.2, 0));
        Color heat = Palette.mix(color, STRIKE_HOT, progress * progress);
        Fx.Brush edge = fx.dust(heat, 1.5f);
        fx.ring(floor, radius, 0.7, progress * 4, edge);
        fx.ring(floor, Math.max(0.6, radius * (1 - progress)), 0.6, -progress * 6, fx.dust(STRIKE_HOT, 1.2f));
        fx.line(floor.clone().add(new Vector(-radius, 0, 0)), floor.clone().add(new Vector(radius, 0, 0)), 0.6, edge);
        fx.line(floor.clone().add(new Vector(0, 0, -radius)), floor.clone().add(new Vector(0, 0, radius)), 0.6, edge);
        Fx.Brush grid = fx.dust(color, 0.9f).sometimes(0.6);
        double step = Math.max(1.5, radius / 4);
        for (double u = -radius + step; u < radius; u += step) {
            double half = Math.sqrt(Math.max(0, radius * radius - u * u));
            fx.line(floor.clone().add(new Vector(u, 0, -half)), floor.clone().add(new Vector(u, 0, half)), 1.0, grid);
            fx.line(floor.clone().add(new Vector(-half, 0, u)), floor.clone().add(new Vector(half, 0, u)), 1.0, grid);
        }
    }

    /**
     * The blast: a flash and a column of light, then a dome that swells to {@code radius} over
     * {@code ticks}. Everyone inside the radius when it lands takes {@code damage} once, then {@code then}.
     */
    protected static void dome(Timeline t, Stage stage, int start, int ticks, Vector center, double radius, Color color,
                               double damage, Consumer<Victim> then) {
        Fx fx = stage.fx();
        Vector floor = stage.onGround(center);
        t.at(start, () -> {
            fx.flash(floor.clone().add(new Vector(0, 2, 0)), color);
            fx.line(floor, floor.clone().add(new Vector(0, 40, 0)), 0.4, fx.dust(Palette.HOLY, 2.6f).and(fx.dust(color, 2.2f, 0.4, 1)));
            fx.impact(floor.clone().add(new Vector(0, 1, 0)), color, Math.min(6, radius / 2));
            fx.sound(floor, Sfx.EXPLODE, 3f, 0.5f);
            fx.sound(floor, Sfx.WARDEN_SONIC_BOOM, 2.5f, 0.6f);
            fx.sound(floor, Sfx.LIGHTNING_THUNDER, 2.5f, 0.6f);
            stage.hit(Area.cylinder(floor, radius, 3, radius), damage, then);
            crater(stage, floor, radius, (int) Math.min(24, radius * 2));
        });
        t.span(start, start + ticks, (tick, p) -> {
            double r = Math.max(0.5, radius * Ease.at(Ease.OUT, p));
            Color shell = Palette.mix(STRIKE_HOT, color, p);
            for (Vector point : Shapes.sphere(floor, r, (int) (60 + r * 12))) {
                if (point.getY() < floor.getY()) continue;
                fx.dust(shell, 2.4f).at(point);
            }
            fx.ring(floor.clone().add(new Vector(0, 0.3, 0)), r, 0.8, tick, fx.dust(color, 2.0f).and(fx.particle(Particle.LARGE_SMOKE).sometimes(0.2)));
        });
        t.span(start + ticks, start + ticks + 30, (tick, p) -> {
            if (tick % 3 != 0) return;
            for (int i = 0; i < 6; i++) {
                double a = stage.random().nextDouble() * Math.PI * 2;
                double d = Math.sqrt(stage.random().nextDouble()) * radius;
                Vector at = floor.clone().add(Shapes.heading(a).multiply(d)).add(new Vector(0, 0.3, 0));
                fx.moving(Particle.LARGE_SMOKE, at, new Vector(0, 0.12, 0));
                fx.dust(Palette.mix(color, Palette.ASH, p), 1.6f).at(at);
            }
        });
    }

    /** Chunks of the floor flung out from a blast, the crater it would leave. */
    protected static void crater(Stage stage, Vector center, double radius, int chunks) {
        Material ground = stage.groundMaterial(center);
        for (int i = 0; i < chunks; i++) {
            double a = 2 * Math.PI * i / chunks + stage.random().nextDouble() * 0.3;
            double d = radius * (0.3 + stage.random().nextDouble() * 0.6);
            Vector at = stage.onGround(center.clone().add(Shapes.heading(a).multiply(d)));
            Vector out = Shapes.heading(a).multiply(0.25 + stage.random().nextDouble() * 0.3).setY(0.5 + stage.random().nextDouble() * 0.4);
            stage.debris(at, out, ground, 30);
        }
    }

    /** Beams from a ring high in the sky converging on {@code center}, narrowing as {@code progress} grows. */
    protected static void convergingBeams(Stage stage, Vector center, int beams, double height, double spread,
                                          double progress, Color color) {
        Fx fx = stage.fx();
        Vector floor = stage.onGround(center);
        double r = spread * (1 - progress);
        for (int i = 0; i < beams; i++) {
            Vector top = floor.clone().add(Shapes.heading(2 * Math.PI * i / beams + progress * 2).multiply(r)).add(new Vector(0, height, 0));
            fx.line(top, floor, 1.2, fx.dust(color, 1.4f).sometimes(0.7));
        }
    }
}
