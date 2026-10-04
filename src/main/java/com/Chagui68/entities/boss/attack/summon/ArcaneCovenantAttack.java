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
 * Arcane Covenant: the Sentinel's casters answer together — a Chaos Mage, a Venom Witch and an Ender
 * Knight, appearing behind and beside it so they fight from its cover.
 */
public class ArcaneCovenantAttack extends SummoningAttack {

    public ArcaneCovenantAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        Vector feet = stage.feet();
        Vector back = stage.forward().multiply(-5);
        Vector side = stage.body().right().multiply(6);
        return List.of(feet.clone().add(back), feet.clone().add(side), feet.clone().subtract(side));
    }

    @Override
    protected Color color() {
        return Palette.AMETHYST;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        return switch (index) {
            case 0 -> plugin.getChaosMage() == null ? List.of() : spawnExisting(world, at, plugin.getChaosMage()::trySpawn);
            case 1 -> plugin.getVenomWitch() == null ? List.of() : spawnExisting(world, at, plugin.getVenomWitch()::trySpawn);
            default -> plugin.getEnderKnight() == null ? List.of() : spawnExisting(world, at, plugin.getEnderKnight()::trySpawn);
        };
    }

    @Override
    public String getName() {
        return "arcanecovenant";
    }
}
