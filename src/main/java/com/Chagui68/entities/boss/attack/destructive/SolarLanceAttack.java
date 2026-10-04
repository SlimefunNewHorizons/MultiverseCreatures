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

import java.util.List;

/**
 * Solar Lance: a spear of sunlight fourteen blocks long forms over the Sentinel, drawing in light for
 * three and a half seconds while it tracks the target. It is hurled, and the impact is a blast of
 * holy fire that leaves the ground burning.
 */
public class SolarLanceAttack extends DestructiveAttack {

    private static final int FORM = 70;
    private static final int LOCK = 60;
    private static final int FLIGHT = 8;
    private static final double RADIUS = 8;
    private static final float LENGTH = 14f;

    public SolarLanceAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        if (target == null) return null;
        Fx fx = stage.fx();
        Vector[] aim = {stage.onGround(target.position())};
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        Prop[] lance = new Prop[1];
        Vector[] from = new Vector[1];

        tweenTo(t, stage, 0, 24, Poses.THROW_COIL, Ease.IN_OUT);
        t.at(0, () -> {
            fx.sound(stage.feet(), Sfx.BEACON_ACTIVATE, 3f, 1.2f);
            hud(stage, aim[0], 60, "☀ SOLAR LANCE", "Don't stand where it points");
        });
        t.span(0, FORM, (tick, p) -> {
            if (tick < LOCK) aim[0] = stage.onGround(aim[0].clone().add(target.position().subtract(aim[0]).multiply(0.2)));
            Vector hold = stage.body().head().add(new Vector(0, 5, 0));
            Vector dir = aim[0].clone().subtract(hold);
            if (lance[0] == null) {
                lance[0] = stage.block(Material.GLOWSTONE, hold, 1.2f, Prop.pointing(dir));
                lance[0].resize(1.2f, 0.2f, Prop.pointing(dir), 0);
                lance[0].glow(Palette.GOLD);
                props.add(lance[0]);
            }
            lance[0].moveTo(hold.clone().subtract(dir.clone().normalize().multiply(LENGTH * Ease.at(Ease.OUT, p) / 2)), 1);
            lance[0].resize(1.2f, (float) (0.2 + LENGTH * Ease.at(Ease.OUT, p)), Prop.pointing(dir), 1);
            if (tick % 2 == 0) fx.gather(hold, 10, 4, Palette.GOLD, 12);
            if (tick % 2 == 0) reticle(stage, aim[0], RADIUS, p, Palette.GOLD);
            if (tick % 4 == 0) fx.line(hold, aim[0], 1.2, fx.dust(Palette.HOLY, 1.0f).sometimes(0.5));
            if (tick % 15 == 0) fx.sound(hold, Sfx.AMETHYST_CHIME, 2f, 0.5f + (float) p);
        });
        tween(t, stage, FORM, FORM + 4, Poses.THROW_COIL, Poses.THROW, Ease.OUT_BACK);
        t.at(FORM, () -> {
            from[0] = stage.body().head().add(new Vector(0, 5, 0));
            fx.sound(from[0], Sfx.TRIDENT_THROW, 3f, 0.6f);
            // Scheduled at the throw, so the blast lands where the lance was locked.
            dome(t, stage, FORM + FLIGHT, 14, aim[0].clone(), RADIUS, Palette.GOLD, seal(stage, 1.6), v -> v.ignite(100));
        });
        t.span(FORM, FORM + FLIGHT, (tick, p) -> {
            if (lance[0] == null || from[0] == null) return;
            Vector at = from[0].clone().add(aim[0].clone().subtract(from[0]).multiply((tick + 1) / (double) FLIGHT));
            lance[0].moveTo(at, 1);
            fx.line(at, from[0], 1.0, fx.dust(Palette.GOLD, 1.8f).sometimes(0.5));
        });
        t.at(FORM + FLIGHT, () -> {
            if (lance[0] != null) lance[0].remove();
        });
        t.span(FORM + FLIGHT, FORM + FLIGHT + 70, (tick, p) -> {
            if (tick % 3 == 0) fx.disc(aim[0].clone().add(new Vector(0, 0.3, 0)), RADIUS * 0.8, 1.4, fx.particle(Particle.SMALL_FLAME).sometimes(0.5));
            if (tick % 20 != 0) return;
            for (Victim v : stage.victimsIn(Area.cylinder(aim[0], RADIUS * 0.8, 1, 3))) {
                stage.damage(v, seal(stage, 0.2));
                v.ignite(40);
            }
        });
        recover(t, stage, FORM + 6, FORM + 26, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "solarlance";
    }
}
