package com.Chagui68.entities.boss.attack.aerial;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Spiral Storm: hovering, the Sentinel spins its spear over its head and looses a stream of lances
 * that come down along a spiral wound around the target, from the outside in. The spiral is drawn on
 * the floor point by point just ahead of the lances.
 */
public class SpiralStormAttack extends ChoreographedAttack.Aerial {

    private static final int WIND = 16;
    private static final int LANCES = 22;
    private static final int EVERY = 2;
    private static final int WARN = 14;
    private static final double OUTER = 11;
    private static final double RADIUS = 1.8;

    public SpiralStormAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        Fx fx = stage.fx();
        Vector center = stage.onGround(target != null ? target.position() : stage.feet());
        List<Vector> spiral = new ArrayList<>();
        for (int i = 0; i < LANCES; i++) {
            double f = i / (double) (LANCES - 1);
            spiral.add(stage.onGround(center.clone().add(Shapes.heading(f * Math.PI * 5).multiply(OUTER * (1 - f) + 0.5))));
        }
        double damage = seal(stage, 0.45);
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, WIND, Poses.SPEAR_RAISED.withBody(0, 20, 0), Ease.OUT);
        t.at(0, () -> fx.sound(stage.feet(), Sfx.BREEZE_WIND_BURST, 2f, 0.6f));
        t.span(0, WIND + LANCES * EVERY, (tick, p) -> {
            Vector tip = stage.body().spearTip();
            fx.draw(Shapes.circle(tip, 2.5, 12, Shapes.FLAT_U, Shapes.FLAT_V, tick * 0.6), fx.dust(Palette.STORM, 1.3f));
            fx.cloud(Particle.CLOUD, tip, 1, 0.5, 0.02);
        });
        for (int i = 0; i < LANCES; i++) {
            Vector spot = spiral.get(i);
            int land = WIND + WARN + i * EVERY;
            t.span(land - WARN, land, (tick, p) -> {
                if (tick % 2 == 0) Telegraph.circle(stage, spot, RADIUS, p);
                if (tick >= WARN - 4) {
                    double f = (tick - (WARN - 4) + 1) / 4.0;
                    Vector from = stage.body().spearTip();
                    Vector at = from.clone().add(spot.clone().subtract(from).multiply(f));
                    fx.line(at, at.clone().add(from.clone().subtract(spot).normalize().multiply(2.5)), 0.3, fx.dust(Palette.ICE, 1.4f));
                }
            });
            t.at(land, () -> {
                fx.flatBurst(spot, Particle.CLOUD, 10, 0.25);
                fx.dust(Palette.STORM, 1.8f, 0.4, 6).at(spot.clone().add(new Vector(0, 0.4, 0)));
                fx.sound(spot, Sfx.TRIDENT_THROW, 1.2f, 1.3f);
                stage.hit(Area.cylinder(spot, RADIUS, 1, 3), damage, null);
            });
        }
        int end = WIND + WARN + LANCES * EVERY;
        tweenTo(t, stage, end, end + 14, Poses.HOVER, Ease.IN_OUT);
        return t;
    }

    @Override
    public String getName() {
        return "spiralstorm";
    }
}
