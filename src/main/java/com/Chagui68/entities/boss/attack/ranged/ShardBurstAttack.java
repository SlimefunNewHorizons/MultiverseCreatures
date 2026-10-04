package com.Chagui68.entities.boss.attack.ranged;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Missile;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Prop;
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
import java.util.List;

/**
 * Shard Burst: obsidian splinters gather in front of the Sentinel's shield, then it punches them out
 * in a wide fan of nine shards, each one a solid block of glass-sharp stone.
 */
public class ShardBurstAttack extends ChoreographedAttack.Ranged {

    private static final int GATHER = 20;
    private static final int SHARDS = 9;
    private static final double SPREAD = Math.toRadians(35);
    private static final double SPEED = 1.6;

    public ShardBurstAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        if (target == null) return null;
        Fx fx = stage.fx();
        double damage = seal(stage, 0.5);
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        List<Missile> missiles = new ArrayList<>();
        cleanupMissiles(t, missiles);

        tweenTo(t, stage, 0, GATHER, Poses.SHIELD_WALL, Ease.IN_OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.AMETHYST_CHIME, 2f, 0.5f));
        t.span(0, GATHER, (tick, p) -> {
            Vector face = stage.body().shieldFace();
            if (tick % 2 == 0) fx.gather(face, 4, 4, Palette.VOID, 8);
            fx.crumble(Material.OBSIDIAN, 2, 0.6).at(face);
            if (tick % 2 == 0) Telegraph.cone(stage, stage.feet(), target.position().subtract(stage.feet()), SPREAD, 24, p);
        });
        tween(t, stage, GATHER, GATHER + 4, Poses.SHIELD_WALL, Poses.THRUST, Ease.OUT_BACK);
        t.at(GATHER + 1, () -> {
            Vector from = stage.body().shieldFace();
            Vector aim = target.chest().subtract(from);
            double base = Math.atan2(aim.getZ(), aim.getX());
            double pitch = aim.getY() / Math.max(1, Math.hypot(aim.getX(), aim.getZ()));
            fx.sound(from, Sfx.GLASS_BREAK, 2.5f, 0.6f);
            for (int i = 0; i < SHARDS; i++) {
                double a = base - SPREAD + 2 * SPREAD * i / (SHARDS - 1);
                Vector velocity = Shapes.heading(a).setY(pitch).normalize().multiply(SPEED);
                Prop shard = stage.block(Material.OBSIDIAN, from, 0.4f, Prop.pointing(velocity));
                shard.resize(0.4f, 1.2f, Prop.pointing(velocity), 0);
                props.add(shard);
                Missile missile = new Missile(from, velocity, 0.9)
                        .carrying(shard)
                        .look((at, dir, age) -> fx.dust(Palette.AMETHYST, 1.0f).at(at))
                        .onHit(victim -> {
                            stage.damage(victim, damage);
                            victim.effect(Affliction.WEAKNESS, 60, 0);
                        })
                        .onBurst(at -> fx.crumble(Material.OBSIDIAN, 10, 0.3).at(at));
                missiles.add(missile);
                fly(t, stage, GATHER + 2, 30, missile);
            }
        });
        recover(t, stage, GATHER + 6, GATHER + 20, Poses.GUARD);
        t.hold(GATHER + 34);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return GATHER + 20;
    }

    @Override
    public String getName() {
        return "shardburst";
    }
}
