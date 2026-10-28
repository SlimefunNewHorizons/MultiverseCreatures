package com.Chagui68.items.components;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Third link: a carved gourd from Pottsfield's harvest, where the dead wear pumpkins. */
public class PottsfieldGourd {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_pottsfield_gourd");
    public static final ItemStack POTTSFIELD_GOURD = ItemBuilder.of(Material.CARVED_PUMPKIN)
            .name(MscText.title(GOLD, "Pottsfield Gourd"))
            .lore(
                    MscText.line(GRAY, "Carved for the harvest festival,"),
                    MscText.line(GRAY, "where the skeletons of Pottsfield"),
                    MscText.line(GRAY, "dance under their pumpkins."),
                    MscText.blank(),
                    MscText.line(WHITE, "Crafting Ingredient"),
                    MscText.blank(),
                    MscText.quote(DARK_PURPLE, "\"You're a bit early,"),
                    MscText.quote(DARK_PURPLE, "but you'll be back.\""),
                    MscText.blank(),
                    MscText.footer("The Unknown")
            )
            .tagged(KEY)
            .build();
}
