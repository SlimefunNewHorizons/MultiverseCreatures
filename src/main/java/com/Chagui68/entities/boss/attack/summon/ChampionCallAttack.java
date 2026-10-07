package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
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
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Champion Call: once per phase the Sentinel tears open a gate between worlds and one of the
 * multiverse's bosses, picked at random, steps through to fight beside it — NIX, DIO, Garou,
 * Mahoraga or Kinger. The champion fights until it falls; if the Sentinel falls first, the gate
 * takes it back.
 *
 * <p>JackStar is left out on purpose: his own arrival calls in another boss, and a chain of
 * summons inside one fight is more than any arena can hold.
 */
public class ChampionCallAttack extends SummoningAttack {

    private static final int OPEN = 60;
    /** How long the call can keep a champion bound to the fight: thirty minutes. */
    private static final int BOUND = 36_000;
    private static final double GATE = 4.5;
    private static final Color RIFT = Color.fromRGB(0xC02BFF);

    /** One champion that can answer: its name and how to bring it in. */
    private record Champion(String name, Predicate<Location> spawn) {
    }

    public ChampionCallAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected boolean ready(BossInstance instance) {
        return super.ready(instance) && instance.championPhase != instance.currentPhase;
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return List.of(stage.feet().add(stage.forward().multiply(10)));
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        List<Champion> champions = champions(plugin);
        Collections.shuffle(champions, stage.random());
        for (Champion champion : champions) {
            List<LivingEntity> made = spawnExisting(world, at, champion.spawn());
            if (made.isEmpty()) continue;
            announce(stage, champion.name());
            return made;
        }
        return List.of();
    }

    @Override
    public Timeline choreograph(Stage stage) {
        BossInstance instance = stage.instance();
        if (instance != null) instance.championPhase = instance.currentPhase;
        Fx fx = stage.fx();
        Vector gate = stage.onGround(points(stage).get(0)).add(new Vector(0, GATE + 0.5, 0));
        Vector right = stage.body().right();
        List<LivingEntity> champions = new ArrayList<>();
        Timeline t = new Timeline();

        tweenTo(t, stage, 0, 24, Poses.SPEAR_RAISED.withLeftArm(-150, 0, -60).withHead(-20, 0, 0), Ease.OUT);
        t.at(0, () -> {
            fx.sound(stage.feet(), Sfx.END_PORTAL_SPAWN, 2f, 0.6f);
            fx.sound(stage.feet(), Sfx.EVOKER_PREPARE_SUMMON, 3f, 0.4f);
        });
        t.span(0, OPEN, (tick, p) -> {
            double open = Ease.at(Ease.OUT_BACK, Math.min(1, tick / (double) (OPEN - 10)));
            // A standing gate: two counter-turning rings and a star, its face swirling with void.
            fx.draw(Shapes.circle(gate, GATE * open, 48, right, Shapes.UP, tick * 0.08), fx.dust(RIFT, 1.8f));
            fx.draw(Shapes.circle(gate, GATE * 0.8 * open, 40, right, Shapes.UP, -tick * 0.12), fx.dust(Palette.GOLD, 1.3f));
            fx.draw(Shapes.star(gate, GATE * 0.7 * open, 7, 3, tick * 0.05, 0.5, right, Shapes.UP), fx.dust(Palette.VOID, 1.2f));
            if (tick % 2 == 0) fx.cloud(Particle.PORTAL, gate, 20, GATE * 0.5 * open, 0.4);
            if (tick % 4 == 0) fx.line(stage.body().spearTip(), gate, 0.6, fx.dust(RIFT, 1.0f).sometimes(0.6));
            if (tick % 15 == 0) fx.sound(gate, Sfx.WARDEN_HEARTBEAT, 2f, 0.6f + (float) p);
        });
        t.at(OPEN, () -> {
            fx.flash(gate, RIFT);
            fx.impact(gate.clone().subtract(new Vector(0, GATE, 0)), RIFT, 2.5);
            stage.lightning(gate.clone().subtract(new Vector(0, GATE, 0)));
            fx.sound(gate, Sfx.WITHER_SPAWN, 2f, 0.7f);
            stage.onServer(world -> {
                for (LivingEntity champion : summon(world, stage, stage.onGround(points(stage).get(0)), 0)) {
                    bind(instance, champion);
                    champions.add(champion);
                }
            });
        });
        recover(t, stage, OPEN + 4, OPEN + 20, Poses.GUARD);
        t.span(OPEN + 1, OPEN + BOUND, (tick, p) -> {
            boolean anyAlive = false;
            for (LivingEntity champion : champions) {
                if (champion.isValid() && !champion.isDead()) anyAlive = true;
            }
            if (!anyAlive && tick > 5) t.stop();
        });
        // The gate only takes the champion back when the Sentinel is gone; a champion that is still
        // fighting a living Sentinel keeps fighting.
        t.onFinish(() -> {
            if (stage.alive() && t.now() < OPEN + BOUND) return;
            for (LivingEntity champion : champions) {
                if (!champion.isValid() || champion.isDead()) continue;
                fx.cloud(Particle.PORTAL, champion.getLocation().toVector().add(new Vector(0, 1, 0)), 60, 1, 0.5);
                champion.remove();
            }
        });
        return t;
    }

    @Override
    public int lockTicks(Timeline timeline) {
        return OPEN + 20;
    }

    /** Tells everyone near the gate which champion answered. */
    private static void announce(Stage stage, String name) {
        stage.onServer(world -> {
            Location at = stage.feet().toLocation(world);
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distanceSquared(at) > 80 * 80) continue;
                p.sendTitle(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "A CHAMPION ANSWERS",
                        ChatColor.LIGHT_PURPLE + name + ChatColor.GRAY + " steps through the gate", 5, 50, 15);
            }
        });
    }

    /** Every boss of the plugin that can be called, if its module is loaded. */
    private static List<Champion> champions(MultiverseCreatures plugin) {
        List<Champion> out = new ArrayList<>();
        if (plugin.getNixBoss() != null) out.add(new Champion("NIX - The Executioner", plugin.getNixBoss()::trySpawn));
        if (plugin.getDioBoss() != null) out.add(new Champion("DIO", plugin.getDioBoss()::trySpawn));
        if (plugin.getGarouBoss() != null) out.add(new Champion("Garou", plugin.getGarouBoss()::trySpawn));
        if (plugin.getMahoraga() != null) out.add(new Champion("Mahoraga", plugin.getMahoraga()::trySpawn));
        if (plugin.getKinger() != null) out.add(new Champion("Kinger", plugin.getKinger()::trySpawn));
        return out;
    }

    @Override
    public String getName() {
        return "championcall";
    }
}
