package com.Chagui68.entities.boss.attack.ranged;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Missile;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Prop;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Javelin Volley: three javelins thrown in quick succession, each one aimed where the target will
 * be rather than where it is — running in a straight line is the way to be hit. Change direction.
 */
public class JavelinVolleyAttack extends ChoreographedAttack.Ranged {

    private static final int COIL = 12;
    private static final int GAP = 10;
    private static final int THROWS = 3;
    private static final double SPEED = 2.4;

    public JavelinVolleyAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        if (target == null) return null;
        Fx fx = stage.fx();
        double damage = seal(stage, 0.7);
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        List<Missile> missiles = new ArrayList<>();
        cleanupMissiles(t, missiles);
        // Where the target stood on the previous tick and on this one: its velocity, for the lead.
        Vector[] seen = {target.position(), target.position()};

        t.span(0, COIL + GAP * THROWS, (tick, p) -> {
            seen[0] = seen[1];
            seen[1] = target.position();
        });
        for (int i = 0; i < THROWS; i++) {
            int release = COIL + GAP * i;
            tween(t, stage, release - COIL / 2 - (i == 0 ? COIL / 2 : 0), release, Poses.GUARD, Poses.THROW_COIL, Ease.IN_OUT);
            tween(t, stage, release, release + 4, Poses.THROW_COIL, Poses.THROW, Ease.OUT_BACK);
            t.at(release, () -> {
                Vector from = stage.body().rightHand().add(new Vector(0, 1, 0));
                Vector now = target.chest();
                // Lead the target by its motion over the flight time.
                Vector motion = seen[1].clone().subtract(seen[0]);
                double flight = now.distance(from) / SPEED;
                Vector aim = now.clone().add(motion.multiply(flight)).subtract(from);
                Vector velocity = aim.normalize().multiply(SPEED);
                Prop javelin = stage.item(Material.TRIDENT, from, 1.6f, diagonal(velocity));
                props.add(javelin);
                fx.sound(from, Sfx.TRIDENT_THROW, 2f, 1.0f);
                Missile missile = new Missile(from, velocity, 1.0)
                        .gravity(0.02)
                        .carrying(javelin)
                        .look((at, dir, age) -> fx.line(at, at.clone().subtract(dir.clone().multiply(2)), 0.4, fx.dust(Palette.GOLD, 1.0f)))
                        .onHit(victim -> {
                            stage.damage(victim, damage);
                            victim.push(velocity.clone().normalize().multiply(0.6).setY(0.3));
                        })
                        .onBurst(at -> {
                            fx.cloud(Particle.CRIT, at, 12, 0.4, 0.2);
                            fx.sound(at, Sfx.TRIDENT_THUNDER, 0.8f, 1.6f);
                        });
                missiles.add(missile);
                fly(t, stage, release + 1, 30, missile);
            });
        }
        int end = COIL + GAP * THROWS;
        recover(t, stage, end, end + 14, Poses.GUARD);
        t.hold(end + 32);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return COIL + GAP * THROWS + 14;
    }

    @Override
    public String getName() {
        return "javelinvolley";
    }
}
