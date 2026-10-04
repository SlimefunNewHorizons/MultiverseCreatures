package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
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
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Obsidian Cocoon: pillars of obsidian close around the kneeling Sentinel and seal it in. For three
 * seconds nothing can hurt it and it mends inside; then the cocoon bursts outwards in a shockwave.
 */
public class ObsidianCocoonAttack extends ChoreographedAttack {

    private static final int CLOSE = 16;
    private static final int HOLD = 60;
    private static final int WAVE = 16;
    private static final int PILLARS = 10;
    private static final double RADIUS = 2.6;
    private static final double DEFAULT_HEAL = 0.04;

    public ObsidianCocoonAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return !instance.regenerating && !instance.healingCircleActive && !instance.isFlying
                && !instance.invulnerable && boss.isOnGround(instance.stand);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        BossInstance instance = stage.instance();
        if (instance != null) instance.regenerating = true;
        double total = stage.config("entities.armor-stand-boss.obsidian-cocoon-heal-percent", DEFAULT_HEAL);
        Vector feet = stage.onGround(stage.feet());
        double height = stage.body().head().getY() - feet.getY() + 1.5;
        Timeline t = new Timeline();
        List<Prop> pillars = props(t);

        tweenTo(t, stage, 0, CLOSE, Poses.KNEEL.withHead(10, 0, 0), Ease.IN_OUT);
        for (int i = 0; i < PILLARS; i++) {
            double angle = 2 * Math.PI * i / PILLARS;
            Vector base = stage.onGround(Shapes.onCircle(feet, RADIUS, angle, Shapes.FLAT_U, Shapes.FLAT_V));
            pillar(t, stage, 0, base, i % 3 == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN, 1.6f, height, CLOSE, HOLD, pillars);
        }
        t.at(0, () -> fx.sound(feet, Sfx.WITHER_BREAK_BLOCK, 2f, 0.5f));
        t.at(CLOSE, () -> {
            if (instance != null) {
                instance.invulnerable = true;
                instance.invulnerableTimer = HOLD;
            }
            fx.flash(stage.body().chest(), Palette.VOID);
            fx.sound(feet, Sfx.ANVIL_LAND, 2.5f, 0.5f);
        });
        t.span(CLOSE, CLOSE + HOLD, (tick, p) -> {
            mend(instance, ofMaxHealth(instance, total) / HOLD);
            Vector chest = stage.body().chest();
            fx.cloud(Particle.DRIPPING_OBSIDIAN_TEAR, chest, 4, RADIUS, 2, 0);
            if (tick % 15 == 0) {
                fx.cloud(Particle.HEART, chest.clone().add(new Vector(0, 3, 0)), 3, 2, 0);
                fx.sound(feet, Sfx.RESPAWN_ANCHOR_CHARGE, 1.4f, 0.8f + (float) p * 0.5f);
            }
        });
        t.at(CLOSE + HOLD, () -> {
            if (instance != null) {
                instance.invulnerable = false;
                instance.invulnerableTimer = 0;
            }
            // The pillars sink back into the floor on their own; the burst throws chunks off them.
            for (Prop pillar : pillars) {
                fx.crumble(Material.OBSIDIAN, 12, 0.7).at(pillar.position());
                stage.debris(pillar.position().add(new Vector(0, 2, 0)),
                        Shapes.flat(pillar.position().subtract(feet)).multiply(0.4).setY(0.5), Material.OBSIDIAN, 24);
            }
            fx.impact(stage.body().chest(), Palette.VOID, 3);
            fx.sound(feet, Sfx.EXPLODE, 2.5f, 0.6f);
        });
        shockwave(t, stage, CLOSE + HOLD, WAVE, feet, 10, Palette.VOID, seal(stage, 0.6),
                v -> v.push(Shapes.flat(v.position().subtract(feet)).multiply(1.2).setY(0.5)));
        recover(t, stage, CLOSE + HOLD, CLOSE + HOLD + 16, Poses.GUARD);
        t.onFinish(() -> {
            if (instance == null) return;
            instance.regenerating = false;
            if (instance.invulnerableTimer <= 0) instance.invulnerable = false;
        });
        return t;
    }

    @Override
    public String getName() {
        return "obsidiancocoon";
    }
}
