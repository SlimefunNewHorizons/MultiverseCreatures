package com.Chagui68.entities.boss.attack.defensive;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.ChoreographedAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Soul Siphon: the Sentinel throws tethers of soul light at up to three nearby players and drains
 * them, healing several times what it takes. A tether snaps when its player runs far enough away.
 */
public class SoulSiphonAttack extends ChoreographedAttack {

    private static final int CAST = 14;
    private static final int DRAIN = 80;
    private static final int PULSE = 10;
    private static final double RANGE = 16;
    private static final double SNAP = 20;
    private static final int MAX_TETHERS = 3;
    /** Health restored per point of damage drained. */
    private static final double LIFESTEAL = 4.0;

    public SoulSiphonAttack(BossHost boss) {
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
        Vector origin = stage.feet();
        List<Victim> tethered = new ArrayList<>(stage.victimsIn(Area.cylinder(origin, RANGE, 4, 14)));
        if (tethered.isEmpty()) return null;
        tethered.sort(Comparator.comparingDouble(v -> v.position().distanceSquared(origin)));
        while (tethered.size() > MAX_TETHERS) tethered.remove(tethered.size() - 1);
        if (instance != null) instance.regenerating = true;
        double drain = seal(stage, 0.2);
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, CAST, Poses.CAST_FORWARD, Ease.OUT);
        t.at(0, () -> fx.sound(origin, Sfx.SOUL_ESCAPE, 2.5f, 0.6f));
        t.span(0, CAST, (tick, p) -> {
            Vector chest = stage.body().chest();
            for (Victim v : tethered) {
                Vector reach = chest.clone().add(v.chest().subtract(chest).multiply(p));
                fx.line(chest, reach, 0.6, fx.dust(Palette.SOUL, 1.2f));
            }
        });
        t.span(CAST, CAST + DRAIN, (tick, p) -> {
            Vector chest = stage.body().chest();
            tethered.removeIf(v -> {
                Vector at = v.position();
                boolean gone = Math.hypot(at.getX() - stage.feet().getX(), at.getZ() - stage.feet().getZ()) > SNAP;
                if (gone) fx.sound(at, Sfx.CHAIN_BREAK, 1.6f, 1.2f);
                return gone;
            });
            for (Victim v : tethered) {
                if (tick % 2 == 0) fx.line(v.chest(), chest, 0.8, fx.dust(Palette.SOUL, 1.1f).sometimes(0.7));
                if (tick % PULSE != 0) continue;
                stage.damage(v, drain);
                mend(instance, drain * LIFESTEAL);
                fx.trail(v.chest(), chest, Palette.SOUL, 10);
                fx.sound(v.position(), Sfx.SOUL_SAND_BREAK, 1.2f, 0.6f);
            }
        });
        recover(t, stage, CAST + DRAIN, CAST + DRAIN + 12, Poses.GUARD);
        t.onFinish(() -> {
            if (instance != null) instance.regenerating = false;
        });
        return t;
    }

    @Override
    public String getName() {
        return "soulsiphon";
    }
}
