package com.Chagui68.items.components;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Second link: Edelwood Sap ground at the Woodsman's mill, the oil that keeps the lantern lit. */
public class EdelwoodOil {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_edelwood_oil");
    public static final ItemStack EDELWOOD_OIL = ItemBuilder.of(Material.DRAGON_BREATH)
            .name(MscText.title(GOLD, "Edelwood Oil"))
            .lore(
                    MscText.line(GRAY, "Ground at the Woodsman's mill and"),
                    MscText.line(GRAY, "pressed into oil. It burns slow"),
                    MscText.line(GRAY, "and never quite goes out."),
                    MscText.blank(),
                    MscText.line(WHITE, "Crafting Ingredient"),
                    MscText.blank(),
                    MscText.quote(DARK_PURPLE, "\"Keep the lantern lit,"),
                    MscText.quote(DARK_PURPLE, "whatever it costs.\""),
                    MscText.blank(),
                    MscText.footer("The Unknown")
            )
            .tagged(KEY)
            .build();
}
