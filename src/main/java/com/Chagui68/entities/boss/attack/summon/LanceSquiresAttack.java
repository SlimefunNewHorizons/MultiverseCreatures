package com.Chagui68.entities.boss.attack.summon;

import com.Chagui68.entities.boss.BossHost;
import com.Chagui68.entities.boss.attack.SummoningAttack;
import com.Chagui68.entities.boss.fx.Fx;
import com.Chagui68.entities.boss.fx.Palette;
import com.Chagui68.entities.boss.fx.Sfx;
import com.Chagui68.entities.boss.fx.Stage;
import com.Chagui68.utils.MscText;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.util.List;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;

/**
 * Lance Squires: two of the Sentinel's squires in obsidian-dyed armour, carrying small lances. They
 * fight on foot and every few seconds lunge at their target from up to ten blocks away.
 */
public class LanceSquiresAttack extends SummoningAttack {

    private static final int LUNGE_EVERY = 60;
    private static final Color OBSIDIAN = Color.fromRGB(0x2A1440);

    public LanceSquiresAttack(BossHost boss) {
        super(boss);
    }

    @Override
    protected List<Vector> points(Stage stage) {
        Vector side = stage.body().right().multiply(5);
        Vector ahead = stage.forward().multiply(3);
        return List.of(stage.feet().add(side).add(ahead), stage.feet().subtract(side).add(ahead));
    }

    @Override
    protected Color color() {
        return Palette.VOID;
    }

    @Override
    protected List<LivingEntity> summon(World world, Stage stage, Vector at, int index) {
        Husk squire = world.spawn(at.toLocation(world), Husk.class, h -> {
            prepare(h, MscText.title(DARK_PURPLE, "Obsidian Squire"), 50.0);
            h.setAdult();
            setAttribute(h, Attribute.MOVEMENT_SPEED, 0.3);
            setAttribute(h, Attribute.ATTACK_DAMAGE, 9.0);
            EntityEquipment eq = h.getEquipment();
            if (eq != null) {
                eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                eq.setChestplate(dyed(Material.LEATHER_CHESTPLATE));
                eq.setLeggings(dyed(Material.LEATHER_LEGGINGS));
                eq.setBoots(dyed(Material.LEATHER_BOOTS));
                eq.setItemInMainHand(new ItemStack(Material.TRIDENT));
                eq.setHelmetDropChance(0);
                eq.setChestplateDropChance(0);
                eq.setLeggingsDropChance(0);
                eq.setBootsDropChance(0);
                eq.setItemInMainHandDropChance(0);
            }
        });
        return List.of(squire);
    }

    @Override
    protected void behave(Stage stage, LivingEntity minion, int index, int tick) {
        Fx fx = stage.fx();
        if (tick % 4 == 0) fx.dust(Palette.VOID, 1.0f, 0.3, 1).at(minion.getLocation().toVector().add(new Vector(0, 1, 0)));
        if ((tick + index * LUNGE_EVERY / 2) % LUNGE_EVERY != 0 || !(minion instanceof Mob mob) || mob.getTarget() == null) return;
        Vector from = minion.getLocation().toVector();
        Vector to = mob.getTarget().getLocation().toVector().subtract(from);
        double distance = to.length();
        if (distance < 3 || distance > 10) return;
        minion.setVelocity(to.setY(0).normalize().multiply(1.1).setY(0.35));
        fx.line(from.clone().add(new Vector(0, 1, 0)), from.clone().add(to.clone().normalize().multiply(3)).add(new Vector(0, 1, 0)),
                0.3, fx.dust(Palette.AMETHYST, 1.2f));
        fx.cloud(Particle.SWEEP_ATTACK, from.clone().add(new Vector(0, 1, 0)), 1, 0.2, 0);
        fx.sound(from, Sfx.TRIDENT_RIPTIDE, 1.2f, 1.4f);
    }

    private static ItemStack dyed(Material leather) {
        ItemStack item = new ItemStack(leather);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(OBSIDIAN);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getName() {
        return "lancesquires";
    }
}
