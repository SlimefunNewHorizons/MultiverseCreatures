package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.BossInstance.DefenseState;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Prop;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Bulwark: the Sentinel plants its feet and a ring of blackstone ramparts rises around it. Braced, it
 * takes about a third of the damage, but it cannot move or attack until the ramparts fall: the
 * moment to reposition and heal, not to trade blows.
 */
public class BulwarkAttack extends ChoreographedAttack {

    private static final int RAISE = 16;
    private static final int RAMPARTS = 8;
    private static final double RADIUS = 5.5;
    private static final int DEFAULT_DURATION = 120;

    public BulwarkAttack(BossHost boss) {
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
            instance.activeDefense = DefenseState.BULWARK;
            instance.defenseTimer = 0;
        }
        int duration = (int) Math.max(1, stage.config("entities.armor-stand-boss.defense-duration-bulwark-ticks", DEFAULT_DURATION));
        Vector feet = stage.onGround(stage.feet());
        Timeline t = new Timeline();
        List<Prop> ramparts = props(t);

        tweenTo(t, stage, 0, RAISE, Poses.SHIELD_WALL, Ease.IN_OUT);
        t.at(0, () -> fx.sound(feet, Sfx.IRON_GOLEM_ATTACK, 2.5f, 0.5f));
        for (int i = 0; i < RAMPARTS; i++) {
            double angle = 2 * Math.PI * i / RAMPARTS;
            Vector base = stage.onGround(Shapes.onCircle(feet, RADIUS, angle, Shapes.FLAT_U, Shapes.FLAT_V));
            pillar(t, stage, 0, base, i % 2 == 0 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.GILDED_BLACKSTONE,
                    2.4f, 4.0, RAISE, duration, ramparts);
        }
        t.at(RAISE, () -> {
            fx.flash(stage.body().chest(), Palette.STONE);
            fx.sound(feet, Sfx.ANVIL_LAND, 2.5f, 0.6f);
        });
        t.span(RAISE, RAISE + duration, (tick, p) -> {
            boolean active = instance == null || instance.activeDefense == DefenseState.BULWARK;
            if (!active) {
                t.stop();
                return;
            }
            if (tick % 4 == 0) {
                fx.ring(feet.clone().add(new Vector(0, 0.3, 0)), RADIUS, 0.6, tick * 0.1, fx.dust(Palette.STONE, 1.4f));
                fx.ring(feet.clone().add(new Vector(0, 4.2, 0)), RADIUS, 0.9, -tick * 0.1, fx.dust(Palette.GOLD, 1.0f).sometimes(0.5));
            }
        });
        recover(t, stage, RAISE + duration, RAISE + duration + 12, Poses.GUARD);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return timeline.length();
    }

    @Override
    public String getName() {
        return "bulwark";
    }
}
