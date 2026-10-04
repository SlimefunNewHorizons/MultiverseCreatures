package com.Chagui68.entities.boss.attack.ground;

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
import com.Chagui68.entities.boss.fx.Telegraph;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Aegis Rush: the Sentinel lowers its shield and charges sixteen blocks in a straight line, throwing
 * everyone in its path aside. The lane is drawn on the floor before it starts running.
 */
public class AegisRushAttack extends ChoreographedAttack.Ground {

    private static final int BRACE = 22;
    private static final int RUN = 14;
    private static final double LENGTH = 16;
    private static final double WIDTH = 4;

    public AegisRushAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Victim target = stage.target();
        Vector start = stage.feet();
        Vector dir = Shapes.flat(target != null ? target.position().subtract(start) : stage.forward());
        Vector end = start.clone().add(dir.clone().multiply(LENGTH));
        double damage = seal(stage, 0.9);
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, BRACE, Poses.SHIELD_WALL.withBody(15, 0, 0), Ease.IN_OUT);
        t.at(0, () -> fx.sound(start, Sfx.RAVAGER_ROAR, 2.5f, 0.7f));
        t.span(0, BRACE, (tick, p) -> {
            if (tick % 2 == 0) Telegraph.line(stage, start, end, WIDTH, p);
            fx.cloud(Particle.CLOUD, start, 2, 1.2, 0.02);
        });
        t.span(BRACE, BRACE + RUN, (tick, p) -> {
            Vector before = stage.feet();
            stage.walk(dir.clone().multiply(LENGTH / RUN));
            Vector after = stage.feet();
            fx.cloud(Particle.CLOUD, after, 6, 1.5, 0.05);
            fx.crumble(stage.groundMaterial(after), 8, 1.2).at(after.clone().add(new Vector(0, 0.3, 0)));
            fx.dust(Palette.FROST, 2f, 1.5, 6).at(stage.body().shieldFace());
            if (tick % 3 == 0) fx.sound(after, Sfx.IRON_GOLEM_ATTACK, 1.6f, 0.8f);
            for (Victim v : stage.victimsIn(Area.segment(before, after, WIDTH / 2 + 0.8))) {
                if (!struck.add(v.id())) continue;
                stage.damage(v, damage);
                v.effect(Affliction.SLOWNESS, 40, 1);
                Vector sideways = new Vector(-dir.getZ(), 0, dir.getX());
                if (sideways.dot(v.position().subtract(after)) < 0) sideways.multiply(-1);
                v.fling(sideways.multiply(1.2).add(dir.clone().multiply(0.6)).setY(0.7));
            }
        });
        t.at(BRACE + RUN, () -> {
            fx.impact(stage.body().shieldFace(), Palette.FROST, 1.4);
            fx.sound(stage.feet(), Sfx.SHIELD_BLOCK, 2.5f, 0.6f);
        });
        recover(t, stage, BRACE + RUN, BRACE + RUN + 14, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "aegisrush";
    }
}
