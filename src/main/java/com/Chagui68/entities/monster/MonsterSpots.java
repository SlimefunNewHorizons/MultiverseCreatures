package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Where the Monster can wait for a player: on the floor, under a ceiling, on top of any solid block,
 * or in the air beside a player who is flying.
 *
 * <p>A spot holds when the whole box of the pose is free there (the block it lies on aside) and,
 * ideally, neither its eyes nor its chest are in the player's view. The open poses face the player
 * and start from where the viewer shows them round its player, so the game keeps their spacing. The
 * tight poses, made for a room one or two blocks across, are tried square to the blocks in all four
 * directions and centred on one or two cells, so they find the shaft, the corridor or the alcove they
 * were made for.</p>
 */
final class MonsterSpots {

    /** A pose, the point it hangs from, the angle it faces (radians about {@code y}), and whether it is out of sight. */
    record Spot(Pose pose, Vector anchor, float yaw, boolean hidden) {
    }

    /** The spot a search found, or why there was none. */
    record Search(Spot spot, String reason) {
    }

    /** Degrees from the centre of the view inside which a spot counts as seen when it is chosen. */
    static final double HIDDEN_ANGLE = 80;
    /** How far round a player a natural visit looks for a place. */
    static final int NATURAL_RADIUS = 6;
    /** Poses a natural visit tries before it settles for what it found. */
    private static final int NATURAL_TRIES = 12;
    /** How much wider a tight pose's room may not be, for a natural visit to call it tight. */
    private static final float SNUG = 0.7f;
    /** Closest an anchor may be to the player's feet. */
    private static final double TOO_CLOSE = 0.9;
    /** Most candidate points one search looks at. */
    private static final int MAX_BASES = 3000;
    private static final double[][] CELL_SHIFTS = {{0, 0}, {0.5, 0}, {-0.5, 0}, {0, 0.5}, {0, -0.5},
            {0.5, 0.5}, {0.5, -0.5}, {-0.5, 0.5}, {-0.5, -0.5}};

    private final MonsterRig rig;
    /** Each pose's box round its anchor, before it is turned. */
    private final Map<String, float[]> boxes = new HashMap<>();

    MonsterSpots(MonsterRig rig) {
        this.rig = rig;
        for (Pose pose : rig.poses().values()) {
            boxes.put(pose.name(), rig.bounds(pose, MonsterRig.body(pose)));
        }
    }

    /** Spots for a natural visit: hidden ones only, for a handful of poses that suit the place. */
    List<Spot> find(Player player, Random random) {
        List<Spot> spots = new ArrayList<>();
        List<Pose> poses = new ArrayList<>();
        for (Pose pose : rig.poses().values()) {
            if (pose.place() == Place.ACTION) continue;
            if (airborne(player) != (pose.place() == Place.AIR)) continue;
            poses.add(pose);
        }
        Collections.shuffle(poses, random);
        for (Pose pose : poses.subList(0, Math.min(NATURAL_TRIES, poses.size()))) {
            Search search = search(player, pose, NATURAL_RADIUS, true, true);
            if (search.spot() != null) spots.add(search.spot());
        }
        return spots;
    }

