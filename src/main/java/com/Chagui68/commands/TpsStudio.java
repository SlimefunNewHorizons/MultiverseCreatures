package com.Chagui68.commands;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.monitor.MonitorServer;
import com.Chagui68.monitor.ServerMonitor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Locale;

import static org.bukkit.ChatColor.*;

/**
 * {@code /msc tps} — a quick TPS read-out in chat and, for a player, a private link to the monitor
 * page. The page is served by the plugin itself ({@link MonitorServer}) and only to the link this
 * command issues, so it exists for whoever ran the command and for nobody else.
 */
final class TpsStudio {

    private final MultiverseCreatures plugin;

    TpsStudio(MultiverseCreatures plugin) {
        this.plugin = plugin;
    }

    void handle(CommandSender sender) {
        ServerMonitor monitor = plugin.getServerMonitor();
        if (monitor == null) {
            sender.sendMessage(RED + "The monitor is off. Set monitor.enabled: true in config.yml and restart.");
            return;
        }

        ServerMonitor.Sample now = monitor.latest();
        if (now == null) {
            sender.sendMessage(YELLOW + "The monitor has no samples yet. Try again in a few seconds.");
        } else {
            sender.sendMessage(GOLD + "TPS " + color(now.tps(), 19, 15, true) + fmt(now.tps())
                    + GRAY + "  MSPT " + color(now.msptAvg(), 35, 50, false) + fmt(now.msptAvg())
                    + GRAY + "  Heap " + fmt(now.heapMb() / Math.max(1, now.heapMaxMb()) * 100) + "%"
                    + "  Entities " + now.entities() + "  Freezes " + monitor.snapshot().freezes().size());
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(GRAY + "Run /msc tps in game to get the link to the full page.");
            return;
        }

        MonitorServer web = plugin.getMonitorServer();
        int port = plugin.getConfig().getInt("monitor.port", 8765);
        try {
            port = web.start(port);
        } catch (IOException e) {
            sender.sendMessage(RED + "Could not open port " + port + " for the monitor page: " + e.getMessage());
            sender.sendMessage(GRAY + "Pick another one with monitor.port in config.yml.");
            return;
        }

        String url = "http://" + host() + ":" + port + "/" + web.issue(player.getUniqueId()) + "/";
        int minutes = plugin.getConfig().getInt("monitor.link-minutes", 30);
        player.sendMessage(Component.text("» Open the server monitor", NamedTextColor.AQUA, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(Component.text(url, NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Private link, valid " + minutes
                + " min. A new /msc tps cancels this one.", NamedTextColor.DARK_GRAY));
    }

    /** The address players reach the server on: monitor.public-host, else server-ip, else localhost. */
    private String host() {
        String configured = plugin.getConfig().getString("monitor.public-host", "");
        if (configured != null && !configured.isBlank()) return configured.trim();
        String ip = plugin.getServer().getIp();
        return ip == null || ip.isBlank() || ip.equals("0.0.0.0") ? "localhost" : ip;
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String color(double v, double good, double bad, boolean higherIsBetter) {
        if (Double.isNaN(v)) return GRAY.toString();
        boolean isGood = higherIsBetter ? v >= good : v <= good;
        boolean isBad = higherIsBetter ? v < bad : v > bad;
        return (isGood ? GREEN : isBad ? RED : YELLOW).toString();
    }
}
