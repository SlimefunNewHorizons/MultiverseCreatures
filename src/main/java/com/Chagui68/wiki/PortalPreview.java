package com.Chagui68.wiki;

import com.Chagui68.ritual.RitualStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /mscwiki portal}: shows the ritual portal as block displays one block in front of the player
 * for five seconds, so they can see how to build it. Nothing is placed in the world.
 */
final class PortalPreview {

    private static final long LIFETIME_TICKS = 100L;
    private static final int SIZE = 7;

    private PortalPreview() {
    }

    static void show(Plugin plugin, Player player) {
        BlockFace facing = player.getFacing();
        Location feet = player.getLocation().getBlock().getLocation();
        // One empty block between the player and the near edge, structure centred on the player's line.
        int reach = 2 + SIZE / 2;
        Location origin = feet.add(facing.getModX() * reach - SIZE / 2, 0, facing.getModZ() * reach - SIZE / 2);

        Map<Location, Material> layout = RitualStructure.getStructureBlocks();
        for (Location relative : layout.keySet()) {
            if (!origin.clone().add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ())
                    .getBlock().isReplaceable()) {
                player.sendMessage(Component.text("There is no space for the portal to appear.", NamedTextColor.RED));
                return;
            }
        }

        List<BlockDisplay> displays = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : layout.entrySet()) {
            if (entry.getValue().isAir()) {
                continue;
            }
            Location relative = entry.getKey();
            Location at = origin.clone().add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
            displays.add(at.getWorld().spawn(at, BlockDisplay.class, display -> {
                display.setBlock(entry.getValue().createBlockData());
                display.setPersistent(false);
            }));
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> displays.forEach(BlockDisplay::remove),
                LIFETIME_TICKS);
    }
}
