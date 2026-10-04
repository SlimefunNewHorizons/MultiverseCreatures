package com.Chagui68.entities.boss;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * The Sentinel's change of phase, played as a scene instead of a single frame: it buckles to one
 * knee, power spirals up out of the ground into it, it rises roaring with its arms thrown open
 * inside a pillar of light, and at the climax the phase's own blast goes off and every player in
 * range is told what the Sentinel has just gained. It cannot be hurt until the climax.
 */
final class SentinelPhaseShift extends ChoreographedAttack {

    private static final int BUCKLE = 20;
    private static final int RISE = 50;
    private static final int CLIMAX = 60;
    private static final int END = 96;

    private final int phase;
    private final Runnable blast;

    SentinelPhaseShift(BossHost boss, int phase, Runnable blast) {
        super(boss);
        this.phase = phase;
        this.blast = blast;
    }

    /** The colour each phase burns with: rage red, barrier blue, storm white, despair violet. */
    static Color colorOf(int phase) {
        return switch (phase) {
            case 1 -> Palette.EMBER;
            case 2 -> Palette.FROST;
            case 3 -> Palette.STORM;
            default -> Palette.VOID;
        };
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        BossInstance instance = stage.instance();
        Color color = colorOf(phase);
        Vector feet = stage.onGround(stage.feet());
        Timeline t = new Timeline();

        t.at(0, () -> {
            if (instance != null) {
                instance.invulnerable = true;
                instance.invulnerableTimer = Math.max(instance.invulnerableTimer, CLIMAX);
            }
            fx.sound(feet, Sfx.WARDEN_HEARTBEAT, 3f, 0.5f);
            fx.sound(feet, Sfx.BEACON_DEACTIVATE, 2.5f, 0.5f);
        });
        tweenTo(t, stage, 0, BUCKLE, Poses.KNEEL.withHead(25, 0, 0), Ease.OUT);
        // Power drawn up out of the floor: spirals climbing the body and streaks from all around.
        t.span(0, CLIMAX, (tick, p) -> {
            Vector chest = stage.body().chest();
            double height = chest.getY() - feet.getY() + 2;
            fx.draw(Shapes.helix(feet, 4.5 * (1 - p) + 1.5, height, 2.5, 30, tick * 0.25), fx.dust(color, 1.6f));
            fx.draw(Shapes.helix(feet, 4.5 * (1 - p) + 1.5, height, 2.5, 30, tick * 0.25 + Math.PI), fx.dust(Palette.GOLD, 1.2f).sometimes(0.6));
            if (tick % 3 == 0) fx.gather(chest, 12, 6, color, 14);
            if (tick % 2 == 0) {
                Vector floor = feet.clone().add(new Vector(0, 0.15, 0));
                double r = 3 + 9 * Ease.at(Ease.OUT, p);
                fx.draw(Shapes.star(floor, r, 7, 3, tick * 0.03, 0.6, Shapes.FLAT_U, Shapes.FLAT_V), fx.dust(color, 1.4f));
                fx.ring(floor, r * 1.12, 0.7, -tick * 0.04, fx.dust(Palette.GOLD, 1.2f));
            }
            if (tick % 20 == 0) fx.sound(feet, Sfx.WARDEN_HEARTBEAT, 3f, 0.5f + (float) p * 0.6f);
        });
        tweenTo(t, stage, BUCKLE, RISE, Poses.ROAR.withHead(-30, 0, 0), Ease.IN_OUT);
        tweenTo(t, stage, RISE, CLIMAX, Poses.SPREAD.withHead(-35, 0, 0), Ease.OUT_BACK);
        // A pillar of light thickening around it as it stands.
        t.span(BUCKLE, CLIMAX, (tick, p) -> {
            double r = 0.5 + 2.5 * p;
            for (int i = 0; i < 6; i++) {
                Vector base = feet.clone().add(Shapes.heading(i * Math.PI / 3 + tick * 0.1).multiply(r));
                fx.line(base, base.clone().add(new Vector(0, 30, 0)), 1.6, fx.dust(Palette.mix(color, Palette.HOLY, p), 1.4f).sometimes(0.6));
            }
            if (tick % 8 == 0) stage.lightning(feet.clone().add(Shapes.heading(stage.random().nextDouble() * 6.28).multiply(10 + stage.random().nextDouble() * 6)));
        });
        t.at(CLIMAX, () -> {
            Vector chest = stage.body().chest();
            fx.flash(chest, color);
            fx.burst(chest, Particle.END_ROD, 120, 1.2);
            fx.draw(Shapes.sphere(chest, 8, 220), fx.dust(color, 2.6f));
            fx.sound(feet, Sfx.WITHER_SPAWN, 2.5f, 0.6f);
            fx.sound(feet, Sfx.DRAGON_GROWL, 3f, 0.5f);
            fx.sound(feet, Sfx.EXPLODE, 2.5f, 0.6f);
            if (instance != null) {
                instance.invulnerableTimer = Math.min(instance.invulnerableTimer, 1);
            }
            blast.run();
            announce(stage, phase, feet);
        });
        // The afterglow: a ring rolling out across the arena and embers settling.
        t.span(CLIMAX, END, (tick, p) -> {
            double r = 2 + 28 * Ease.at(Ease.OUT, p);
            fx.ring(feet.clone().add(new Vector(0, 0.4, 0)), r, 0.9, tick, fx.dust(color, 2.2f).and(fx.particle(Particle.CLOUD).sometimes(0.15)));
            if (tick % 2 == 0) fx.cloud(Particle.FLAME, stage.body().chest(), 4, 2.5, 0.02);
        });
        recover(t, stage, CLIMAX + 16, END, Poses.GUARD);
        return t;
    }

    /** The phase's name and its new passive, on every screen within sixty blocks. */
    private static void announce(Stage stage, int phase, Vector at) {
        String[] passive = SentinelPassives.announcement(phase);
        if (passive == null) return;
        String numeral = switch (phase) {
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            default -> "V";
        };
        stage.onServer(world -> {
            Location center = at.toLocation(world);
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(center) > 60 * 60) continue;
                p.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "PHASE " + numeral + " — " + passive[0],
                        ChatColor.GRAY + passive[1], 5, 60, 20);
            }
        });
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return END;
    }

    @Override
    public String getName() {
        return "phaseshift";
    }
}
