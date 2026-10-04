package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Telegraph;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Apocalypse Rain: the Sentinel roars at a sky that turns red, and for five seconds meteors rain on
 * the whole arena, each one marked on the floor a second before it lands. Keep moving.
 */
public class ApocalypseRainAttack extends DestructiveAttack {

    private static final int ROAR = 40;
    private static final int RAIN = 100;
    private static final int WARN = 20;
    private static final int FALL = 4;
    private static final double REACH = 22;
    private static final double RADIUS = 2.8;

    /** One meteor of the rain: where it lands and when its warning started. */
    private record Meteor(Vector at, int born) {
    }

    public ApocalypseRainAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Vector center = stage.onGround(stage.feet());
        List<Meteor> meteors = new ArrayList<>();
        int end = ROAR + RAIN + WARN + FALL;
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, 20, Poses.ROAR.withHead(-40, 0, 0), Ease.OUT);
        t.at(0, () -> {
            fx.sound(center, Sfx.WITHER_SPAWN, 3f, 0.4f);
            hud(stage, center, 60, "☄ THE SKY IS FALLING", "Keep moving");
        });
        t.span(0, end, (tick, p) -> {
            // The red sky: a slow churn of ember dust high over the arena.
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    Vector at = center.clone().add(Shapes.heading(stage.random().nextDouble() * 6.28)
                            .multiply(Math.sqrt(stage.random().nextDouble()) * REACH)).add(new Vector(0, 24 + stage.random().nextDouble() * 4, 0));
                    fx.dust(stage.random().nextBoolean() ? Palette.WARNING : Palette.EMBER, 3f).at(at);
                }
            }
            if (tick >= ROAR && tick < ROAR + RAIN && tick % 3 == 0) {
                List<Victim> victims = stage.victims();
                Vector spot;
                if (!victims.isEmpty() && stage.random().nextInt(100) < 60) {
                    Victim aim = victims.get(stage.random().nextInt(victims.size()));
                    spot = aim.position().add(new Vector(stage.random().nextGaussian() * 2.5, 0, stage.random().nextGaussian() * 2.5));
                } else {
                    spot = center.clone().add(Shapes.heading(stage.random().nextDouble() * 6.28).multiply(Math.sqrt(stage.random().nextDouble()) * REACH));
                }
                meteors.add(new Meteor(stage.onGround(spot), tick));
            }
            for (Meteor m : meteors) {
                int age = tick - m.born();
                if (age < 0 || age > WARN + FALL) continue;
                if (age < WARN) {
                    if (age % 2 == 0) Telegraph.circle(stage, m.at(), RADIUS, age / (double) WARN);
                    continue;
                }
                double f = (age - WARN + 1) / (double) FALL;
                Vector rock = m.at().clone().add(new Vector(6 * (1 - f), 26 * (1 - f), 0));
                fx.line(rock, rock.clone().add(new Vector(3, 8, 0)), 0.5, fx.dust(Palette.MOLTEN, 2.2f));
                fx.cloud(Particle.FLAME, rock, 6, 0.6, 0.05);
                if (age < WARN + FALL) continue;
                fx.impact(m.at().clone().add(new Vector(0, 0.6, 0)), Palette.EMBER, 1.6);
                fx.sound(m.at(), Sfx.EXPLODE, 1.6f, 0.9f);
                stage.hit(Area.cylinder(m.at(), RADIUS, 1, 3), seal(stage, 0.6), v -> v.ignite(40));
            }
            if (tick % 20 == 0) fx.sound(center, Sfx.LIGHTNING_THUNDER, 2f, 0.5f);
        });
        tweenTo(t, stage, ROAR, ROAR + 20, Poses.CAST_SKY, Ease.IN_OUT);
        recover(t, stage, ROAR + RAIN, ROAR + RAIN + 20, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "apocalypserain";
    }
}
