package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Area;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Shapes;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.entities.boss.fx.Telegraph;
import com.Chagui68.utils.MscText;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

/**
 * Obsidian Brute: a hulking, twice-sized warrior of black stone with an axe. Slow, very tough and
 * immovable; every four seconds it raises its axe and slams the ground around it, which the floor
 * warns about first.
 */
public class ObsidianBruteAttack extends SummoningAttack {

    private static final int SLAM_EVERY = 80;
    private static final int WIND = 16;
    private static final double RADIUS = 4.5;
    private static final Color STONE = Color.fromRGB(0x1A1424);

    public ObsidianBruteAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        return List.of(stage.feet().add(stage.forward().multiply(7)));
    }

    @Override
    protected Color color() {
        return Palette.STONE;
    }

    @Override
    protected int lifetime() {
        return 1200;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        Zombie brute = world.spawn(at.toLocation(world), Zombie.class, z -> {
            prepare(z, MscText.title(DARK_GRAY, "Obsidian Brute"), 160.0);
            z.setAdult();
            setAttribute(z, Attribute.SCALE, 2.2);
            setAttribute(z, Attribute.MOVEMENT_SPEED, 0.2);
            setAttribute(z, Attribute.ATTACK_DAMAGE, 12.0);
            setAttribute(z, Attribute.KNOCKBACK_RESISTANCE, 1.0);
            EntityEquipment eq = z.getEquipment();
            if (eq != null) {
                eq.setHelmet(new ItemStack(Material.CRYING_OBSIDIAN));
                eq.setChestplate(dyed(Material.LEATHER_CHESTPLATE));
                eq.setLeggings(dyed(Material.LEATHER_LEGGINGS));
                eq.setBoots(dyed(Material.LEATHER_BOOTS));
                eq.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                eq.setHelmetDropChance(0);
                eq.setChestplateDropChance(0);
                eq.setLeggingsDropChance(0);
                eq.setBootsDropChance(0);
                eq.setItemInMainHandDropChance(0);
            }
        });
        return List.of(brute);
    }

    @Override
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
        Fx fx = stage.fx();
        Vector feet = minion.getLocation().toVector();
        int phase = tick % SLAM_EVERY;
        int windStart = SLAM_EVERY - WIND;
        if (phase >= windStart) {
            double p = (phase - windStart) / (double) WIND;
            if (phase % 2 == 0) Telegraph.circle(stage, feet, RADIUS, p);
            return;
        }
        if (phase != 0 || tick == 0) return;
        fx.flatBurst(feet, Particle.CLOUD, 30, 0.4);
        fx.crumble(Material.OBSIDIAN, 30, RADIUS * 0.4).at(feet.clone().add(new Vector(0, 0.3, 0)));
        fx.ring(feet.clone().add(new Vector(0, 0.3, 0)), RADIUS, 0.5, 0, fx.dust(Palette.ASH, 1.8f));
        fx.sound(feet, Sfx.MACE_SMASH_GROUND, 2f, 0.5f);
        stage.hit(Area.cylinder(feet, RADIUS, 1.5, 3), seal(stage, 0.6),
                v -> v.push(Shapes.flat(v.position().subtract(feet)).multiply(0.8).setY(0.6)));
    }

    private static ItemStack dyed(Material leather) {
        ItemStack item = new ItemStack(leather);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(STONE);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getName() {
        return "obsidianbrute";
    }
}
