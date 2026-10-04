package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Affliction;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.utils.MscText;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;

/**
 * Void Wisps: three unstable flickers of the void that drift through walls towards the players and
 * burst when they get close, or when their time runs out. Shoot them down before they arrive.
 */
public class VoidWispsAttack extends SummoningAttack {

    private static final int WISPS = 3;
    private static final int FUSE = 220;
    private static final double TRIGGER = 2.5;
    private static final double BLAST = 3.5;

    public VoidWispsAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return around(stage, stage.feet(), 4, WISPS);
    }

    @Override
    protected Color color() {
        return Palette.VOID;
    }

    @Override
    protected int lifetime() {
        return FUSE + 10;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        Vex wisp = world.spawn(at.clone().add(new Vector(0, 2, 0)).toLocation(world), Vex.class, v -> {
            prepare(v, MscText.title(DARK_PURPLE, "Void Wisp"), 14.0);
            EntityEquipment eq = v.getEquipment();
            if (eq != null) eq.setItemInMainHand(new ItemStack(Material.AIR));
            v.setGlowing(true);
        });
        return List.of(wisp);
    }

    @Override
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
        Fx fx = stage.fx();
        Vector at = minion.getLocation().toVector().add(new Vector(0, 0.5, 0));
        double fuse = tick / (double) FUSE;
        fx.dust(Palette.mix(Palette.VOID, Palette.WARNING, fuse), 1.6f, 0.25, 2).at(at);
        if (tick % 5 == 0) fx.cloud(Particle.PORTAL, at, 4, 0.3, 0.2);
        if (tick % 20 == 0) fx.sound(at, Sfx.AMETHYST_CHIME, 0.7f, 0.6f + (float) fuse);
        boolean close = !stage.victimsIn(Area.sphere(at, TRIGGER)).isEmpty();
        if (!close && tick < FUSE) return;
        fx.impact(at, Palette.VOID, 1.6);
        fx.sound(at, Sfx.EXPLODE, 1.4f, 1.3f);
        stage.hit(Area.sphere(at, BLAST), seal(stage, 0.4), v -> {
            v.effect(Affliction.DARKNESS, 60, 0);
            v.push(Shapes.flat(v.position().subtract(at)).multiply(0.6).setY(0.4));
        });
        minion.remove();
    }

    @Override
    public String getName() {
        return "voidwisps";
    }
}
