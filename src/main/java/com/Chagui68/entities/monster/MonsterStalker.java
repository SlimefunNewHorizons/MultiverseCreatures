package com.Chagui68.entities.monster;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.utils.MscWorldPolicy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sends the Monster after players: every so often, a player in the dark may find it waiting
 * somewhere they are not looking. At most one visit per player, a cooldown after each, and a cap on
 * how many run at once. Everything is read from {@code entities.monster} when it is used, so a
 * config reload applies to the next visit.
 */
public final class MonsterStalker {

    /** Chance it picks a ceiling or a block over the floor, when the place offers both. */
    private static final double PLACE_OVER_FLOOR = 0.6;
    /** How close a player has to be to a spawn command's point to be the one it visits. */
    private static final double COMMAND_RANGE = 24;
    /** How far round the player {@code /msc troll} looks for a place for the position asked. */
    public static final int TROLL_RADIUS = 10;

    /** What {@code /msc troll} did: whether it came, and what to tell whoever sent it. */
    public record TrollResult(boolean spawned, String message) {
    }

    private final MultiverseCreatures plugin;
    private final MonsterRig rig;
    private final MonsterSpots spots;
    private final Random random = new Random();
    private final Map<UUID, MonsterHaunt> haunts = new HashMap<>();
    private final Map<UUID, Long> lastVisit = new HashMap<>();

    /**
     * The loop that moves every visit and, every few seconds, may start one. Held so
     * {@link #stopAll()} can end it.
     */
    private BukkitTask ticker;
    private long tick;

    public MonsterStalker(MultiverseCreatures plugin) {
        this.plugin = plugin;
        this.rig = MonsterRig.get();
        this.spots = new MonsterSpots(rig);
        if (!plugin.isEnabled("entities.monster")) return;
        startTicker();
    }

