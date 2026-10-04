package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.BossInstance.DefenseState;
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
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Thorn Aura: obsidian thorns break out of the Sentinel's armour. For ten seconds it takes a little
 * less damage, every hit landed on it stings back, and anyone standing close is pricked and shoved
 * away every second.
 */
public class ThornAuraAttack extends ChoreographedAttack {

    private static final int RAISE = 14;
    private static final int MAX = 400;
    private static final double REACH = 6;

    public ThornAuraAttack(BossHost boss) {
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
            instance.activeDefense = DefenseState.THORNS;
            instance.defenseTimer = 0;
        }
        double prick = seal(stage, 0.2);
        List<Vector> spines = Shapes.sphere(new Vector(), 1, 28);
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, RAISE, Poses.ROAR, Ease.OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.WITHER_BREAK_BLOCK, 2f, 1.2f));
        t.at(RAISE, () -> {
            fx.burst(stage.body().chest(), Particle.CRIT, 40, 0.6);
            fx.sound(stage.feet(), Sfx.PLAYER_ATTACK_STRONG, 2f, 0.6f);
        });
        recover(t, stage, RAISE, RAISE + 12, Poses.GUARD);
        t.span(RAISE, MAX, (tick, p) -> {
            boolean active = instance == null ? tick < 200 : instance.activeDefense == DefenseState.THORNS;
            if (!active) {
                fx.burst(stage.body().chest(), Particle.CRIT, 30, 0.5);
                t.stop();
                return;
            }
            Vector chest = stage.body().chest();
            if (tick % 4 == 0) {
                for (Vector s : spines) {
                    Vector root = chest.clone().add(s.clone().multiply(2.5));
                    fx.line(root, root.clone().add(s.clone().multiply(1.2)), 0.4, fx.dust(Palette.mix(Palette.ASH, Palette.BLOOD, 0.4), 1.1f));
                }
            }
            if (tick % 20 != 0) return;
            Vector feet = stage.feet();
            fx.ring(feet.clone().add(new Vector(0, 0.3, 0)), REACH, 0.7, tick * 0.1, fx.dust(Palette.BLOOD, 1.3f));
            stage.hit(Area.cylinder(feet, REACH, 2, 8), prick,
                    v -> v.push(Shapes.flat(v.position().subtract(feet)).multiply(0.6).setY(0.3)));
        });
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return RAISE + 12;
    }

    @Override
    public String getName() {
        return "thornaura";
    }
}
