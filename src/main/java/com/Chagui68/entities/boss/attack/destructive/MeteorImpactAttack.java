package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Prop;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Meteor Impact: the Sentinel calls down a meteor the size of a house. It comes in slowly from high
 * and far, burning, for five seconds; the impact covers twelve blocks and leaves the ground on fire.
 */
public class MeteorImpactAttack extends DestructiveAttack {

    private static final int CALL = 20;
    private static final int FALL = 100;
    private static final double RADIUS = 12;
    private static final float SIZE = 6f;

    public MeteorImpactAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        Fx fx = stage.fx();
        Vector center = stage.onGround(target != null ? target.position() : stage.feet().add(stage.forward().multiply(14)));
        Vector start = center.clone().add(Shapes.flat(stage.forward()).multiply(-35)).add(new Vector(0, 60, 0));
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        Prop[] rock = new Prop[1];
        Quaternionf[] spin = {new Quaternionf()};

        tweenTo(t, stage, 0, CALL, Poses.CAST_SKY.withHead(-35, 0, 0), Ease.OUT);
        t.at(0, () -> {
            fx.sound(stage.feet(), Sfx.WITHER_SPAWN, 2f, 0.4f);
            hud(stage, center, 60, "☄ METEOR INBOUND", "Run from the burning circle");
            rock[0] = stage.block(Material.MAGMA_BLOCK, start, SIZE, spin[0]);
            rock[0].glow(Palette.EMBER);
            props.add(rock[0]);
        });
        t.span(0, FALL, (tick, p) -> {
            double fall = Ease.at(Ease.IN, p);
            Vector at = start.clone().add(center.clone().subtract(start).multiply(fall));
            spin[0] = spin[0].rotateXYZ(0.05f, 0.08f, 0.03f);
            if (rock[0] != null) {
                rock[0].moveTo(at, 1);
                rock[0].reshape(SIZE, spin[0], 1);
            }
            Vector core = at.clone().add(new Vector(0, SIZE / 2, 0));
            fx.cloud(Particle.FLAME, core, 12, SIZE * 0.5, 0.05);
            fx.cloud(Particle.LARGE_SMOKE, core, 6, SIZE * 0.6, 0.02);
            fx.line(core, core.clone().subtract(center.clone().subtract(start).normalize().multiply(14)), 0.8,
                    fx.dust(Palette.MOLTEN, 2.4f).sometimes(0.6));
            if (tick % 2 == 0) reticle(stage, center, RADIUS, p, Palette.EMBER);
            if (tick % 20 == 0) fx.sound(center, Sfx.BELL, 2.5f, 0.5f + (float) p);
        });
        t.at(FALL, () -> {
            if (rock[0] != null) rock[0].remove();
        });
        dome(t, stage, FALL, 18, center, RADIUS, Palette.EMBER, seal(stage, 1.6), v -> {
            v.ignite(100);
            v.push(Shapes.flat(v.position().subtract(center)).multiply(1.2).setY(0.8));
        });
        // The crater keeps burning.
        t.span(FALL, FALL + 80, (tick, p) -> {
            if (tick % 3 == 0) fx.disc(center.clone().add(new Vector(0, 0.3, 0)), RADIUS * 0.6, 1.4, fx.particle(Particle.FLAME).sometimes(0.4));
            if (tick % 20 != 0) return;
            for (Victim v : stage.victimsIn(Area.cylinder(center, RADIUS * 0.6, 1, 3))) v.ignite(60);
        });
        recover(t, stage, CALL, CALL + 20, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "meteorimpact";
    }
}
