package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Victim;
import com.Chagui68.utils.MscText;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.GOLD;

/**
 * Ember Hounds: a pack of three wolves wreathed in fire burst out of a single rune. They are fast,
 * they hunt as a pack, and anyone they get close to catches fire.
 */
public class EmberHoundsAttack extends SummoningAttack {

    private static final int PACK = 3;

    public EmberHoundsAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return List.of(stage.feet().add(stage.forward().multiply(6)));
    }

    @Override
    protected Color color() {
        return Palette.MOLTEN;
    }

    @Override
    protected int lifetime() {
        return 600;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        List<LivingEntity> pack = new java.util.ArrayList<>();
        for (int i = 0; i < PACK; i++) {
            Vector spot = at.clone().add(new Vector(i - 1, 0, (i % 2) * 0.8));
            pack.add(world.spawn(spot.toLocation(world), Wolf.class, w -> {
                prepare(w, MscText.title(GOLD, "Ember Hound"), 24.0);
                w.setAngry(true);
                setAttribute(w, Attribute.MOVEMENT_SPEED, 0.42);
                setAttribute(w, Attribute.ATTACK_DAMAGE, 5.0);
                w.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false));
                w.setVisualFire(true);
            }));
        }
        return pack;
    }

    @Override
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
        Fx fx = stage.fx();
        Vector at = minion.getLocation().toVector();
        if (tick % 2 == 0) {
            fx.cloud(Particle.FLAME, at.clone().add(new Vector(0, 0.5, 0)), 2, 0.3, 0.01);
            fx.dust(Palette.EMBER, 1.0f).at(at.clone().add(new Vector(0, 0.2, 0)));
        }
        if (minion instanceof Wolf wolf && !wolf.isAngry()) wolf.setAngry(true);
        if (tick % 10 != 0) return;
        for (Victim victim : stage.victimsIn(Area.cylinder(at, 1.8, 1, 2.5))) {
            victim.ignite(60);
        }
    }

    @Override
    public String getName() {
        return "emberhounds";
    }
}
