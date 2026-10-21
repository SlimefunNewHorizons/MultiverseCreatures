package com.Chagui68.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The results of an event boss: the three teams and the three players that dealt it the most damage,
 * told to the whole server. A team's damage is what its members dealt, by the teams as they stand
 * when the boss falls.
 */
public final class EventRanking {

    private static final ChatColor[] PLACES = {ChatColor.GOLD, ChatColor.GRAY, ChatColor.RED};

    private EventRanking() {
    }

    /** The {@code n} highest totals, highest first. */
    static <K> List<Map.Entry<K, Double>> top(Map<K, Double> totals, int n) {
        List<Map.Entry<K, Double>> out = new ArrayList<>(totals.entrySet());
        out.sort(Map.Entry.<K, Double>comparingByValue().reversed());
        return out.subList(0, Math.min(n, out.size()));
    }

    /** Each team's total: its members' damage summed; players in no team count for none. */
    static Map<String, Double> byTeam(Map<UUID, Double> damage, Function<UUID, String> teamOf) {
        Map<String, Double> out = new HashMap<>();
        damage.forEach((player, dealt) -> {
            String team = teamOf.apply(player);
            if (team != null) out.merge(team, dealt, Double::sum);
        });
        return out;
    }

    public static void announce(Map<UUID, Double> damage, EventTeams teams) {
        if (damage.isEmpty()) return;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "===== WITHER STORM EVENT: RESULTS =====");
        if (teams != null) {
            List<Map.Entry<String, Double>> topTeams = top(byTeam(damage, p -> {
                EventTeams.Team t = teams.teamOf(p);
                return t == null ? null : t.name();
            }), 3);
            if (!topTeams.isEmpty()) {
                lines.add(ChatColor.LIGHT_PURPLE + "Top teams by damage:");
                for (int i = 0; i < topTeams.size(); i++) lines.add(place(i, topTeams.get(i).getKey(), topTeams.get(i).getValue()));
            }
        }
        lines.add(ChatColor.LIGHT_PURPLE + "Top players by damage:");
        List<Map.Entry<UUID, Double>> topPlayers = top(damage, 3);
        for (int i = 0; i < topPlayers.size(); i++) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(topPlayers.get(i).getKey());
            lines.add(place(i, p.getName() == null ? "?" : p.getName(), topPlayers.get(i).getValue()));
        }
        lines.forEach(Bukkit::broadcastMessage);
    }

    private static String place(int i, String name, double dealt) {
        return PLACES[i] + " #" + (i + 1) + " " + ChatColor.WHITE + name + ChatColor.DARK_GRAY + " - "
                + ChatColor.YELLOW + String.format(java.util.Locale.ROOT, "%,.0f", dealt) + " damage";
    }
}
