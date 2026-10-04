package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

/**
 * World Breaker: the Sentinel crouches, the ground cracking under its weight, and leaps thirty blocks
 * into the sky. It hangs there while the landing zone burns red, then falls like a meteor: the impact
 * is a blast and three shockwaves rolling out to twenty blocks, each one a jump to clear.
 */
public class WorldBreakerAttack extends DestructiveAttack {

    private static final int CROUCH = 30;
    private static final int RISE = 30;
    private static final int HANG = 24;
    private static final int DROP = 8;
    private static final double HEIGHT = 30;
    private static final double RADIUS = 7;

    public WorldBreakerAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        BossInstance instance = stage.instance();
        Victim target = stage.target();
        Vector origin = stage.feet();
        Vector[] landing = {stage.onGround(target != null ? target.position() : origin.clone().add(stage.forward().multiply(12)))};
        Material ground = stage.groundMaterial(origin);
        int up = CROUCH;
        int top = up + RISE;
        int fall = top + HANG;
        int impact = fall + DROP;
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, CROUCH, Poses.KNEEL.withRightArm(-30, 0, 20).withHead(10, 0, 0), Ease.IN_OUT);
        t.at(0, () -> {
            fx.sound(origin, Sfx.WARDEN_ROAR, 3f, 0.5f);
            hud(stage, landing[0], 60, "☄ WORLD BREAKER", "Jump the shockwaves");
        });
        t.span(0, CROUCH, (tick, p) -> {
            if (tick % 3 == 0) fx.crumble(ground, 10, 1.2).at(stage.onGround(origin).add(new Vector(0, 0.3, 0)));
            if (tick % 2 == 0) fx.ring(stage.onGround(origin).add(new Vector(0, 0.2, 0)), 3 + 3 * p, 0.6, tick, fx.dust(Palette.EMBER, 1.4f));
        });
        t.at(up, () -> {
            if (instance != null) instance.airborneAttack = true;
            if (target != null) landing[0] = stage.onGround(target.position());
            fx.flatBurst(stage.onGround(origin), Particle.CLOUD, 50, 0.6);
            fx.sound(origin, Sfx.WIND_CHARGE_BURST, 3f, 0.5f);
        });
        tweenTo(t, stage, up, top, Poses.HOVER, Ease.OUT);
        t.span(up, top, (tick, p) -> {
            double rise = Ease.at(Ease.OUT, (tick + 1) / (double) RISE);
            Vector at = origin.clone().add(landing[0].clone().subtract(origin).multiply(rise * 0.5)).add(new Vector(0, HEIGHT * rise, 0));
            stage.moveTo(at);
            fx.cloud(Particle.FLAME, stage.feet(), 6, 1.2, 0.05);
        });
        t.span(top, fall, (tick, p) -> {
            if (target != null && tick < HANG / 2) landing[0] = stage.onGround(target.position());
            if (tick % 2 == 0) reticle(stage, landing[0], RADIUS * 2, p, Palette.EMBER);
            fx.line(stage.feet(), landing[0], 1.2, fx.dust(Palette.WARNING, 1.0f).sometimes(0.5));
        });
        tweenTo(t, stage, top, fall, Poses.DIVE, Ease.IN_OUT);
        Vector[] hangAt = new Vector[1];
        t.at(fall, () -> hangAt[0] = stage.feet());
        t.span(fall, impact, (tick, p) -> {
            if (hangAt[0] == null) return;
            double f = Ease.at(Ease.IN, (tick + 1) / (double) DROP);
            stage.moveTo(hangAt[0].clone().add(landing[0].clone().subtract(hangAt[0]).multiply(f)));
            fx.cloud(Particle.FLAME, stage.feet(), 12, 1.5, 0.1);
        });
        t.at(impact, () -> {
            stage.moveTo(landing[0]);
            if (instance != null) instance.airborneAttack = false;
            fx.sound(landing[0], Sfx.MACE_SMASH_GROUND, 3f, 0.4f);
            // The three shockwaves are scheduled from the landing spot, which moved until the drop.
            shockwave(t, stage, impact, 16, landing[0].clone(), 10, Palette.EMBER, seal(stage, 0.9),
                    v -> v.push(Shapes.flat(v.position().subtract(landing[0])).multiply(1.0).setY(0.6)));
            shockwave(t, stage, impact + 10, 20, landing[0].clone(), 15, Palette.ASH, seal(stage, 0.7), null);
            shockwave(t, stage, impact + 22, 24, landing[0].clone(), 20, Palette.STONE, seal(stage, 0.5), null);
            dome(t, stage, impact + 1, 10, landing[0].clone(), RADIUS, Palette.EMBER, seal(stage, 1.5),
                    v -> v.push(new Vector(0, 1.2, 0)));
        });
        tweenTo(t, stage, impact, impact + 6, Poses.STOMP_DOWN, Ease.OUT);
        recover(t, stage, impact + 14, impact + 34, Poses.GUARD);
        t.hold(impact + 50);
        t.onFinish(() -> {
            if (instance != null) instance.airborneAttack = false;
        });
        return t;
    }

    @Override
    public String getName() {
        return "worldbreaker";
    }
}
