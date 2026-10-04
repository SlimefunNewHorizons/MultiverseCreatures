package com.Chagui68.entities.boss.attack.ground;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
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
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Grave Cleaver: an overhead cleave that splits the floor in a long fissure towards the target. A
 * moment later obsidian shards burst out of the whole length of the crack.
 */
public class GraveCleaverAttack extends ChoreographedAttack.Ground {

    private static final int RAISE = 20;
    private static final int ERUPT = 14;
    private static final double LENGTH = 20;
    private static final int SHARDS = 10;

    public GraveCleaverAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Victim target = stage.target();
        Vector start = stage.onGround(stage.feet());
        Vector dir = Shapes.flat(target != null ? target.position().subtract(start) : stage.forward());
        Vector end = start.clone().add(dir.clone().multiply(LENGTH));
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        List<Quaternionf> tilts = new ArrayList<>();

        tweenTo(t, stage, 0, RAISE, Poses.SPEAR_OVERHEAD, Ease.OUT);
        t.span(0, RAISE + ERUPT, (tick, p) -> {
            if (tick % 2 == 0) Telegraph.line(stage, start, end, 2.6, Math.min(1, tick / (double) (RAISE + ERUPT)));
        });
        tween(t, stage, RAISE, RAISE + 4, Poses.SPEAR_OVERHEAD, Poses.SPEAR_SLAM, Ease.IN);
        t.at(RAISE + 3, () -> {
            fx.sound(start, Sfx.PLAYER_ATTACK_STRONG, 2.5f, 0.5f);
            fx.sound(start, Sfx.WITHER_BREAK_BLOCK, 2f, 0.7f);
            stage.hit(Area.segment(start, start.clone().add(dir.clone().multiply(6)), 2), seal(stage, 0.7), null);
        });
        t.span(RAISE + 3, RAISE + ERUPT, (tick, p) -> {
            Vector crack = stage.onGround(start.clone().add(dir.clone().multiply(LENGTH * p)));
            fx.crumble(Material.OBSIDIAN, 8, 0.4).at(crack.clone().add(new Vector(0, 0.2, 0)));
            fx.dust(Palette.VOID, 1.4f, 0.3, 3).at(crack.clone().add(new Vector(0, 0.2, 0)));
        });
        t.at(RAISE + ERUPT, () -> {
            for (int i = 0; i < SHARDS; i++) {
                Vector base = stage.onGround(start.clone().add(dir.clone().multiply(LENGTH * (i + 0.5) / SHARDS)));
                Vector lean = dir.clone().multiply(0.3).add(new Vector((stage.random().nextDouble() - 0.5) * 0.4, 1,
                        (stage.random().nextDouble() - 0.5) * 0.4));
                Quaternionf tilt = Prop.pointing(lean);
                Prop shard = stage.block(Material.OBSIDIAN, base, 0.9f, tilt);
                shard.resize(0.9f, 0.05f, tilt, 0);
                props.add(shard);
                tilts.add(tilt);
            }
            fx.sound(start.clone().add(dir.clone().multiply(LENGTH / 2)), Sfx.POINTED_DRIPSTONE_LAND, 2.5f, 0.6f);
            for (Victim v : stage.victimsIn(Area.segment(start, end, 1.6))) {
                stage.damage(v, seal(stage, 0.8));
                v.effect(Affliction.SLOWNESS, 50, 2);
                v.push(new Vector(0, 0.9, 0));
            }
        });
        t.at(RAISE + ERUPT + 1, () -> {
            for (int i = 0; i < props.size(); i++) props.get(i).resize(0.9f, 3.2f, tilts.get(i), 3);
        });
        t.at(RAISE + ERUPT + 26, () -> {
            for (int i = 0; i < props.size(); i++) {
                fx.crumble(Material.OBSIDIAN, 6, 0.4).at(props.get(i).position().add(new Vector(0, 1, 0)));
                props.get(i).resize(0.9f, 0.05f, tilts.get(i), 6);
            }
        });
        recover(t, stage, RAISE + 8, RAISE + 24, Poses.GUARD);
        t.hold(RAISE + ERUPT + 34);
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return RAISE + 24;
    }

    @Override
    public String getName() {
        return "gravecleaver";
    }
}
