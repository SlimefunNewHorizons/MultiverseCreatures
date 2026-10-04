package com.Chagui68.entities.boss.attack.ground;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Telegraph;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Shapes;
import org.bukkit.util.Vector;

/**
 * Tremor Lance: the Sentinel drives its spear into the ground three times. Each blow sends a
 * shockwave out along the floor, each one wider than the last, with a beat between them to jump.
 */
public class TremorLanceAttack extends ChoreographedAttack.Ground {

    private static final int RAISE = 14;
    private static final int BEAT = 16;
    private static final int STRIKES = 3;

    public TremorLanceAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        double damage = seal(stage, 0.6);
        Timeline t = new Timeline();
        Vector center = spearGround(stage);

        t.at(0, () -> fx.sound(stage.feet(), Sfx.IRON_GOLEM_ATTACK, 2f, 0.5f));
        for (int i = 0; i < STRIKES; i++) {
            int lift = i * BEAT;
            int hit = lift + RAISE;
            double reach = 9 + 5 * i;
            tween(t, stage, lift, hit - 3, Poses.GUARD, Poses.SPEAR_OVERHEAD, Ease.OUT);
            tween(t, stage, hit - 3, hit, Poses.SPEAR_OVERHEAD, Poses.SPEAR_SLAM, Ease.IN);
            t.span(lift, hit, (tick, p) -> {
                if (tick % 2 == 0) Telegraph.ring(stage, center, reach - 1.5, reach, p);
            });
            t.at(hit, () -> {
                fx.impact(center.clone().add(new Vector(0, 0.5, 0)), Palette.STONE, 1.6);
                fx.sound(center, Sfx.MACE_SMASH_GROUND, 2.5f, 0.6f + 0.15f * (float) reach / 10);
            });
            shockwave(t, stage, hit, 14 + 3 * i, center, reach, Palette.ASH, damage,
                    v -> v.push(Shapes.flat(v.position().subtract(center)).multiply(0.5).setY(0.5)));
        }
        recover(t, stage, STRIKES * BEAT + RAISE, STRIKES * BEAT + RAISE + 14, Poses.GUARD);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return STRIKES * BEAT + RAISE + 14;
    }

    @Override
    public String getName() {
        return "tremorlance";
    }
}
