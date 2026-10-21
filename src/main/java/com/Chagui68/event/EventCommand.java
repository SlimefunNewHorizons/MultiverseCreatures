package com.Chagui68.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /mscevent team}: the teams of an event. What a player can do depends on where they stand:
 *
 * <pre>
 * no team    create &lt;name&gt;, join &lt;team&gt; (once invited), list, info [team]
 * member     leave, list, info [team]
 * leader     add &lt;player&gt; (invites them), remove &lt;player&gt;, leader &lt;player&gt;, disband, leave, list, info [team]
 * </pre>
 */
public final class EventCommand implements TabExecutor {

    private final EventTeams teams;

    public EventCommand(EventTeams teams) {
        this.teams = teams;
    }

    /** The team commands open to {@code player} right now. */
    List<String> options(UUID player) {
        if (teams.teamOf(player) == null) return List.of("create", "join", "list", "info");
        if (teams.isLeader(player)) return List.of("add", "remove", "leader", "disband", "leave", "list", "info");
        return List.of("leave", "list", "info");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only a player can be in an event team.");
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("team")) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " team <" + String.join("|", options(player.getUniqueId())) + ">");
            return true;
        }
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        String arg = args.length > 2 ? args[2] : null;
        UUID me = player.getUniqueId();
        if (!options(me).contains(sub)) {
            help(player, label);
            return true;
        }
        switch (sub) {
            case "create" -> {
                if (arg == null) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + label + " team create <name>");
                    return true;
                }
                if (tell(player, teams.create(me, arg), arg)) {
                    player.sendMessage(ChatColor.GREEN + "Team " + arg + " created. You lead it: /" + label + " team add <player> to invite.");
                }
            }
            case "join" -> {
                if (arg == null) {
                    List<String> invited = teams.invitesOf(me);
                    player.sendMessage(invited.isEmpty() ? ChatColor.RED + "Nobody has invited you to a team."
                            : ChatColor.YELLOW + "Invited by: " + String.join(", ", invited) + ". /" + label + " team join <team>");
                    return true;
                }
                if (tell(player, teams.join(me, arg), arg)) {
                    EventTeams.Team t = teams.teamOf(me);
                    tellTeam(t, ChatColor.GREEN + player.getName() + " joined " + t.name() + ".");
                }
            }
            case "leave" -> {
                EventTeams.Team t = teams.teamOf(me);
                if (tell(player, teams.leave(me), null)) {
                    player.sendMessage(ChatColor.YELLOW + "You left " + t.name() + ".");
                    EventTeams.Team after = teams.team(t.name());
                    if (after != null) tellTeam(after, ChatColor.YELLOW + player.getName() + " left the team. Leader: " + name(after.leader()) + ".");
                }
            }
            case "add" -> {
                Player target = arg == null ? null : Bukkit.getPlayerExact(arg);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + label + " team add <online player>");
                    return true;
                }
                if (tell(player, teams.invite(me, target.getUniqueId()), target.getName())) {
                    String team = teams.teamOf(me).name();
                    player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to " + team + ".");
                    target.sendMessage(ChatColor.GREEN + player.getName() + " invited you to the team " + team
                            + ". Join with /" + label + " team join " + team);
                }
            }
            case "remove", "leader" -> {
                UUID target = memberNamed(me, arg);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + label + " team " + sub + " <member>");
                    return true;
                }
                EventTeams.Team t = teams.teamOf(me);
                if (sub.equals("remove") && tell(player, teams.kick(me, target), arg)) {
                    tellTeam(t, ChatColor.YELLOW + name(target) + " was removed from " + t.name() + ".");
                    Player removed = Bukkit.getPlayer(target);
                    if (removed != null) removed.sendMessage(ChatColor.YELLOW + "You were removed from " + t.name() + ".");
                } else if (sub.equals("leader") && tell(player, teams.promote(me, target), arg)) {
                    tellTeam(t, ChatColor.GREEN + name(target) + " now leads " + t.name() + ".");
                }
            }
            case "disband" -> {
                EventTeams.Team t = teams.teamOf(me);
                List<UUID> members = List.copyOf(t.members());
                if (tell(player, teams.disband(me), null)) {
                    for (UUID m : members) {
                        Player p = Bukkit.getPlayer(m);
                        if (p != null) p.sendMessage(ChatColor.YELLOW + "The team " + t.name() + " was disbanded.");
                    }
                }
            }
            case "list" -> {
                if (teams.teams().isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "There are no teams yet.");
                    return true;
                }
                player.sendMessage(ChatColor.DARK_PURPLE + "Event teams:");
                for (EventTeams.Team t : teams.teams()) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + " " + t.name() + ChatColor.GRAY + " (" + t.members().size()
                            + ") led by " + name(t.leader()));
                }
            }
            case "info" -> {
                EventTeams.Team t = arg == null ? teams.teamOf(me) : teams.team(arg);
                if (t == null) {
                    player.sendMessage(ChatColor.RED + (arg == null ? "You are in no team: /" + label + " team info <team>" : "No team called " + arg + "."));
                    return true;
                }
                List<String> names = new ArrayList<>();
                for (UUID m : t.members()) names.add(m.equals(t.leader()) ? name(m) + " (leader)" : name(m));
                player.sendMessage(ChatColor.DARK_PURPLE + t.name() + ChatColor.GRAY + ": " + String.join(", ", names));
            }
            default -> help(player, label);
        }
        return true;
    }

    private void help(Player player, String label) {
        UUID me = player.getUniqueId();
        EventTeams.Team t = teams.teamOf(me);
        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Event teams" + ChatColor.GRAY
                + (t == null ? " - you are in no team" : " - " + t.name() + (teams.isLeader(me) ? " (you lead it)" : "")));
        for (String option : options(me)) {
            String usage = switch (option) {
                case "create" -> "create <name> - start a team and lead it";
                case "join" -> "join <team> - join a team that invited you";
                case "add" -> "add <player> - invite a player to your team";
                case "remove" -> "remove <member> - take a member out of your team";
                case "leader" -> "leader <member> - hand the team to a member";
                case "disband" -> "disband - break the team up";
                case "leave" -> "leave - leave your team";
                case "list" -> "list - every team";
                default -> "info [team] - who is in a team";
            };
            player.sendMessage(ChatColor.YELLOW + " /" + label + " team " + usage);
        }
    }

    /** Puts a failed change into words; true when it went through. */
    private boolean tell(Player player, EventTeams.Result result, String what) {
        String message = switch (result) {
            case OK -> null;
            case HAS_TEAM -> "You are already in a team.";
            case TARGET_HAS_TEAM -> what + " is already in a team.";
            case NO_TEAM -> "You are in no team.";
            case NOT_LEADER -> "Only the team's leader can do that.";
            case BAD_NAME -> "A team name is 3 to 16 letters, digits or underscores.";
            case NAME_TAKEN -> "There is already a team called " + what + ".";
            case NO_SUCH_TEAM -> "No team called " + what + ".";
            case NOT_INVITED -> "The team " + what + " has not invited you.";
            case NOT_MEMBER -> what + " is not in your team.";
            case SELF -> "You cannot do that to yourself.";
        };
        if (message != null) player.sendMessage(ChatColor.RED + message);
        return message == null;
    }

    private void tellTeam(EventTeams.Team team, String message) {
        for (UUID m : team.members()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.sendMessage(message);
        }
    }

    /** A member of {@code leader}'s team by name, online or not. */
    private UUID memberNamed(UUID leader, String name) {
        EventTeams.Team t = teams.teamOf(leader);
        if (t == null || name == null) return null;
        for (UUID m : t.members()) {
            if (name.equalsIgnoreCase(name(m))) return m;
        }
        return null;
    }

    private static String name(UUID player) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(player);
        return p.getName() == null ? player.toString().substring(0, 8) : p.getName();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player)) return out;
        UUID me = player.getUniqueId();
        if (args.length == 1) {
            out.add("team");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            out.addAll(options(me));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("team") && options(me).contains(args[1].toLowerCase(Locale.ROOT))) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "join" -> out.addAll(teams.invitesOf(me));
                case "info" -> teams.teams().forEach(t -> out.add(t.name()));
                case "add" -> Bukkit.getOnlinePlayers().forEach(p -> {
                    if (teams.teamOf(p.getUniqueId()) == null) out.add(p.getName());
                });
                case "remove", "leader" -> teams.teamOf(me).members().forEach(m -> {
                    if (!m.equals(me)) out.add(name(m));
                });
                default -> {
                }
            }
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(typed));
        return out;
    }
}
