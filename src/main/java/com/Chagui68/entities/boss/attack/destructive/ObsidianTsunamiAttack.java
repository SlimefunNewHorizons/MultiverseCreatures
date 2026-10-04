package com.Chagui68.entities.boss.attack.destructive;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.DestructiveAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Ease;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Poses;
import com.Chagui68.entities.boss.fx.Prop;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Timeline;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Obsidian Tsunami: the Sentinel drives its hands into the ground and a wall of obsidian thirty blocks
 * wide rises in front of it, then rolls across the arena. One stretch of the wall is missing — the
 * green lane on the floor — and it is the only way through.
 */
public class ObsidianTsunamiAttack extends DestructiveAttack {

    private static final int RISE = 60;
    private static final int TRAVEL = 44;
    private static final int SEGMENTS = 15;
    private static final double SEGMENT = 2.0;
    private static final double DISTANCE = 36;
    private static final double HEIGHT = 4.5;

    public ObsidianTsunamiAttack(BossHost boss) {
        super(boss);
    }

    @Override
    public Timeline choreograph(Stage stage) {
        Fx fx = stage.fx();
        Vector forward = Shapes.flat(stage.forward());
        Vector side = new Vector(-forward.getZ(), 0, forward.getX());
        Vector start = stage.onGround(stage.feet().add(forward.clone().multiply(3)));
        int gap = 2 + stage.random().nextInt(SEGMENTS - 5);
        double half = SEGMENTS * SEGMENT / 2;
        Timeline t = new Timeline();
        List<Prop> wall = props(t);
        Prop[] blocks = new Prop[SEGMENTS];
        Set<UUID> struck = new HashSet<>();
        Quaternionf facing = new Quaternionf().rotationY((float) -Math.atan2(forward.getZ(), forward.getX()));

        tweenTo(t, stage, 0, 20, Poses.CAST_GROUND, Ease.IN_OUT);
        t.at(0, () -> {
            fx.sound(start, Sfx.WITHER_BREAK_BLOCK, 3f, 0.4f);
            hud(stage, start, 60, "🌊 OBSIDIAN TSUNAMI", "Find the gap in the wall");
            for (int i = 0; i < SEGMENTS; i++) {
                if (i == gap || i == gap + 1) continue;
                Vector at = start.clone().add(side.clone().multiply(-half + SEGMENT * (i + 0.5)));
                Prop block = stage.block(i % 3 == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN, at, (float) SEGMENT, facing);
                block.resize((float) SEGMENT, 0.05f, facing, 0);
                blocks[i] = block;
                wall.add(block);
            }
        });
        t.at(1, () -> {
            for (Prop block : blocks) if (block != null) block.resize((float) SEGMENT, (float) HEIGHT, facing, RISE - 10);
        });
        t.span(0, RISE, (tick, p) -> {
            if (tick % 2 != 0) return;
            for (double d = 0; d <= DISTANCE; d += 1.5) {
                Vector row = start.clone().add(forward.clone().multiply(d));
                Vector a = row.clone().add(side.clone().multiply(-half + SEGMENT * gap));
                Vector b = row.clone().add(side.clone().multiply(-half + SEGMENT * (gap + 2)));
                fx.dust(Palette.PLAGUE, 1.3f).at(stage.onGround(a).add(new Vector(0, 0.2, 0)));
                fx.dust(Palette.PLAGUE, 1.3f).at(stage.onGround(b).add(new Vector(0, 0.2, 0)));
            }
            fx.ring(start.clone().add(new Vector(0, 0.2, 0)), half, 1.2, tick, fx.dust(Palette.mix(Palette.WARNING, Palette.WARNING_HOT, p), 1.2f).sometimes(0.5));
            fx.crumble(Material.OBSIDIAN, 6, 1.5).at(start.clone().add(side.clone().multiply((stage.random().nextDouble() * 2 - 1) * half)));
        });
        t.span(RISE, RISE + TRAVEL, (tick, p) -> {
            double before = DISTANCE * Ease.at(Ease.IN, tick / (double) TRAVEL);
            double after = DISTANCE * Ease.at(Ease.IN, (tick + 1) / (double) TRAVEL);
            Vector front = start.clone().add(forward.clone().multiply(after));
            for (int i = 0; i < SEGMENTS; i++) {
                if (blocks[i] == null) continue;
                Vector at = stage.onGround(front.clone().add(side.clone().multiply(-half + SEGMENT * (i + 0.5))));
                blocks[i].moveTo(at, 1);
                if (tick % 3 == 0) fx.crumble(Material.OBSIDIAN, 4, 0.6).at(at.clone().add(new Vector(0, 0.5, 0)));
                // The stretch of floor this segment swept over this tick, so a fast wave skips no one.
                Vector mid = stage.onGround(start.clone().add(forward.clone().multiply((before + after) / 2))
                        .add(side.clone().multiply(-half + SEGMENT * (i + 0.5))));
                Vector across = side.clone().multiply(SEGMENT / 2);
                Area swept = Area.segment(mid.clone().subtract(across), mid.clone().add(across), Math.max(1.2, (after - before) / 2 + 0.8));
                for (Victim v : stage.victimsIn(swept)) {
                    if (!struck.add(v.id())) continue;
                    stage.damage(v, seal(stage, 1.5));
                    v.push(forward.clone().multiply(1.6).setY(0.6));
                }
            }
            if (tick % 4 == 0) fx.sound(front, Sfx.WITHER_BREAK_BLOCK, 2f, 0.6f);
            fx.cloud(Particle.CLOUD, front.clone().add(new Vector(0, HEIGHT, 0)), 6, half * 0.6, 0.02);
        });
        t.at(RISE + TRAVEL, () -> {
            for (Prop block : blocks) {
                if (block == null) continue;
                fx.crumble(Material.OBSIDIAN, 20, 1).at(block.position().add(new Vector(0, 1, 0)));
                stage.debris(block.position().add(new Vector(0, 2, 0)), forward.clone().multiply(0.3).setY(0.4), Material.OBSIDIAN, 24);
                block.remove();
            }
            fx.sound(start.clone().add(forward.clone().multiply(DISTANCE)), Sfx.EXPLODE, 2.5f, 0.6f);
        });
        recover(t, stage, 30, 50, Poses.GUARD);
        t.hold(RISE + TRAVEL + 4);
        return t;
    }

    @Override
    public String getName() {
        return "obsidiantsunami";
    }
}
