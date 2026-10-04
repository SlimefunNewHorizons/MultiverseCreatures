package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.BossInstance.DefenseState;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

/**
 * Afterimage: the Sentinel blurs, and ghost copies of it flicker at its sides. For eight seconds about
 * a third of the hits aimed at it strike an afterimage instead and miss.
 */
public class AfterimageAttack extends ChoreographedAttack {

    private static final int RAISE = 12;
    private static final int MAX = 400;
    private static final double OFFSET = 3.0;

    public AfterimageAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return instance.activeDefense == DefenseState.NONE && boss.isOnGround(instance.stand);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        BossInstance instance = stage.instance();
        if (instance != null) {
            instance.activeDefense = DefenseState.EVASION;
            instance.defenseTimer = 0;
        }
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, RAISE, Poses.SPREAD, Ease.OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.ILLUSIONER_MIRROR, 2.5f, 1.0f));
        t.at(RAISE, () -> fx.burst(stage.body().chest(), Particle.LARGE_SMOKE, 30, 0.3));
        recover(t, stage, RAISE, RAISE + 12, Poses.GUARD);
        t.span(RAISE, MAX, (tick, p) -> {
            boolean active = instance == null ? tick < 160 : instance.activeDefense == DefenseState.EVASION;
            if (!active) {
                fx.sound(stage.feet(), Sfx.ILLUSIONER_MIRROR, 1.5f, 0.6f);
                t.stop();
                return;
            }
            if (tick % 4 != 0) return;
            Vector right = stage.body().right();
            // The copies swap sides and drift, so they read as flicker rather than as statues.
            double side = (tick / 4) % 2 == 0 ? 1 : -1;
            double drift = OFFSET + Math.sin(tick * 0.2) * 0.8;
            Fx.Brush ghost = fx.dust(Palette.mix(Palette.SPECTRAL, Palette.VOID, (tick % 12) / 12.0), 1.2f).sometimes(0.7);
            silhouette(stage, stage.feet().add(right.clone().multiply(side * drift)), stage.yaw(), stage.pose(), ghost, 0.6);
            if (tick % 40 == 0) fx.sound(stage.feet(), Sfx.ILLUSIONER_MIRROR, 0.8f, 1.6f);
        });
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return RAISE + 12;
    }

    @Override
    public String getName() {
        return "afterimage";
    }
}
