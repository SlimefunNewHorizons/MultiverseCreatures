package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
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
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Earth Splitter: the Sentinel lifts its spear over its head while the floor groans and cracks open
 * in a giant cross aimed at the target. When the spear comes down, the four fissures tear outwards
 * thirty blocks, throwing molten rock and everyone standing on them into the air.
 */
public class EarthSplitterAttack extends DestructiveAttack {

    private static final int RAISE = 70;
    private static final int TEAR = 22;
    private static final double LENGTH = 30;
    private static final double WIDTH = 3;

    public EarthSplitterAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Vector origin = stage.onGround(stage.feet());
        Victim target = stage.target();
        Vector aim = Shapes.flat(target != null ? target.position().subtract(origin) : stage.forward());
        List<Vector> dirs = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            double a = Math.atan2(aim.getZ(), aim.getX()) + k * Math.PI / 2;
            dirs.add(Shapes.heading(a));
        }
        Material ground = stage.groundMaterial(origin);
        Set<UUID> struck = new HashSet<>();
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, RAISE - 10, Poses.SPEAR_OVERHEAD.withHead(-20, 0, 0), Ease.IN_OUT);
        t.at(0, () -> {
            fx.sound(origin, Sfx.WARDEN_ROAR, 2.5f, 0.6f);
            hud(stage, origin, 50, "⛰ EARTH SPLITTER", "Stand between the cracks");
        });
        t.span(0, RAISE, (tick, p) -> {
            if (tick % 2 == 0) {
                for (Vector dir : dirs) Telegraph.line(stage, origin, origin.clone().add(dir.clone().multiply(LENGTH)), WIDTH, p);
            }
            if (tick % 4 == 0) {
                Vector shake = origin.clone().add(Shapes.heading(stage.random().nextDouble() * 6.28).multiply(stage.random().nextDouble() * 8));
                fx.crumble(ground, 8, 0.6).at(stage.onGround(shake).add(new Vector(0, 0.3, 0)));
            }
            if (tick % 14 == 0) fx.sound(origin, Sfx.WITHER_BREAK_BLOCK, 1.6f, 0.5f + (float) p * 0.5f);
            fx.cloud(Particle.FLAME, stage.body().spearTip(), 3, 0.5, 0.02);
        });
        tween(t, stage, RAISE - 10, RAISE, Poses.SPEAR_OVERHEAD, Poses.SPEAR_SLAM, Ease.IN);
        t.at(RAISE, () -> {
            fx.impact(origin.clone().add(new Vector(0, 1, 0)), Palette.EMBER, 4);
            fx.sound(origin, Sfx.MACE_SMASH_GROUND, 3f, 0.4f);
            fx.sound(origin, Sfx.EXPLODE, 3f, 0.5f);
        });
        t.span(RAISE, RAISE + TEAR, (tick, p) -> {
            double before = LENGTH * tick / TEAR;
            double after = LENGTH * (tick + 1) / TEAR;
            for (Vector dir : dirs) {
                Vector a = origin.clone().add(dir.clone().multiply(before));
                Vector b = origin.clone().add(dir.clone().multiply(after));
                Vector mid = stage.onGround(b);
                fx.flatBurst(mid, Particle.LAVA, 6, 0.3);
                fx.crumble(ground, 14, 0.8).at(mid.clone().add(new Vector(0, 0.4, 0)));
                fx.line(mid, mid.clone().add(new Vector(0, 3, 0)), 0.4, fx.dust(Palette.MOLTEN, 2.0f));
                if (tick % 2 == 0) stage.debris(mid, new Vector(0, 0.7, 0).add(dir.clone().multiply(0.1)), ground, 24);
                for (Victim v : stage.victimsIn(Area.segment(a, b, WIDTH / 2 + 0.5))) {
                    if (!struck.add(v.id())) continue;
                    stage.damage(v, seal(stage, 1.5));
                    v.ignite(60);
                    v.push(new Vector(0, 1.3, 0));
                }
            }
            if (tick % 4 == 0) fx.sound(origin.clone().add(dirs.get(0).clone().multiply(after)), Sfx.EXPLODE, 1.6f, 0.7f);
        });
        recover(t, stage, RAISE + 10, RAISE + 30, Poses.GUARD);
        return t;
    }

    @Override
    public String getName() {
        return "earthsplitter";
    }
}
