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
 * Elemental Conclave: three runes in a triangle around the Sentinel call fire, frost and storm at
 * once — a Flame Elemental, a Frost Golem and a Storm Caller.
 */
public class ElementalConclaveAttack extends SummoningAttack {

    public ElementalConclaveAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return around(stage, stage.feet(), 7, 3);
    }

    @Override
    protected Color color() {
        return Palette.STORM;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        return switch (index) {
            case 0 -> plugin.getFlameElemental() == null ? List.of()
                    : spawnExisting(world, at, plugin.getFlameElemental()::trySpawn);
            case 1 -> plugin.getFrostGolem() == null ? List.of()
                    : spawnExisting(world, at, plugin.getFrostGolem()::trySpawn);
            default -> plugin.getStormCaller() == null ? List.of()
                    : spawnExisting(world, at, plugin.getStormCaller()::trySpawn);
        };
    }

    @Override
    public String getName() {
        return "elementalconclave";
    }
}
