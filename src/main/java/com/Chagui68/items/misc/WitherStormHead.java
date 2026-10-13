package com.Chagui68.items.misc;

import com.Chagui68.utils.ItemBuilder;
import com.Chagui68.utils.MscText;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * The trophy the Wither Storm leaves: one of its heads, the wither skull its model is built with,
 * named after the form it was killed in. It can be placed like any skull.
 */
public final class WitherStormHead {

    public static final NamespacedKey KEY = new NamespacedKey("multiversecreatures", "msc_wither_storm_head");

    private WitherStormHead() {
    }

    /** The head of a storm killed in the form called {@code form}. */
    public static ItemStack of(String form) {
        return ItemBuilder.of(Material.WITHER_SKELETON_SKULL)
                .name(MscText.title(DARK_PURPLE, "Wither Storm Head"))
                .lore(
                        MscText.line(GRAY, "Torn from " + form + " as it fell."),
                        MscText.blank(),
                        MscText.line(WHITE, "Boss Trophy"),
                        MscText.blank(),
                        MscText.quote(DARK_PURPLE, "\"It ate the world, and you stopped it.\""),
                        MscText.blank(),
                        MscText.footer("DrakesCraft")
                )
                .tagged(KEY)
                .build();
    }
}
