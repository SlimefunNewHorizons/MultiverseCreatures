package com.Chagui68.items.armor;

import com.Chagui68.utils.MscText;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Worldbinder: the Obsidian Bastion reforged around a Multiversal Core, the apex armor set. */
public class Worldbinder {

    public static final NamespacedKey HELMET_KEY = new NamespacedKey("multiversecreatures", "msc_worldbinder_helmet");
    public static final NamespacedKey CHEST_KEY = new NamespacedKey("multiversecreatures", "msc_worldbinder_chest");
    public static final NamespacedKey LEGS_KEY = new NamespacedKey("multiversecreatures", "msc_worldbinder_legs");
    public static final NamespacedKey BOOTS_KEY = new NamespacedKey("multiversecreatures", "msc_worldbinder_boots");

    public static final ItemStack HELMET = buildPiece(Material.NETHERITE_HELMET, HELMET_KEY, "Crown");
    public static final ItemStack CHESTPLATE = buildPiece(Material.NETHERITE_CHESTPLATE, CHEST_KEY, "Cuirass");
    public static final ItemStack LEGGINGS = buildPiece(Material.NETHERITE_LEGGINGS, LEGS_KEY, "Greaves");
    public static final ItemStack BOOTS = buildPiece(Material.NETHERITE_BOOTS, BOOTS_KEY, "Sabatons");

    public static final double MAX_HEALTH_BONUS = 0.5;
    public static final double SPEED_BONUS = 0.25;
    public static final double DAMAGE_BONUS = 0.35;
    public static final double DAMAGE_RESISTANCE = 0.3;
    public static final double BOSS_TRUE_DAMAGE_REDUCTION = 0.25;
    public static final double POTION_BONUS = 0.2;

    public static final double REFLECT_CHANCE = 0.15;
    public static final double REFLECT_MULTIPLIER = 1.2;
    public static final double DEATH_EVADE_CHANCE = 0.05;
    public static final long DEATH_EVADE_INVULNERABLE_TICKS = 60L;
    public static final long DEATH_EVADE_COOLDOWN_MS = 60_000L;
    public static final double VANISH_CHANCE = 0.10;
    public static final long VANISH_TICKS = 100L;
    public static final long VANISH_COOLDOWN_MS = 30_000L;

    private static ItemStack buildPiece(Material mat, NamespacedKey key, String pieceName) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MscText.title(LIGHT_PURPLE, "Worldbinder " + pieceName));

            List<Component> lore = new ArrayList<>();
            lore.add(MscText.rich(DARK_PURPLE, "✦ ━━━━━━━ ", LIGHT_PURPLE, "◆", DARK_PURPLE, " ━━━━━━━ ✦"));
            lore.add(MscText.line(GRAY, "Obsidian Bastion plate, unmade and"));
            lore.add(MscText.line(GRAY, "reforged around a Multiversal Core."));
            lore.add(MscText.line(GRAY, "Bone of the Aegis lines its heart,"));
            lore.add(MscText.line(GRAY, "the Veilwalker's shadow its crown."));
            lore.add(MscText.blank());
            lore.add(MscText.rich(DARK_GRAY, "  ⟡ ", LIGHT_PURPLE, "A thousand skies were torn"));
            lore.add(MscText.rich(DARK_GRAY, "  ⟡ ", LIGHT_PURPLE, "to temper a single plate."));
            lore.add(MscText.rich(DARK_PURPLE, "✦ ━━━━━━━ ", LIGHT_PURPLE, "◆", DARK_PURPLE, " ━━━━━━━ ✦"));
            lore.add(MscText.blank());
            lore.add(MscText.quote(DARK_PURPLE, "\"Every world bends. Not one"));
            lore.add(MscText.quote(DARK_PURPLE, "of them breaks its wearer.\""));
            lore.add(MscText.blank());
            lore.add(MscText.footer("Multiverse"));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.PROTECTION, 4, true);
            meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
            meta.addEnchant(Enchantment.THORNS, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
