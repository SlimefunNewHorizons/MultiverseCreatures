package com.Chagui68.items.components;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Fourth link: a feather of Beatrice, the bluebird who led Wirt and Greg through the woods. */
public class BeatriceFeather {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_beatrice_feather");
    public static final ItemStack BEATRICE_FEATHER = ItemBuilder.of(Material.FEATHER)
            .name(MscText.title(BLUE, "Beatrice's Feather"))
            .lore(
                    MscText.line(GRAY, "A bluebird's feather, shed by a girl"),
                    MscText.line(GRAY, "cursed for throwing a rock."),
                    MscText.line(GRAY, "It still knows the way through."),
                    MscText.blank(),
                    MscText.line(WHITE, "Crafting Ingredient"),
                    MscText.blank(),
                    MscText.quote(DARK_PURPLE, "\"Bluebirds are bad luck,"),
                    MscText.quote(DARK_PURPLE, "everyone knows that.\""),
                    MscText.blank(),
                    MscText.footer("The Unknown")
            )
            .tagged(KEY)
            .build();
}
