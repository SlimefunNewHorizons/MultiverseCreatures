package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

/**
 * Obsidian Regeneration: shards of obsidian start orbiting the Sentinel's chest and it mends while it
 * keeps fighting, a pulse of health every half second for eight seconds. Unlike the healing circle it
 * does not kneel for it, so the only answer is to out-damage the mending.
 */
public class RegenerationAttack extends ChoreographedAttack {

    private static final int RAISE = 20;
    private static final int DURATION = 160;
    private static final int PULSE = 10;
    private static final double DEFAULT_HEAL = 0.05;
    private static final Color OBSIDIAN = Color.fromRGB(0x4B1C7A);

    public RegenerationAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return !instance.regenerating && !instance.healingCircleActive && !instance.isFlying;
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        BossInstance instance = stage.instance();
        if (instance != null) instance.regenerating = true;
        double total = stage.config("entities.armor-stand-boss.regeneration-heal-percent", DEFAULT_HEAL);
        int pulses = DURATION / PULSE;
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, RAISE, Poses.CHANNEL.withHead(-15, 0, 0), Ease.IN_OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.RESPAWN_ANCHOR_CHARGE, 2.5f, 0.6f));
        recover(t, stage, RAISE, RAISE + 12, Poses.GUARD);
        t.span(0, RAISE + DURATION, (tick, p) -> {
            Vector chest = stage.body().chest();
            Vector feet = stage.feet();
            if (tick % 2 == 0) {
                double open = Math.min(1, tick / (double) RAISE);
                fx.draw(Shapes.circle(chest, 4.0 * open, 10, Shapes.FLAT_U, Shapes.FLAT_V, tick * 0.15), fx.dust(OBSIDIAN, 2.2f));
                fx.draw(Shapes.helix(feet, 3.0, chest.getY() - feet.getY(), 1.5, 20, tick * 0.2),
                        fx.dust(Palette.AMETHYST, 1.2f).sometimes(0.5));
            }
            if (tick < RAISE || (tick - RAISE) % PULSE != 0) return;
            double gained = mend(instance, ofMaxHealth(instance, total) / pulses);
            fx.cloud(Particle.HEART, chest.clone().add(new Vector(0, 3, 0)), gained > 0 ? 3 : 1, 2, 0);
            fx.sound(chest, Sfx.AMETHYST_CHIME, 1.5f, 0.8f + (float) p * 0.6f);
        });
        t.onFinish(() -> {
            if (instance != null) instance.regenerating = false;
        });
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return RAISE + 12;
    }

    @Override
    public String getName() {
        return "regeneration";
    }
}
