package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

/**
 * Supernova: a star is born over the Sentinel's head, swelling for four seconds while it drinks the
 * light of the arena, then collapses and explodes across 22 blocks. The only shelter is the eye of
 * the storm: the small circle at the Sentinel's own feet.
 */
public class SupernovaAttack extends DestructiveAttack {

    private static final int GROW = 80;
    private static final int COLLAPSE = 10;
    private static final double SAFE = 5;
    private static final double REACH = 22;

    public SupernovaAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Vector feet = stage.onGround(stage.feet());
        Vector star = stage.body().head().add(new Vector(0, 6, 0));
        int blast = GROW + COLLAPSE;
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, 20, Poses.CAST_SKY.withLeftArm(-170, 0, -20), Ease.OUT);
        t.at(0, () -> {
            fx.sound(feet, Sfx.BEACON_ACTIVATE, 3f, 0.4f);
            hud(stage, feet, 50, "☀ SUPERNOVA", "Only the eye of the storm is safe — get under it");
        });
        t.span(0, GROW, (tick, p) -> {
            double r = 0.5 + 4 * Ease.at(Ease.IN_OUT, p);
            fx.draw(Shapes.sphere(star, r, (int) (40 + r * 20)), fx.dust(Palette.mix(Palette.GOLD, Palette.HOLY, p), 2.2f));
            if (tick % 2 == 0) fx.gather(star, 14, 4, Palette.GOLD, 14);
            fx.cloud(Particle.END_ROD, star, 3, r, 0.02);
            if (tick % 2 == 0) {
                Vector floor = feet.clone().add(new Vector(0, 0.2, 0));
                fx.ring(floor, SAFE, 0.6, tick * 0.05, fx.dust(Palette.PLAGUE, 1.6f));
                fx.ring(floor, REACH, 1.0, -tick * 0.02, fx.dust(Palette.mix(Palette.WARNING, Palette.WARNING_HOT, p), 1.6f));
            }
            if (tick % 16 == 0) fx.sound(star, Sfx.WARDEN_HEARTBEAT, 3f, 0.5f + (float) p);
        });
        t.span(GROW, blast, (tick, p) -> {
            double r = 4.5 * (1 - p) + 0.3;
            fx.draw(Shapes.sphere(star, r, 60), fx.dust(Palette.HOLY, 2.6f));
            if (tick == 0) fx.sound(star, Sfx.BEACON_DEACTIVATE, 3f, 0.4f);
        });
        tweenTo(t, stage, GROW, blast, Poses.ROAR, Ease.IN);
        t.at(blast, () -> {
            fx.flash(star, Palette.HOLY);
            fx.burst(star, Particle.END_ROD, 160, 1.4);
            fx.sound(feet, Sfx.EXPLODE, 3f, 0.4f);
            fx.sound(feet, Sfx.WARDEN_SONIC_BOOM, 3f, 0.5f);
            for (Victim v : stage.victimsIn(Area.ring(feet, SAFE, REACH, 12))) {
                stage.damage(v, seal(stage, 1.6));
                v.effect(Affliction.BLINDNESS, 50, 0);
                v.push(Shapes.flat(v.position().subtract(feet)).multiply(1.4).setY(0.7));
            }
            crater(stage, feet, REACH * 0.6, 24);
        });
        t.span(blast, blast + 20, (tick, p) -> {
            double r = SAFE + (REACH - SAFE) * Ease.at(Ease.OUT, p);
            fx.draw(Shapes.sphere(star.clone().subtract(new Vector(0, 6, 0)), r, (int) (80 + r * 10)), fx.dust(Palette.mix(Palette.HOLY, Palette.GOLD, p), 2.4f).sometimes(0.6));
            fx.ring(feet.clone().add(new Vector(0, 0.4, 0)), r, 0.8, tick, fx.dust(Palette.GOLD, 2.0f));
        });
        recover(t, stage, blast + 6, blast + 26, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "supernova";
    }
}