    /**
     * The nearest place within {@code radius} blocks of the player where {@code pose} fits. A spot out
     * of their view wins; when {@code hiddenOnly} is false a spot in plain sight is taken if there is no
     * other. With {@code snug}, a tight pose only takes a room that really is tight, so it folds itself
     * into a shaft or a corridor but never stands folded in the open. When there is nothing, the reason
     * says what was missing.
     */
    Search search(Player player, Pose pose, int radius, boolean hiddenOnly, boolean snug) {
        if (pose.place() == Place.ACTION) {
            return new Search(null, "\"" + pose.id() + "\" is what it does once it has been seen, not a place to wait");
        }
        List<Vector> bases = bases(player, pose, radius);
        if (bases.isEmpty()) {
            return new Search(null, switch (pose.place()) {
                case CEILING -> "there is no ceiling within " + radius + " blocks of the player";
                case BLOCK -> "there is no solid block with free space on top within " + radius + " blocks";
                case AIR -> "there is no open air round the player";
                default -> "there is no floor within " + radius + " blocks of the player";
            });
        }
        float[] box = boxes.get(pose.name());
        MonsterSpace space = new WorldSpace(player.getWorld());
        Vector feet = player.getLocation().toVector();
        int fitted = 0;
        Spot inSight = null;
        for (Vector base : bases.subList(0, Math.min(MAX_BASES, bases.size()))) {
            float facing = (float) Math.atan2(feet.getX() - base.getX(), feet.getZ() - base.getZ());
            for (float yaw : yaws(pose, facing, base)) {
                for (Vector anchor : anchors(pose, base, yaw, box)) {
                    Vector3d at = new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ());
                    Set<Long> support = MonsterScript.support(pose, at, yaw);
                    if (!MonsterSpace.fits(space, at, yaw, box, support)) continue;
                    if (snug && pose.space() != null && MonsterSpace.fits(space, at, yaw, grown(box), support)) continue;
                    fitted++;
                    boolean hidden = !seen(player, pose, anchor, yaw, HIDDEN_ANGLE);
                    Spot spot = new Spot(pose, anchor, yaw, hidden);
                    if (hidden) return new Search(spot, null);
                    if (inSight == null) inSight = spot;
                }
            }
        }
        if (inSight != null && !hiddenOnly) return new Search(inSight, null);
        if (fitted > 0) return new Search(null, "every place within " + radius + " blocks where it fits is in plain sight");
        return new Search(null, "nothing within " + radius + " blocks has room for it: \"" + pose.id() + "\" needs "
                + room(pose, box) + " of free space" + (pose.place() == Place.BLOCK ? " on top of a block" : ""));
    }

    /** The box with {@link #SNUG} more room on every side. */
    private static float[] grown(float[] box) {
        return new float[]{box[0] - SNUG, box[1], box[2] - SNUG, box[3] + SNUG, box[4], box[5] + SNUG};
    }

    /** True when the player is gliding, flying, or has nothing under them for a few blocks. */
    static boolean airborne(Player player) {
        if (player.isGliding() || player.isFlying()) return true;
        Block feet = player.getLocation().getBlock();
        for (int down = 1; down <= 4; down++) {
            if (!feet.getRelative(0, -down, 0).isPassable()) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ candidates

    /** The points a pose could hang from round the player, nearest first. */
    private List<Vector> bases(Player player, Pose pose, int radius) {
        World world = player.getWorld();
        Location feet = player.getLocation();
        int fx = feet.getBlockX(), fy = feet.getBlockY(), fz = feet.getBlockZ();
        List<Vector> found = new ArrayList<>();
        Vector designed = designed(player, pose);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = fx + dx, z = fz + dz;
                switch (pose.place()) {
                    case GROUND -> {
                        for (int y = fy + 3; y >= fy - 4; y--) {
                            Block below = world.getBlockAt(x, y - 1, z);
                            if (!below.isPassable() && world.getBlockAt(x, y, z).isPassable()) {
                                found.add(new Vector(x + 0.5, below.getY() + below.getBoundingBox().getHeight(), z + 0.5));
                            }
                        }
                    }
                    case CEILING -> {
                        for (int y = fy + 1; y <= fy + 8; y++) {
                            if (!world.getBlockAt(x, y, z).isPassable() && world.getBlockAt(x, y - 1, z).isPassable()) {
                                found.add(new Vector(x + 0.5, y, z + 0.5));
                                break;
                            }
                        }
                    }
                    case BLOCK -> {
                        for (int y = fy - 3; y <= fy + 2; y++) {
                            Block b = world.getBlockAt(x, y, z);
                            if (!top(b)) continue;
                            double height = y + b.getBoundingBox().getHeight();
                            if (!pose.wide()) {
                                found.add(new Vector(x + 0.5, height, z + 0.5));
                                continue;
                            }
                            // Two tops side by side, level with each other: a double chest, or any two blocks.
                            Block east = b.getRelative(1, 0, 0), south = b.getRelative(0, 0, 1);
                            if (top(east) && east.getBoundingBox().getHeight() == b.getBoundingBox().getHeight()) {
                                found.add(new Vector(x + 1.0, height, z + 0.5));
                            }
                            if (top(south) && south.getBoundingBox().getHeight() == b.getBoundingBox().getHeight()) {
                                found.add(new Vector(x + 0.5, height, z + 1.0));
                            }
                        }
                    }
                    case AIR -> {
                        // Level with the designed spot, which floats a block up for a player on the ground.
                        double up = designed == null ? 1.0 : designed.getY() - feet.getY();
                        found.add(new Vector(x + 0.5, feet.getY() + up, z + 0.5));
                    }
                    default -> {
                    }
                }
            }
        }
        Vector at = feet.toVector();
        found.removeIf(v -> Math.hypot(v.getX() - at.getX(), v.getZ() - at.getZ()) < TOO_CLOSE);
        found.sort(Comparator.comparingDouble(v -> v.distanceSquared(at)));
        if (designed != null) found.add(0, designed);
        return found;
    }

    /**
     * Where the viewer shows an open floor or air pose round its player, carried over to this player:
     * the viewer's player faces {@code +z} with {@code +x} on their left, as a Minecraft player at yaw 0
     * does. {@code null} for the other poses, or when there is no floor there.
     */
    private Vector designed(Player player, Pose pose) {
        if (pose.space() != null || (pose.place() != Place.GROUND && pose.place() != Place.AIR)) return null;
        float[] stand = rig.player();
        Location feet = player.getLocation();
        double yaw = Math.toRadians(feet.getYaw());
        Vector left = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector at = feet.toVector().add(left.multiply(pose.at().x - stand[0])).add(forward.multiply(pose.at().z - stand[2]));
        if (pose.place() == Place.AIR) return at.setY(feet.getY() + (airborne(player) ? pose.at().y - stand[1] : 1.0));
        Double floor = floor(player.getWorld(), at, feet.getBlockY());
        return floor == null ? null : at.setY(floor);
    }

    /** True for a solid block with room above it, to lie or sit on. */
    private static boolean top(Block b) {
        return !b.isPassable() && b.getType().isSolid() && b.getRelative(0, 1, 0).isPassable();
    }

    /**
     * The angles to try: facing the player; for a tight pose, square to the blocks, nearest first;
     * for a double chest, along it, its front to the player's side first.
     */
    private static float[] yaws(Pose pose, float facing, Vector base) {
        if (pose.wide()) {
            // Along x when the base sits between two blocks in x (a whole x), along z otherwise.
            boolean alongX = base.getX() == Math.floor(base.getX());
            float a = alongX ? 0f : (float) (Math.PI / 2), b = a + (float) Math.PI;
            return Math.cos(facing - a) >= Math.cos(facing - b) ? new float[]{a, b} : new float[]{b, a};
        }
        if (pose.space() == null) return new float[]{facing};
        float[] square = new float[4];
        int k = Math.round(facing / (float) (Math.PI / 2));
        int[] order = {0, 1, -1, 2};
        for (int i = 0; i < 4; i++) {
            square[i] = (float) ((k + order[i]) * Math.PI / 2);
        }
        return square;
    }

    /** The anchors to try round a base: the base itself, or for a tight pose its box centred on one or two cells. */
    private static List<Vector> anchors(Pose pose, Vector base, float yaw, float[] box) {
        if (pose.space() == null) return List.of(base);
        double cx = (box[0] + box[3]) / 2, cz = (box[2] + box[5]) / 2;
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        double wx = cx * cos + cz * sin, wz = -cx * sin + cz * cos;
        List<Vector> out = new ArrayList<>(CELL_SHIFTS.length);
        for (double[] shift : CELL_SHIFTS) {
            out.add(new Vector(base.getX() + shift[0] - wx, base.getY(), base.getZ() + shift[1] - wz));
        }
        return out;
    }

    // ------------------------------------------------------------------ checks

    /** True when the player sees its eyes or its chest, within {@code angle} degrees of where they look. */
    boolean seen(Player player, Pose pose, Vector anchor, float yaw, double angle) {
        Matrix4f turned = new Matrix4f().rotateY(yaw).mul(MonsterRig.body(pose));
        return sees(player, at(anchor, rig.eyes(pose, turned)), angle)
                || sees(player, at(anchor, rig.chest(pose, turned)), angle);
    }

    /** The world's blocks, as the Monster's planning sees them. */
    static final class WorldSpace implements MonsterSpace {
        private final World world;

        WorldSpace(World world) {
            this.world = world;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            return !world.getBlockAt(x, y, z).isPassable();
        }

        @Override
        public double height(int x, int y, int z) {
            Block b = world.getBlockAt(x, y, z);
            return b.isPassable() ? 0 : b.getBoundingBox().getHeight();
        }
    }

    /**
     * True when {@code point} is within {@code angle} degrees of where the player looks, close enough
     * and with nothing solid in between.
     */
    static boolean sees(Player player, Vector point, double angle) {
        Location eye = player.getEyeLocation();
        Vector to = point.clone().subtract(eye.toVector());
        double distance = to.length();
        if (distance < 0.01) return true;
        if (distance > 48) return false;
        double cos = to.dot(eye.getDirection()) / distance;
        if (cos < Math.cos(Math.toRadians(angle))) return false;
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, to.normalize(), distance - 0.5,
                FluidCollisionMode.NEVER, true);
        return hit == null;
    }

    /**
     * The height to stand at round {@code column}: the top of the highest block with room above it,
     * a little above or below {@code feetY}.
     */
    static Double floor(World world, Vector column, int feetY) {
        for (int y = feetY + 2; y >= feetY - 3; y--) {
            Block below = world.getBlockAt(column.getBlockX(), y - 1, column.getBlockZ());
            Block at = world.getBlockAt(column.getBlockX(), y, column.getBlockZ());
            if (!below.isPassable() && at.isPassable()) {
                return below.getY() + below.getBoundingBox().getHeight();
            }
        }
        return null;
    }

    /** A point of the model, given relative to the anchor, in the world. */
    static Vector at(Vector anchor, Vector3f local) {
        return new Vector(anchor.getX() + local.x, anchor.getY() + local.y, anchor.getZ() + local.z);
    }

    /** The room a pose asks for, in blocks: its own space for a tight pose, its box otherwise. */
    private static String room(Pose pose, float[] box) {
        float[] s = pose.space();
        float w = s != null ? s[0] : box[3] - box[0], d = s != null ? s[1] : box[5] - box[2], h = s != null ? s[2] : box[4] - box[1];
        return String.format(Locale.ROOT, "%s x %s x %s blocks (wide x deep x tall)", size(w), size(d), size(h));
    }

    private static String size(float v) {
        return v == Math.round(v) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }
}
