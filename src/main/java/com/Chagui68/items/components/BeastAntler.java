package com.Chagui68.items.components;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Last link: an antler of the Beast, whose soul burns in the lantern. The merchant takes it for Wirt's Lantern. */
public class BeastAntler {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_beast_antler");
    public static final ItemStack BEAST_ANTLER = ItemBuilder.of(Material.DEAD_BUSH)
            .name(MscText.title(DARK_GRAY, "The Beast's Antler"))
            .lore(
                    MscText.line(GRAY, "Broken from the Beast of the woods,"),
                    MscText.line(GRAY, "whose soul is the flame of the lantern."),
                    MscText.line(GRAY, "The Multiverse Merchant wants it."),
                    MscText.blank(),
                    MscText.line(WHITE, "Trade Ingredient"),
                    MscText.blank(),
                    MscText.quote(DARK_PURPLE, "\"Give in, child."),
                    MscText.quote(DARK_PURPLE, "Let the light go out.\""),
                    MscText.blank(),
                    MscText.footer("The Unknown")
            )
            .tagged(KEY)
            .build();
}
