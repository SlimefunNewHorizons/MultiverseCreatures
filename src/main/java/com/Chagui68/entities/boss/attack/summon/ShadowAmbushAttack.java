package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Victim;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Shadow Ambush: the runes open around the target instead of the boss — two Shadow Rogues and two
 * Void Crawlers climb out of the dark at the four corners of whoever the Sentinel is fighting.
 */
public class ShadowAmbushAttack extends SummoningAttack {

    public ShadowAmbushAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        Victim target = stage.target();
        Vector center = target != null ? target.position() : stage.feet().add(stage.forward().multiply(8));
        return around(stage, center, 5, 4);
    }

    @Override
    protected Color color() {
        return Palette.VOID_DEEP;
    }

    @Override
    protected int lifetime() {
        return 700;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        if (index % 2 == 0) {
            return plugin.getShadowRogue() == null ? List.of() : spawnExisting(world, at, plugin.getShadowRogue()::trySpawn);
        }
        return plugin.getVoidCrawler() == null ? List.of() : spawnExisting(world, at, plugin.getVoidCrawler()::trySpawn);
    }

    @Override
    public String getName() {
        return "shadowambush";
    }
}
