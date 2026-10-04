package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.util.Vector;

/**
 * Orbital Strike: the Sentinel raises its spear and signals the sky. A targeting grid finds the
 * target and follows it, every screen in range flashes the warning, the lock closes, beams converge
 * from orbit — and a red dome of light ten blocks wide comes down where the grid was.
 */
public class OrbitalStrikeAttack extends DestructiveAttack {

    private static final int SIGNAL = 20;
    private static final int LOCK = 70;
    private static final int IMPACT = 92;
    private static final double RADIUS = 10;

    public OrbitalStrikeAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Victim target = stage.target();
        if (target == null) return null;
        Fx fx = stage.fx();
        Vector[] aim = {target.position()};
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, SIGNAL, Poses.CAST_SKY, Ease.OUT);
        t.at(0, () -> {
            fx.sound(stage.feet(), Sfx.BEACON_ACTIVATE, 3f, 0.6f);
            hud(stage, stage.feet(), 60, "⚠ TARGET ACQUIRED", "Orbital strike inbound — get out of the grid");
        });
        t.span(0, SIGNAL, (tick, p) -> {
            Vector tip = stage.body().spearTip();
            fx.line(tip, tip.clone().add(new Vector(0, 60 * p, 0)), 0.8, fx.dust(STRIKE, 1.6f));
        });
        t.span(0, IMPACT, (tick, p) -> {
            if (tick < LOCK) aim[0] = aim[0].clone().add(target.position().subtract(aim[0]).multiply(0.15));
            if (tick % 2 == 0) reticle(stage, aim[0], RADIUS, Math.min(1, tick / (double) LOCK), STRIKE);
            if (tick % 10 == 0) fx.sound(aim[0], Sfx.NOTE_HAT, 2f, 0.6f + (float) p);
        });
        t.at(LOCK, () -> {
            hud(stage, aim[0], 60, "IMPACT CONFIRMED", "LOCK ON");
            fx.sound(aim[0], Sfx.BEACON_POWER, 3f, 0.5f);
            // Scheduled at the lock, so the blast lands where the grid stopped, not where it started.
            dome(t, stage, IMPACT, 16, aim[0].clone(), RADIUS, STRIKE, seal(stage, 1.6), v -> {
                v.effect(Affliction.BLINDNESS, 40, 0);
                v.push(new Vector(0, 1.0, 0));
            });
        });
        t.span(LOCK, IMPACT, (tick, p) -> convergingBeams(stage, aim[0], 10, 60, RADIUS * 1.5, p, STRIKE));
        tweenTo(t, stage, LOCK, IMPACT, Poses.CAST_FORWARD.withHead(-10, 0, 0), Ease.IN_OUT);
        t.at(IMPACT, () -> fx.flash(stage.onGround(aim[0]).add(new Vector(0, 6, 0)), Palette.HOLY));
        recover(t, stage, IMPACT + 4, IMPACT + 24, Poses.GUARD);
        t.hold(IMPACT + 46);
        return t;
    }

    @Override
    public String getName() {
        return "orbitalstrike";
    }
}
