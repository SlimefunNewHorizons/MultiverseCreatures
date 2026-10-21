package com.Chagui68.event;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The teams of an event ({@code /mscevent team}): each has a name, a leader and its members, and a
 * player is in one team at most. The leader invites players, who join with the invite; members leave
 * on their own. Teams are saved to {@code event-teams.yml} after every change; invites are not.
 */
public final class EventTeams {

    /** A team; {@code members} holds the leader too, in the order they joined. */
    public record Team(String name, UUID leader, Set<UUID> members) {
    }

    /** What a change to the teams came to, for the command to put into words. */
    public enum Result {
        OK, HAS_TEAM, TARGET_HAS_TEAM, NO_TEAM, NOT_LEADER, BAD_NAME, NAME_TAKEN, NO_SUCH_TEAM, NOT_INVITED, NOT_MEMBER, SELF
    }

    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, String> teamOf = new HashMap<>();
    /** Each player's pending invites, by team key. */
    private final Map<UUID, Set<String>> invites = new HashMap<>();
    private final File file;
    private final Logger log;

    /** Teams kept in {@code file}, or only in memory when it is {@code null}. */
    public EventTeams(File file, Logger log) {
        this.file = file;
        this.log = log;
        load();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /** 3 to 16 letters, digits or underscores, as a player name. */
    static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    public Team teamOf(UUID player) {
        String k = teamOf.get(player);
        return k == null ? null : teams.get(k);
    }

    public Team team(String name) {
        return name == null ? null : teams.get(key(name));
    }

    public Collection<Team> teams() {
        return teams.values();
    }

    /** The teams that invited {@code player}, by name. */
    public List<String> invitesOf(UUID player) {
        List<String> out = new ArrayList<>();
        for (String k : invites.getOrDefault(player, Set.of())) {
            Team t = teams.get(k);
            if (t != null) out.add(t.name());
        }
        return out;
    }

    public boolean isLeader(UUID player) {
        Team t = teamOf(player);
        return t != null && t.leader().equals(player);
    }

    public Result create(UUID leader, String name) {
        if (teamOf.containsKey(leader)) return Result.HAS_TEAM;
        if (!validName(name)) return Result.BAD_NAME;
        if (teams.containsKey(key(name))) return Result.NAME_TAKEN;
        Set<UUID> members = new LinkedHashSet<>();
        members.add(leader);
        teams.put(key(name), new Team(name, leader, members));
        teamOf.put(leader, key(name));
        invites.remove(leader);
        save();
        return Result.OK;
    }

    /** The leader invites {@code player}, who joins with {@link #join}. */
    public Result invite(UUID leader, UUID player) {
        Team t = teamOf(leader);
        if (t == null) return Result.NO_TEAM;
        if (!t.leader().equals(leader)) return Result.NOT_LEADER;
        if (leader.equals(player)) return Result.SELF;
        if (teamOf.containsKey(player)) return Result.TARGET_HAS_TEAM;
        invites.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(key(t.name()));
        return Result.OK;
    }

    public Result join(UUID player, String name) {
        if (teamOf.containsKey(player)) return Result.HAS_TEAM;
        Team t = team(name);
        if (t == null) return Result.NO_SUCH_TEAM;
        if (!invites.getOrDefault(player, Set.of()).contains(key(t.name()))) return Result.NOT_INVITED;
        t.members().add(player);
        teamOf.put(player, key(t.name()));
        invites.remove(player);
        save();
        return Result.OK;
    }

    /** Leaves the team; a leader hands it to the next member, and the last one out disbands it. */
    public Result leave(UUID player) {
        Team t = teamOf(player);
        if (t == null) return Result.NO_TEAM;
        drop(t, player);
        save();
        return Result.OK;
    }

    /** The leader removes a member. */
    public Result kick(UUID leader, UUID player) {
        Team t = teamOf(leader);
        if (t == null) return Result.NO_TEAM;
        if (!t.leader().equals(leader)) return Result.NOT_LEADER;
        if (leader.equals(player)) return Result.SELF;
        if (!t.members().contains(player)) return Result.NOT_MEMBER;
        drop(t, player);
        save();
        return Result.OK;
    }

    /** The leader hands the team to another member. */
    public Result promote(UUID leader, UUID player) {
        Team t = teamOf(leader);
        if (t == null) return Result.NO_TEAM;
        if (!t.leader().equals(leader)) return Result.NOT_LEADER;
        if (leader.equals(player)) return Result.SELF;
        if (!t.members().contains(player)) return Result.NOT_MEMBER;
        teams.put(key(t.name()), new Team(t.name(), player, t.members()));
        save();
        return Result.OK;
    }

    public Result disband(UUID leader) {
        Team t = teamOf(leader);
        if (t == null) return Result.NO_TEAM;
        if (!t.leader().equals(leader)) return Result.NOT_LEADER;
        remove(t);
        save();
        return Result.OK;
    }

    private void drop(Team t, UUID player) {
        t.members().remove(player);
        teamOf.remove(player);
        if (t.members().isEmpty()) {
            remove(t);
        } else if (t.leader().equals(player)) {
            teams.put(key(t.name()), new Team(t.name(), t.members().iterator().next(), t.members()));
        }
    }

    private void remove(Team t) {
        teams.remove(key(t.name()));
        t.members().forEach(teamOf::remove);
        invites.values().forEach(set -> set.remove(key(t.name())));
    }

    // ------------------------------------------------------------------ saving

    private void load() {
        if (file == null || !file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("teams");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            try {
                UUID leader = UUID.fromString(root.getString(name + ".leader", ""));
                Set<UUID> members = new LinkedHashSet<>();
                members.add(leader);
                for (String m : root.getStringList(name + ".members")) members.add(UUID.fromString(m));
                members.removeIf(teamOf::containsKey);
                if (!members.contains(leader) || !validName(name)) continue;
                teams.put(key(name), new Team(name, leader, members));
                members.forEach(m -> teamOf.put(m, key(name)));
            } catch (IllegalArgumentException e) {
                log.warning("Skipping event team '" + name + "' in " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void save() {
        if (file == null) return;
        YamlConfiguration yml = new YamlConfiguration();
        for (Team t : teams.values()) {
            yml.set("teams." + t.name() + ".leader", t.leader().toString());
            yml.set("teams." + t.name() + ".members", t.members().stream().map(UUID::toString).toList());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            log.warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
