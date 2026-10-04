package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.BossInstance;
import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.utils.MscText;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Witch;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

/**
 * Obsidian Mender: a glowing acolyte appears behind the Sentinel and pours a beam of life into it,
 * healing it every second for as long as the mender lives. Kill it first.
 */
public class ObsidianMenderAttack extends SummoningAttack {

    /** Share of the boss's max health restored every second while the mender lives. */
    private static final double HEAL_PER_SECOND = 0.004;
    private static final double REACH = 30;
    private static final Color LIFE = Color.fromRGB(0x5CFF7A);

    public ObsidianMenderAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return List.of(stage.feet().subtract(stage.forward().multiply(8)));
    }

    @Override
    protected Color color() {
        return LIFE;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        Witch mender = world.spawn(at.toLocation(world), Witch.class, w -> {
            prepare(w, MscText.title(GREEN, "Obsidian Mender"), 40.0);
            w.setGlowing(true);
            w.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false));
        });
        return List.of(mender);
    }

    @Override
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
        Fx fx = stage.fx();
        Vector from = minion.getEyeLocation().toVector();
        Vector chest = stage.body().chest();
        if (from.distanceSquared(chest) > REACH * REACH) return;
        if (tick % 3 == 0) fx.line(from, chest, 0.7, fx.dust(LIFE, 1.1f).sometimes(0.7));
        if (tick % 20 != 0) return;
        BossInstance instance = stage.instance();
        double healed = mend(instance, ofMaxHealth(instance, HEAL_PER_SECOND));
        if (healed <= 0) return;
        fx.trail(from, chest, LIFE, 12);
        fx.cloud(Particle.HEART, chest.clone().add(new Vector(0, 3, 0)), 1, 1.5, 0);
        if (tick % 60 == 0) fx.sound(from, Sfx.AMETHYST_CHIME, 1.2f, 1.4f);
        fx.cloud(Particle.HAPPY_VILLAGER, from, 4, 0.4, 0);
        fx.dust(Palette.HOLY, 1.4f).at(from);
    }

    @Override
    public String getName() {
        return "obsidianmender";
    }
}
