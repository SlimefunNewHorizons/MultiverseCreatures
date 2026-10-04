package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
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
 * Judgment Pillars: the Sentinel calls the heavens to judge the arena. Six rings of light open in a
 * circle around it and one more under every player; they fire one after another as columns of light
 * thirty blocks tall, each burning a three-block circle.
 */
public class JudgmentPillarsAttack extends DestructiveAttack {

    private static final int CHARGE = 50;
    private static final int GAP = 8;
    private static final int WARN = 30;
    private static final double RADIUS = 3;

    public JudgmentPillarsAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        List<Vector> sites = new ArrayList<>();
        double start = stage.random().nextDouble() * Math.PI * 2;
        for (int i = 0; i < 6; i++) {
            sites.add(stage.onGround(stage.feet().add(Shapes.heading(start + i * Math.PI / 3).multiply(11))));
        }
        for (Victim v : stage.victims()) {
            if (sites.size() >= 12) break;
            if (v.position().distanceSquared(stage.feet()) < 50 * 50) sites.add(stage.onGround(v.position()));
        }
        int last = CHARGE + GAP * (sites.size() - 1);
        Timeline t = new Timeline();
        List<Prop> props = props(t);
        Prop[] columns = new Prop[sites.size()];

        tweenTo(t, stage, 0, 24, Poses.SPEAR_RAISED.withHead(-30, 0, 0), Ease.OUT);
        t.at(0, () -> {
            fx.sound(stage.feet(), Sfx.BELL_RESONATE, 3f, 0.5f);
            hud(stage, stage.feet(), 60, "✦ JUDGMENT", "Leave the circles of light before they fire");
        });
        t.span(0, CHARGE, (tick, p) -> {
            if (tick % 6 == 0) stage.lightning(stage.feet().add(Shapes.heading(stage.random().nextDouble() * 6.28).multiply(14)));
            fx.line(stage.body().spearTip(), stage.body().spearTip().add(new Vector(0, 30, 0)), 0.6, fx.dust(Palette.HOLY, 1.6f).sometimes(0.6));
        });
        for (int i = 0; i < sites.size(); i++) {
            Vector site = sites.get(i);
            int index = i;
            int fire = CHARGE + GAP * i;
            t.span(Math.max(0, fire - WARN), fire, (tick, p) -> {
                if (tick % 2 == 0) Telegraph.circle(stage, site, RADIUS, p);
                fx.line(site.clone().add(new Vector(0, 30, 0)), site.clone().add(new Vector(0, 30 - 28 * p, 0)), 1.0,
                        fx.dust(Palette.HOLY, 1.0f).sometimes(0.5));
            });
            t.at(fire, () -> {
                Quaternionf upright = new Quaternionf();
                Prop column = stage.block(Material.SEA_LANTERN, site, (float) RADIUS, upright);
                column.resize((float) RADIUS, 30f, upright, 2);
                column.glow(Palette.HOLY);
                props.add(column);
                columns[index] = column;
                fx.flash(site.clone().add(new Vector(0, 2, 0)), Palette.HOLY);
                fx.sound(site, Sfx.LIGHTNING_IMPACT, 2.5f, 0.8f);
                stage.hit(Area.cylinder(site, RADIUS, 1, 30), seal(stage, 1.3), v -> {
                    v.effect(Affliction.SLOWNESS, 40, 1);
                    v.push(new Vector(0, 0.8, 0));
                });
                crater(stage, site, RADIUS, 6);
            });
            t.at(fire + 14, () -> {
                if (columns[index] != null) columns[index].resize((float) RADIUS, 0.05f, new Quaternionf(), 8);
            });
        }
        recover(t, stage, last, last + 20, Poses.GUARD);
        t.hold(last + 30);
        return t;
    }

    @Override
    public String getName() {
        return "judgmentpillars";
    }
}
