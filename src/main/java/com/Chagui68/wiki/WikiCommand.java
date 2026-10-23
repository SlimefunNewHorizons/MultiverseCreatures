package com.Chagui68.wiki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /mscwiki}: opens the in-game wiki, or shows how a summoning structure is built.
 *
 * <pre>
 * /mscwiki               the home page (the flag in its corner switches the language)
 * /mscwiki &lt;structure&gt;   shows that structure in front of you for five seconds and says how it is used:
 *                        portal, sentinel, nix, dio, jack, olympus, asgard, duat, void, witherstorm
 * </pre>
 */
public final class WikiCommand implements TabExecutor {

    private final Plugin plugin;

    public WikiCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("The wiki can only be opened by a player.", NamedTextColor.RED));
            return true;
        }
        WikiLang lang = WikiLang.of(player);
        if (args.length == 0) {
            player.openInventory(WikiMenu.home(lang).getInventory());
            return true;
        }
        StructurePreview.Structure structure = StructurePreview.Structure.byId(args[0]);
        if (structure == null) {
            player.sendMessage(Component.text(lang.pick("No structure called ", "No hay ninguna estructura llamada ")
                    + args[0] + ". " + lang.pick("Try: ", "Prueba: ") + String.join(", ", ids()) + ".",
                    NamedTextColor.RED));
            return true;
        }
        StructurePreview.show(plugin, player, structure, lang);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length != 1) {
            return out;
        }
        String typed = args[0].toLowerCase(Locale.ROOT);
        for (String id : ids()) {
            if (id.startsWith(typed)) out.add(id);
        }
        return out;
    }

    private static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (StructurePreview.Structure structure : StructurePreview.Structure.values()) {
            ids.add(structure.id());
        }
        return ids;
    }
}
