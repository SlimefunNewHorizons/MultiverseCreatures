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
 * Void Collapse: a black hole opens over the arena and drags everything towards it, harder every
 * second, for four seconds. Then it collapses into a blast nine blocks wide. Run against the pull.
 */
public class VoidCollapseAttack extends DestructiveAttack {

    private static final int PULL = 80;
    private static final double REACH = 26;
    private static final double BLAST = 9;

    public VoidCollapseAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Victim target = stage.target();
        Vector floor = stage.onGround(target != null ? target.position().midpoint(stage.feet()) : stage.feet().add(stage.forward().multiply(10)));
        Vector hole = floor.clone().add(new Vector(0, 6, 0));
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, 20, Poses.CAST_FORWARD.withLeftArm(-100, -20, -10), Ease.OUT);
        t.at(0, () -> {
            fx.sound(hole, Sfx.END_PORTAL_SPAWN, 3f, 0.4f);
            hud(stage, hole, 60, "◉ VOID COLLAPSE", "Run against the pull");
        });
        t.span(0, PULL, (tick, p) -> {
            double r = 0.5 + 2.5 * Ease.at(Ease.OUT, p);
            fx.draw(Shapes.sphere(hole, r, (int) (30 + r * 20)), fx.dust(Palette.VOID_DEEP, 2.6f));
            fx.ring(hole, r * 2.2, 0.5, tick * 0.3, fx.dust(Palette.AMETHYST, 1.4f));
            fx.ring(hole, r * 3.0, 0.7, -tick * 0.2, fx.dust(Palette.VOID, 1.2f).sometimes(0.6));
            if (tick % 2 == 0) fx.gather(hole, REACH * 0.6, 5, Palette.VOID, 18);
            if (tick % 2 == 0) reticle(stage, floor, BLAST, p, Palette.VOID);
            double strength = 0.04 + 0.1 * p;
            for (Victim v : stage.victimsIn(Area.sphere(hole, REACH))) {
                Vector toward = hole.clone().subtract(v.position());
                if (toward.lengthSquared() < 1) continue;
                v.push(toward.normalize().multiply(strength));
            }
            if (tick % 20 == 0) fx.sound(hole, Sfx.WARDEN_HEARTBEAT, 3f, 0.4f + (float) p);
        });
        t.at(PULL, () -> {
            fx.flash(hole, Palette.VOID);
            fx.sound(hole, Sfx.BEACON_DEACTIVATE, 3f, 0.4f);
        });
        tweenTo(t, stage, PULL, PULL + 8, Poses.ROAR, Ease.IN);
        dome(t, stage, PULL + 6, 14, floor, BLAST, Palette.VOID, seal(stage, 1.6), v -> {
            v.effect(Affliction.DARKNESS, 80, 0);
            v.push(Shapes.flat(v.position().subtract(floor)).multiply(1.6).setY(0.8));
        });
        t.at(PULL + 6, () -> fx.burst(hole, Particle.REVERSE_PORTAL, 120, 1.2));
        recover(t, stage, PULL + 12, PULL + 30, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "voidcollapse";
    }
}
