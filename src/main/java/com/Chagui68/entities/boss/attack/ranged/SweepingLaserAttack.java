package com.Chagui68.entities.boss.attack.ranged;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sweeping Laser: the Sentinel points its spear low and a beam of red light sweeps a 120-degree arc
 * across the floor in front of it, knee high. Jump it as it passes.
 */
public class SweepingLaserAttack extends ChoreographedAttack.Ranged {

    private static final int CHARGE = 22;
    private static final int SWEEP = 30;
    private static final double REACH = 22;
    private static final double HALF = Math.toRadians(60);

    public SweepingLaserAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Victim target = stage.target();
        Vector origin = stage.onGround(stage.feet());
        Vector aim = Shapes.flat(target != null ? target.position().subtract(origin) : stage.forward());
        double base = Math.atan2(aim.getZ(), aim.getX());
        boolean clockwise = stage.random().nextBoolean();
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, CHARGE, Poses.THRUST.withHead(10, 0, 0), Ease.IN_OUT);
        t.at(0, () -> fx.sound(origin, Sfx.WARDEN_SONIC_CHARGE, 2f, 1.2f));
        t.span(0, CHARGE, (tick, p) -> {
            if (tick % 2 == 0) Telegraph.cone(stage, origin, aim, HALF, REACH, p);
            fx.dust(Palette.WARNING, 1.6f, 0.3, 3).at(stage.body().spearTip());
        });
        t.span(CHARGE, CHARGE + SWEEP, (tick, p) -> {
            double a = base + (clockwise ? 1 : -1) * (-HALF + 2 * HALF * Ease.at(Ease.IN_OUT, p));
            Vector dir = Shapes.heading(a);
            Vector from = origin.clone().add(new Vector(0, 0.6, 0)).add(dir.clone().multiply(2));
            Vector to = from.clone().add(dir.clone().multiply(REACH));
            fx.beam(stage.body().spearTip(), from, Palette.WARNING_HOT, Palette.WARNING, 0.3);
            fx.beam(from, to, Palette.WARNING_HOT, Palette.WARNING, 0.4);
            if (tick % 3 == 0) fx.sound(from, Sfx.BEACON_POWER, 1.2f, 1.6f);
            for (Victim v : stage.victimsIn(Area.segment(from.clone().subtract(new Vector(0, 0.6, 0)), to.clone().subtract(new Vector(0, 0.6, 0)), 0.8))) {
                if (!struck.add(v.id())) continue;
                stage.damage(v, seal(stage, 0.8));
                v.ignite(60);
            }
        });
        recover(t, stage, CHARGE + SWEEP, CHARGE + SWEEP + 14, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "sweepinglaser";
    }
}
