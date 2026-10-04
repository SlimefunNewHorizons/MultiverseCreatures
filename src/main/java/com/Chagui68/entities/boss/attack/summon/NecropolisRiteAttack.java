package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Stage;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Necropolis Rite: a Soul Reaper rises in front of the Sentinel, flanked by two Bone Shields that
 * guard it.
 */
public class NecropolisRiteAttack extends SummoningAttack {

    public NecropolisRiteAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        Vector front = stage.feet().add(stage.forward().multiply(7));
        Vector side = stage.body().right().multiply(3.5);
        return List.of(front, front.clone().add(side), front.clone().subtract(side));
    }

    @Override
    protected Color color() {
        return Palette.SOUL;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        if (index == 0) {
            return plugin.getSoulReaper() == null ? List.of() : spawnExisting(world, at, plugin.getSoulReaper()::trySpawn);
        }
        return plugin.getBoneShield() == null ? List.of() : spawnExisting(world, at, plugin.getBoneShield()::trySpawn);
    }

    @Override
    public String getName() {
        return "necropolisrite";
    }
}
