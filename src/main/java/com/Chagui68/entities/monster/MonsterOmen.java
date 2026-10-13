package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * What comes before a visit, for the one player it is coming for: steps behind them that stop when
 * they get close, a door somewhere near, the lights round them going out one by one, and once,
 * maybe, a glimpse of it at the edge of their sight, gone before they can look at it.
 *
 * <p>Only that player hears and sees any of it. The lights are only out on their screen, and stay
 * out until the visit that follows is over; {@link #end()} puts them back.</p>
 */
final class MonsterOmen {

    /** The lights it can put out: ones whose block can be shown as air without a hole in the build. */
    private static final Set<Material> LIGHTS = EnumSet.of(Material.TORCH, Material.WALL_TORCH,
            Material.SOUL_TORCH, Material.SOUL_WALL_TORCH, Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
            Material.LANTERN, Material.SOUL_LANTERN);
    /** How far round the player it looks for a light to put out. */
    private static final int LIGHT_RADIUS = 8;
    /** Ticks between two steps behind the player. */
    private static final int STEP_TICKS = 7;
    /** Ticks a glimpse lasts at most, if the player never turns to it. */
    private static final int GLIMPSE_TICKS = 40;
    /** Degrees from the centre of the view at which a glimpse is gone: before they can see it well. */
    private static final double GLIMPSE_GONE_ANGLE = 35;

    private final MonsterRig rig;
    private final UUID playerId;
    private final World world;
    private final int length;
    private final Random random;
    /** The lights it put out on the player's screen, by block. */
    private final Map<Block, BlockData> dark = new LinkedHashMap<>();

    private int ticks;
    private int nextCue = 40;
    /** Steps still to come, where the next one falls and which way they come. */
    private int steps;
    private Vector stepAt;
    private Vector stepWay;
    /** The tick a door it opened closes on, and where; 0 when no door is open. */
    private int doorCloses;
    private Location door;
    private boolean glimpsed;
    private MonsterBody glimpse;
    private Vector glimpseChest;
    private int glimpseTicks;

    MonsterOmen(MonsterRig rig, Player player, int lengthTicks, Random random) {
        this.rig = rig;
        this.playerId = player.getUniqueId();
        this.world = player.getWorld();
        this.length = lengthTicks;
        this.random = random;
    }

    UUID playerId() {
        return playerId;
    }

    /** One tick; false once it is over, the player gone or the time up. */
    boolean tick(Player player) {
        if (player == null || !player.isOnline() || player.isDead() || !player.getWorld().equals(world)) {
            end();
            return false;
        }
        ticks++;
        if (ticks >= length) {
            hideGlimpse();
            return false;
        }
        tickGlimpse(player);
        tickSteps(player);
        if (doorCloses > 0 && ticks >= doorCloses) {
            player.playSound(door, Sound.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 0.8f, 0.8f);
            doorCloses = 0;
        }
        if (ticks >= nextCue) {
            cue(player);
            nextCue = ticks + 60 + random.nextInt(80);
        }
        return true;
    }

    /** Something new: steps, a door, a light going out, or, past halfway and once, a glimpse of it. */
    private void cue(Player player) {
        double roll = random.nextDouble();
        if (!glimpsed && ticks > length / 2 && roll < 0.25 && glimpse(player)) return;
        if (roll < 0.45 && steps == 0) {
            startSteps(player);
        } else if (roll < 0.75 && putOutLight(player)) {
            return;
        } else if (doorCloses == 0) {
            door = around(player, 6 + random.nextInt(5));
            player.playSound(door, Sound.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 0.8f, 0.8f);
            doorCloses = ticks + 25 + random.nextInt(20);
        }
    }

    // ------------------------------------------------------------------ steps

    /** Steps start six blocks behind the player and come towards them. */
    private void startSteps(Player player) {
        Vector back = flat(player.getEyeLocation().getDirection()).multiply(-1);
        stepWay = back.clone().multiply(-1);
        stepAt = player.getLocation().toVector().add(back.multiply(6));
        steps = 4 + random.nextInt(3);
    }

    /** One step every few ticks, closer each time, until they are right behind the player. Then nothing. */
    private void tickSteps(Player player) {
        if (steps == 0 || ticks % STEP_TICKS != 0) return;
        Double floor = MonsterSpots.floor(world, stepAt, player.getLocation().getBlockY());
        if (floor == null || stepAt.clone().setY(0).distance(player.getLocation().toVector().setY(0)) < 2) {
            steps = 0;
            return;
        }
        Location at = new Location(world, stepAt.getX(), floor, stepAt.getZ());
        Block under = at.clone().subtract(0, 0.1, 0).getBlock();
        Sound sound = under.getType().isSolid() ? under.getBlockData().getSoundGroup().getStepSound() : Sound.BLOCK_STONE_STEP;
        player.playSound(at, sound, SoundCategory.HOSTILE, 0.6f, 0.75f + random.nextFloat() * 0.1f);
        stepAt.add(stepWay.clone().multiply(0.8));
        steps--;
    }

    // ------------------------------------------------------------------ lights

    /** Puts out a light near the player, on their screen only, with a hiss. False when there is none left. */
    private boolean putOutLight(Player player) {
        Block centre = player.getLocation().getBlock();
        List<Block> found = new ArrayList<>();
        for (int dx = -LIGHT_RADIUS; dx <= LIGHT_RADIUS; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                for (int dz = -LIGHT_RADIUS; dz <= LIGHT_RADIUS; dz++) {
                    Block b = centre.getRelative(dx, dy, dz);
                    if (LIGHTS.contains(b.getType()) && !dark.containsKey(b)) found.add(b);
                }
            }
        }
        if (found.isEmpty()) return false;
        Block light = found.get(random.nextInt(found.size()));
        dark.put(light, light.getBlockData());
        player.sendBlockChange(light.getLocation(), Material.AIR.createBlockData());
        player.playSound(light.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ the glimpse

    /**
     * It stands, still and facing them, out at the edge of the player's sight, seven to twelve blocks
     * off; false, with nothing shown, when no place there fits it in the open.
     */
    private boolean glimpse(Player player) {
        Pose pose = rig.poses().containsKey("head tilt") ? rig.pose("head tilt") : rig.pose("idle");
        if (pose.place() != Place.GROUND) pose = rig.pose("idle");
        float[] box = rig.bounds(pose, MonsterRig.body(pose));
        MonsterSpace space = new MonsterSpots.WorldSpace(world);
        Location eye = player.getEyeLocation();
        Vector look = flat(eye.getDirection());
        for (int attempt = 0; attempt < 8; attempt++) {
            double side = Math.toRadians((55 + random.nextInt(16)) * (random.nextBoolean() ? 1 : -1));
            double distance = 7 + random.nextInt(6);
            Vector way = new Vector(look.getX() * Math.cos(side) - look.getZ() * Math.sin(side), 0,
                    look.getX() * Math.sin(side) + look.getZ() * Math.cos(side));
            Vector column = player.getLocation().toVector().add(way.multiply(distance));
            Double floor = MonsterSpots.floor(world, column, player.getLocation().getBlockY());
            if (floor == null) continue;
            Vector3d anchor = new Vector3d(column.getBlockX() + 0.5, floor, column.getBlockZ() + 0.5);
            float yaw = MonsterMotion.toward(anchor, new Vector3d(eye.getX(), eye.getY(), eye.getZ()), 0f);
            if (!MonsterSpace.fits(space, anchor, yaw, box, Set.of())) continue;
            Matrix4f turned = new Matrix4f().rotateY(yaw).mul(MonsterRig.body(pose));
            Vector base = new Vector(anchor.x, anchor.y, anchor.z);
            Vector chest = MonsterSpots.at(base, rig.chest(pose, turned));
            Vector eyes = MonsterSpots.at(base, rig.eyes(pose, turned));
            if (!MonsterSpots.clearLine(player, chest) || !MonsterSpots.clearLine(player, eyes)) continue;
            glimpse = new MonsterBody(rig);
            glimpse.spawn(world, base, yaw, pose);
            glimpseChest = chest;
            glimpseTicks = 0;
            glimpsed = true;
            return true;
        }
        return false;
    }

    /** Gone the moment the player turns to it, or after a couple of seconds if they never do. */
    private void tickGlimpse(Player player) {
        if (glimpse == null) return;
        if (++glimpseTicks > GLIMPSE_TICKS || MonsterSpots.sees(player, glimpseChest, GLIMPSE_GONE_ANGLE)) {
            hideGlimpse();
        }
    }

    private void hideGlimpse() {
        if (glimpse != null) glimpse.remove();
        glimpse = null;
    }

    // ------------------------------------------------------------------ the end

    /** Takes the glimpse away and puts every light back on the player's screen. */
    void end() {
        hideGlimpse();
        Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline() && player.getWorld().equals(world)) {
            for (Block b : dark.keySet()) {
                player.sendBlockChange(b.getLocation(), b.getBlockData());
            }
        }
        dark.clear();
    }

    /** A point {@code distance} blocks from the player, any way but the one they look. */
    private Location around(Player player, double distance) {
        double angle = Math.toRadians(90 + random.nextInt(181));
        Vector look = flat(player.getEyeLocation().getDirection());
        Vector way = new Vector(look.getX() * Math.cos(angle) - look.getZ() * Math.sin(angle), 0,
                look.getX() * Math.sin(angle) + look.getZ() * Math.cos(angle));
        return player.getLocation().add(way.multiply(distance)).add(0, 1, 0);
    }

    private static Vector flat(Vector v) {
        Vector f = new Vector(v.getX(), 0, v.getZ());
        return f.lengthSquared() < 1.0e-6 ? new Vector(0, 0, 1) : f.normalize();
    }
}
