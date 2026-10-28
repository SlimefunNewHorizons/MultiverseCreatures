package com.Chagui68.items.components;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** First link of Wirt's Lantern chain: sap bled from an Edelwood tree, a soul that gave up in the Unknown. */
public class EdelwoodSap {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_edelwood_sap");
    public static final ItemStack EDELWOOD_SAP = ItemBuilder.of(Material.RESIN_CLUMP)
            .name(MscText.title(GOLD, "Edelwood Sap"))
            .lore(
                    MscText.line(GRAY, "Amber sap bled from an Edelwood,"),
                    MscText.line(GRAY, "a lost soul that gave up and"),
                    MscText.line(GRAY, "took root in the Unknown."),
                    MscText.blank(),
                    MscText.line(WHITE, "Crafting Ingredient"),
                    MscText.blank(),
                    MscText.quote(DARK_PURPLE, "\"Not every tree in these woods"),
                    MscText.quote(DARK_PURPLE, "was always a tree.\""),
                    MscText.blank(),
                    MscText.footer("The Unknown")
            )
            .tagged(KEY)
            .build();
}
