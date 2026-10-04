package com.Chagui68.entities.boss.attack.ranged;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
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
 * Gravity Orb: a slow, heavy sphere of void drifts towards the target, dragging every player within
 * eight blocks into its path as it goes. After three seconds it implodes.
 */
public class GravityOrbAttack extends ChoreographedAttack.Ranged {

    private static final int FORM = 18;
    private static final int DRIFT = 60;
    private static final double SPEED = 0.35;
    private static final double PULL = 8;
    private static final double BLAST = 4;

    public GravityOrbAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        if (target == null) return null;
        Fx fx = stage.fx();
        Vector[] orb = new Vector[1];
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, FORM, Poses.CAST_FORWARD, Ease.OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.END_PORTAL_SPAWN, 1.5f, 1.4f));
        t.span(0, FORM, (tick, p) -> {
            Vector hand = stage.body().leftHand();
            fx.draw(Shapes.sphere(hand, 0.3 + 1.2 * p, 20), fx.dust(Palette.VOID_DEEP, 2f));
            if (tick % 3 == 0) fx.gather(hand, 3, 4, Palette.VOID, 8);
        });
        t.at(FORM, () -> orb[0] = stage.body().leftHand());
        t.span(FORM, FORM + DRIFT, (tick, p) -> {
            if (orb[0] == null) return;
            Vector toward = target.chest().subtract(orb[0]);
            if (toward.lengthSquared() > 0.25) orb[0].add(toward.normalize().multiply(SPEED));
            fx.draw(Shapes.sphere(orb[0], 1.5, 30), fx.dust(Palette.VOID_DEEP, 2.2f));
            fx.ring(orb[0], 2.6, 0.5, tick * 0.4, fx.dust(Palette.AMETHYST, 1.2f));
            fx.cloud(Particle.REVERSE_PORTAL, orb[0], 6, 1.2, 0.05);
            for (Victim v : stage.victimsIn(Area.sphere(orb[0], PULL))) {
                Vector in = orb[0].clone().subtract(v.chest());
                if (in.lengthSquared() < 1) continue;
                v.push(in.normalize().multiply(0.09));
            }
            if (tick % 10 == 0) fx.sound(orb[0], Sfx.WARDEN_HEARTBEAT, 1.4f, 1.2f);
        });
        t.at(FORM + DRIFT, () -> {
            if (orb[0] == null) return;
            fx.impact(orb[0], Palette.VOID, 2);
            fx.sound(orb[0], Sfx.EXPLODE, 2f, 1.1f);
            stage.hit(Area.sphere(orb[0], BLAST), seal(stage, 0.9), v -> {
                v.effect(Affliction.DARKNESS, 40, 0);
                v.push(Shapes.flat(v.position().subtract(orb[0])).multiply(0.9).setY(0.6));
            });
        });
        recover(t, stage, FORM + 4, FORM + 18, Poses.GUARD);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return FORM + 18;
    }

    @Override
    public String getName() {
        return "gravityorb";
    }
}
