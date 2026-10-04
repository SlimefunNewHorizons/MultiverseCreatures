package com.Chagui68.entities.boss.attack.aerial;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Telegraph;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Hook: from the air the Sentinel throws a hooked chain at up to three players. Each hook is
 * aimed at where its player stood and takes a moment to arrive: whoever is still on the mark is
 * dragged up towards the boss and dropped.
 */
public class ChainHookAttack extends ChoreographedAttack.Aerial {

    private static final int AIM = 16;
    private static final int THROW = 10;
    private static final int MAX_HOOKS = 3;
    private static final double MARK = 1.8;

    public ChainHookAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        List<Vector> marks = new ArrayList<>();
        for (Victim v : stage.victims()) {
            if (marks.size() >= MAX_HOOKS) break;
            if (v.position().distanceSquared(stage.feet()) > 40 * 40) continue;
            marks.add(stage.onGround(v.position()));
        }
        if (marks.isEmpty()) return null;
        double damage = seal(stage, 0.6);
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, AIM, Poses.THROW_COIL, Ease.IN_OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.CHAIN_BREAK, 2f, 0.5f));
        t.span(0, AIM + THROW, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (Vector mark : marks) Telegraph.circle(stage, mark, MARK, p);
        });
        tween(t, stage, AIM, AIM + 4, Poses.THROW_COIL, Poses.THROW, Ease.OUT_BACK);
        t.span(AIM, AIM + THROW, (tick, p) -> {
            Vector hand = stage.body().rightHand();
            for (Vector mark : marks) {
                Vector reach = hand.clone().add(mark.clone().add(new Vector(0, 1, 0)).subtract(hand).multiply((tick + 1) / (double) THROW));
                fx.line(hand, reach, 0.5, fx.dust(Palette.STONE, 1.2f).and(fx.dust(Palette.ASH, 0.9f).sometimes(0.5)));
                fx.particle(Particle.CRIT, 3, 0.1, 0.05).at(reach);
            }
        });
        t.at(AIM + THROW, () -> {
            Vector chest = stage.body().chest();
            for (Vector mark : marks) {
                for (Victim v : stage.victimsIn(Area.cylinder(mark, MARK, 1, 3))) {
                    stage.damage(v, damage);
                    v.effect(Affliction.SLOWNESS, 60, 2);
                    Vector pull = chest.clone().subtract(v.position()).setY(0);
                    if (pull.lengthSquared() > 0.01) pull.normalize().multiply(1.4);
                    v.fling(pull.setY(1.2));
                    fx.line(v.chest(), chest, 0.6, fx.dust(Palette.STONE, 1.4f));
                }
                fx.sound(mark, Sfx.CHAIN_BREAK, 1.6f, 1.2f);
            }
        });
        tweenTo(t, stage, AIM + THROW + 4, AIM + THROW + 18, Poses.HOVER, Ease.IN_OUT);
        return t;
    }

    @Override
    public String getName() {
        return "chainhook";
    }
}