    private void startTicker() {
        if (ticker != null) ticker.cancel();
        ticker = new BukkitRunnable() {
            @Override
            public void run() {
                tickAll();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tickAll() {
        tick++;
        Iterator<Map.Entry<UUID, MonsterHaunt>> it = haunts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, MonsterHaunt> entry = it.next();
            if (!entry.getValue().tick(Bukkit.getPlayer(entry.getKey()))) {
                lastVisit.put(entry.getKey(), System.currentTimeMillis());
                it.remove();
            }
        }
        FileConfiguration config = plugin.getConfig();
        long every = Math.max(1, config.getLong("entities.monster.check-interval-seconds", 20)) * 20L;
        if (tick % every == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (due(player, config)) visit(player);
            }
        }
    }

    /** True when a player meets everything a visit asks for, the roll of the dice included. */
    private boolean due(Player player, FileConfiguration config) {
        if (!plugin.isEnabled("entities.monster")) return false;
        if (haunts.size() >= config.getInt("entities.monster.max-active", 5)) return false;
        if (haunts.containsKey(player.getUniqueId()) || player.getGameMode() == GameMode.SPECTATOR) return false;
        World world = player.getWorld();
        if (!MscWorldPolicy.isAllowed(plugin, world)) return false;
        Long last = lastVisit.get(player.getUniqueId());
        long cooldown = config.getLong("entities.monster.cooldown-seconds", 900) * 1000L;
        if (last != null && System.currentTimeMillis() - last < cooldown) return false;
        if (player.getEyeLocation().getBlock().getLightLevel() > config.getInt("entities.monster.max-light-level", 7)) {
            return false;
        }
        if (config.getBoolean("entities.monster.night-only", false)
                && world.getEnvironment() == World.Environment.NORMAL) {
            long time = world.getTime();
            if (time < 13000 || time > 23000) return false;
        }
        return random.nextDouble() < config.getDouble("entities.monster.chance", 0.04);
    }

    /**
     * Starts a visit to the player now, wherever it fits and they are not looking.
     *
     * @return false when there is no such place round them
     */
    public boolean visit(Player player) {
        if (haunts.containsKey(player.getUniqueId())) return false;
        MonsterSpots.Spot spot = choose(spots.find(player, random));
        if (spot == null) return false;
        start(player, spot);
        return true;
    }

    private void start(Player player, MonsterSpots.Spot spot) {
        FileConfiguration config = plugin.getConfig();
        MonsterScript.Settings settings = new MonsterScript.Settings(
                config.getDouble("entities.monster.approach-speed", 0.35),
                config.getInt("entities.monster.hand-ticks", 25),
                config.getInt("entities.monster.lurk-seconds", 45) * 20,
                config.getDouble("entities.monster.view-angle", 30));
        haunts.put(player.getUniqueId(), new MonsterHaunt(rig, spots, player, spot, settings, random));
        if (ticker == null) startTicker();
    }

    /**
     * {@code /msc troll}: sends it to {@code target} in the position named (a pose's id, or
     * {@code random}), somewhere within {@link #TROLL_RADIUS} blocks. A place out of their view is
     * taken first; one in plain sight only when there is no other, and the answer says so. When there
     * is no place at all, the answer says why.
     */
    public TrollResult troll(Player target, String position) {
        if (!plugin.isEnabled("entities.monster")) {
            return new TrollResult(false, "the Monster is turned off (entities.monster.enabled in config.yml)");
        }
        if (!MscWorldPolicy.isAllowed(plugin, target.getWorld())) {
            return new TrollResult(false, "MultiverseCreatures does not run in " + target.getWorld().getName());
        }
        if (haunts.containsKey(target.getUniqueId())) {
            return new TrollResult(false, target.getName() + " already has it after them");
        }
        MonsterSpots.Search search;
        if (position.equalsIgnoreCase("random")) {
            search = anywhere(target);
        } else {
            MonsterRig.Pose pose = rig.byId(position);
            if (pose == null) {
                return new TrollResult(false, "there is no position called \"" + position + "\"");
            }
            search = spots.search(target, pose, TROLL_RADIUS, false, false);
        }
        MonsterSpots.Spot spot = search.spot();
        if (spot == null) return new TrollResult(false, search.reason());
        start(target, spot);
        double away = spot.anchor().distance(target.getLocation().toVector());
        return new TrollResult(true, String.format(java.util.Locale.ROOT, "it waits for %s %s, %.1f blocks away, %s",
                target.getName(), spot.pose().id(), away, spot.hidden()
                        ? "out of their sight"
                        : "in plain sight (there was nowhere hidden), so they will see it at once"));
    }

    /** Any position that fits round the player, a hidden one first. */
    private MonsterSpots.Search anywhere(Player target) {
        List<MonsterRig.Pose> poses = new ArrayList<>();
        for (MonsterRig.Pose pose : rig.poses().values()) {
            if (pose.place() != Place.ACTION) poses.add(pose);
        }
        java.util.Collections.shuffle(poses, random);
        MonsterSpots.Spot inSight = null;
        for (MonsterRig.Pose pose : poses) {
            MonsterSpots.Search search = spots.search(target, pose, TROLL_RADIUS, false, false);
            MonsterSpots.Spot spot = search.spot();
            if (spot == null) continue;
            if (spot.hidden()) return search;
            if (inSight == null) inSight = spot;
        }
        return inSight != null ? new MonsterSpots.Search(inSight, null)
                : new MonsterSpots.Search(null, "no position fits anywhere within " + TROLL_RADIUS + " blocks of the player");
    }

    /** The positions {@code /msc troll} takes, for its help and tab completion. */
    public List<String> positions() {
        List<String> ids = new ArrayList<>();
        ids.add("random");
        for (MonsterRig.Pose pose : rig.poses().values()) {
            if (pose.place() != Place.ACTION) ids.add(pose.id());
        }
        return ids;
    }

    /**
     * {@code /msc spawn monster}: a visit to the player nearest the point, now, whatever the chance,
     * the light or the cooldown say.
     */
    public boolean trySpawn(Location location) {
        if (!plugin.isEnabled("entities.monster") || location.getWorld() == null) return false;
        Player nearest = null;
        double best = COMMAND_RANGE * COMMAND_RANGE;
        for (Player player : location.getWorld().getPlayers()) {
            double d = player.getLocation().distanceSquared(location);
            if (d <= best) {
                best = d;
                nearest = player;
            }
        }
        return nearest != null && visit(nearest);
    }

    /**
     * One of the spots: a flying player gets one in the air; otherwise a ceiling or a chest is
     * preferred where there is one, since those are the places a player least expects.
     */
    private MonsterSpots.Spot choose(List<MonsterSpots.Spot> found) {
        List<MonsterSpots.Spot> air = new ArrayList<>(), floor = new ArrayList<>(), places = new ArrayList<>();
        for (MonsterSpots.Spot spot : found) {
            Place place = spot.pose().place();
            (place == Place.AIR ? air : place == Place.GROUND ? floor : places).add(spot);
        }
        if (!air.isEmpty()) return pick(air);
        if (!places.isEmpty() && (floor.isEmpty() || random.nextDouble() < PLACE_OVER_FLOOR)) return pick(places);
        return floor.isEmpty() ? null : pick(floor);
    }

    private MonsterSpots.Spot pick(List<MonsterSpots.Spot> spots) {
        return spots.get(random.nextInt(spots.size()));
    }

    /** Ends every visit and the loop; the bodies go with them. */
    public void stopAll() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        for (MonsterHaunt haunt : haunts.values()) {
            haunt.end();
        }
        haunts.clear();
    }
}
