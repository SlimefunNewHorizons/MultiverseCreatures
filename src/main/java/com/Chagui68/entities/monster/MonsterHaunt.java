package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterScript.Event;
import com.Chagui68.entities.monster.MonsterScript.Frame;
import com.Chagui68.entities.monster.MonsterScript.Phase;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One visit of the Monster to one player: {@link MonsterScript} plans it against the world's blocks
 * tick by tick, and this draws it with a {@link MonsterBody} and plays its sounds and effects.
 *
 * <p>It waits, still, in a pose that suits where it is. Once the player looks at it, it plays one of
 * the script's behaviours (gets up and walks over, rises from a double chest, rushes on all fours,
 * runs along the ceiling, blinks into their face, or hides and waits again), always where its body
 * fits, and ends with its face in theirs, screaming. Left unseen too long, it leaves without a
 * sound.</p>
 *
 * <p>From the moment it comes, the player hears it breathing where it is: slow while it waits,
 * faster once it comes for them. Only that player hears it. Its head turns, slowly, to follow them
 * wherever it is shown, and while it comes for them their sight pulses dark. Left unseen, it goes and
 * leaves its footprints behind, and maybe a door near where it waited standing open.</p>
 */
final class MonsterHaunt {

    /**
     * How the visit feels, from {@code entities.monster}: the Darkness pulse while it comes, how long
     * the footprints of a visit nobody saw stay, and the resource pack sounds (blank for vanilla).
     */
    record Feel(boolean darkness, int traceTicks, String scream, String breath) {
    }

    /** Degrees a turn of its head goes at most, each way: [pitch, yaw]. */
    private static final float HEAD_PITCH = 45, HEAD_YAW = 75;
    /** Degrees its head turns a tick at most: slow enough to watch it happen. */
    private static final float HEAD_SPEED = 2.5f;
    private static final Particle.DustOptions PRINT = new Particle.DustOptions(Color.fromRGB(28, 6, 6), 1.1f);

    private final MonsterRig rig;
    private final MonsterSpots spots;
    private final MonsterBody body;
    private final MonsterScript script;
    private final UUID playerId;
    private final MonsterScript.Settings settings;
    private final World world;
    private final MonsterSpace space;
    /** Where the player was last tick: the server's own velocity for a player is not reliable. */
    private Vector lastPlayer;
    private int ticks;
    private final Random random;
    /** The tick it next breathes on. */
    private int breath = 20;
    private final Feel feel;
    /** Its head's turn towards the player on top of the pose, in degrees. */
    private float headPitch, headYaw;
    /** Ticks its footprints still show for, once it has gone unseen; where they are. */
    private int trace;
    private final List<Location> prints = new ArrayList<>();
    /** What came before it, if anything: the lights it put out come back when the visit ends. */
    MonsterOmen omen;

    MonsterHaunt(MonsterRig rig, MonsterSpots spots, Player player, MonsterSpots.Spot spot,
                 MonsterScript.Settings settings, Feel feel, Random random, MonsterScript.Behavior forced) {
        this.rig = rig;
        this.feel = feel;
        this.spots = spots;
        this.body = new MonsterBody(rig);
        this.playerId = player.getUniqueId();
        this.settings = settings;
        this.random = random;
        this.world = player.getWorld();
        this.space = new MonsterSpots.WorldSpace(world);
        Vector3d anchor = vec(spot.anchor());
        this.script = new MonsterScript(rig, space, spot.pose(), anchor, spot.yaw(), settings, random, forced);
        body.spawn(world, spot.anchor(), spot.yaw(), spot.pose());
    }

    UUID playerId() {
        return playerId;
    }

    /** One tick; false once it is over. */
    boolean tick(Player player) {
        if (trace > 0) return tickTrace(player);
        if (script.phase() == Phase.GONE) {
            end();
            return false;
        }
        if (player == null || !player.isOnline() || player.isDead() || !player.getWorld().equals(world)
                || player.getLocation().toVector().distanceSquared(vec(script.anchor())) > 48 * 48 || !body.alive()) {
            end();
            return false;
        }
        ticks++;
        Location eye = player.getEyeLocation();
        Vector now = player.getLocation().toVector();
        double moved = lastPlayer == null ? 0 : now.distance(lastPlayer);
        lastPlayer = now;
        MonsterScript.Watcher watcher = new MonsterScript.Watcher(vec(now), vec(eye.toVector()), vec(eye.getDirection()),
                MonsterSpots.airborne(player), moved);
        boolean seenNow = script.phase() == Phase.LURK
                && spots.seen(player, script.pose(), vec(script.anchor()), script.yaw(), settings.viewAngle());
        MonsterScript.Senses senses = new MonsterScript.Senses() {
            @Override
            public boolean sees(Vector3d point, double angle) {
                return MonsterSpots.sees(player, vec(point), angle);
            }

            @Override
            public boolean open(Vector3d point) {
                return MonsterSpots.clearLine(player, vec(point));
            }
        };
        Frame frame = script.tick(watcher, seenNow, senses);

        play(player, frame.event());
        if (frame.phase() == Phase.GONE) {
            if (frame.event() == Event.LEFT && feel.traceTicks() > 0) {
                body.remove();
                leaveTrace(player, frame);
                return true;
            }
            end();
            return false;
        }
        breathe(player, frame);
        if (feel.darkness() && (frame.phase() == Phase.RISE || frame.phase() == Phase.MOVE) && ticks % 20 == 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false, false));
        }
        if (frame.face()) {
            body.face(vec(frame.anchor()), frame.yaw(), frame.facePitch(), frame.faceScale(), frame.pose());
            if (ticks % 4 == 0 && feel.scream().isEmpty()) {
                player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 0.5f);
            }
        } else if (frame.visible()) {
            body.show(vec(frame.anchor()), frame.yaw(), watching(frame, player), 2);
            if (ticks % 10 == 0) body.light(vec(frame.anchor()));
        } else {
            body.hide();
        }
        return true;
    }

    /** The sounds and effects that go with what just happened. */
    private void play(Player player, Event event) {
        switch (event) {
            case NOTICED -> player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.7f, 0.6f);
            case SCREAM -> {
                Location eye = player.getEyeLocation();
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, settings.screamTicks(), 3, false, false, false));
                if (!feel.scream().isEmpty()) {
                    player.playSound(eye, feel.scream(), SoundCategory.HOSTILE, 1f, 1f);
                    return;
                }
                player.playSound(eye, Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 0.5f);
                player.playSound(eye, Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1f, 0.6f);
                player.playSound(eye, Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.6f, 1.4f);
            }
            case BACK -> player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.5f);
            case VANISHED -> {
                Location face = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(MonsterScript.FACE_DISTANCE));
                world.spawnParticle(Particle.SQUID_INK, face, 30, 0.4, 0.4, 0.4, 0.02);
                world.spawnParticle(Particle.LARGE_SMOKE, face, 20, 0.5, 0.6, 0.5, 0.01);
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.7f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, false, false));
            }
            default -> {
            }
        }
    }

    /**
     * Its breathing, heard only by the player, from its head: a slow, deep breath every three seconds
     * or so while it waits or hides, a quick one every second once it comes for them. Nothing while
     * it screams.
     */
    private void breathe(Player player, Frame frame) {
        if (frame.face() || ticks < breath) return;
        boolean coming = frame.phase() == Phase.RISE || frame.phase() == Phase.MOVE;
        breath = ticks + (coming ? 16 + random.nextInt(8) : 50 + random.nextInt(30));
        Vector3f head = frame.visible()
                ? rig.eyes(frame.pose(), MonsterMotion.root(frame.anchor(), frame.yaw(), frame.pose()))
                : new Vector3f((float) frame.anchor().x, (float) frame.anchor().y + 1.6f, (float) frame.anchor().z);
        Location at = new Location(world, head.x, head.y, head.z);
        float pitch = (coming ? 0.75f : 0.5f) + random.nextFloat() * 0.1f;
        if (!feel.breath().isEmpty()) {
            player.playSound(at, feel.breath(), SoundCategory.HOSTILE, coming ? 1f : 0.7f, coming ? 1.2f : 1f);
            return;
        }
        player.playSound(at, Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, coming ? 1f : 0.7f, pitch);
        if (!coming && random.nextInt(3) == 0) {
            player.playSound(at, Sound.ENTITY_WARDEN_SNIFF, SoundCategory.HOSTILE, 0.35f, 0.6f);
        }
    }

    /**
     * The pose with its head turned a little further towards the player's eyes: worked out in the
     * head's own frame, a few degrees a tick, so it turns slowly and keeps following them.
     */
    private Pose watching(Frame frame, Player player) {
        Pose pose = frame.pose();
        int head = rig.partNames().indexOf("HEAD");
        if (head < 0) return pose;
        Pose turned = pose.plus(Map.of("HEAD", new Vector3f(headPitch, headYaw, 0)));
        Matrix4f root = MonsterMotion.root(frame.anchor(), frame.yaw(), turned);
        Vector3f eyes = rig.eyes(turned, root);
        Location target = player.getEyeLocation();
        Vector3f to = new Vector3f((float) target.getX() - eyes.x, (float) target.getY() - eyes.y,
                (float) target.getZ() - eyes.z);
        if (to.lengthSquared() < 1.0e-4f) return turned;
        Vector3f local = new Matrix4f(rig.parts(turned, root)[head]).invert().transformDirection(to);
        float yaw = (float) Math.toDegrees(Math.atan2(local.x, local.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(local.y, Math.hypot(local.x, local.z)));
        headYaw = clamp(headYaw + clamp(yaw, HEAD_SPEED), HEAD_YAW);
        headPitch = clamp(headPitch + clamp(pitch, HEAD_SPEED), HEAD_PITCH);
        return pose.plus(Map.of("HEAD", new Vector3f(headPitch, headYaw, 0)));
    }

    private static float clamp(float v, float limit) {
        return Math.max(-limit, Math.min(limit, v));
    }

    /**
     * Gone unseen: footprints from where it waited, leading away from the player, and a door near
     * there standing open. Only that player sees them, and the door is only open on their screen.
     */
    private void leaveTrace(Player player, Frame frame) {
        trace = feel.traceTicks();
        Vector3d from = frame.anchor();
        Double floor = MonsterSpace.floor(space, from.x, from.z, (int) Math.floor(from.y) + 1, (int) Math.floor(from.y) - 14);
        if (floor == null) return;
        Vector away = new Vector(from.x - player.getLocation().getX(), 0, from.z - player.getLocation().getZ());
        away = away.lengthSquared() < 1.0e-6 ? new Vector(0, 0, 1) : away.normalize();
        Vector side = new Vector(-away.getZ(), 0, away.getX());
        double y = floor;
        for (int i = 0; i < 7; i++) {
            Vector at = new Vector(from.x, 0, from.z).add(away.clone().multiply(i * 0.7))
                    .add(side.clone().multiply(i % 2 == 0 ? 0.15 : -0.15));
            Double step = MonsterSpace.floor(space, at.getX(), at.getZ(), (int) Math.floor(y) + 1, (int) Math.floor(y) - 2);
            if (step == null) break;
            y = step;
            prints.add(new Location(world, at.getX(), y + 0.05, at.getZ()));
        }
        openDoor(player, new Location(world, from.x, floor, from.z));
    }

    /** Its footprints, a puff of dust every half second; false once they have faded. */
    private boolean tickTrace(Player player) {
        if (--trace <= 0 || player == null || !player.isOnline() || !player.getWorld().equals(world)) {
            end();
            return false;
        }
        if (trace % 10 == 0) {
            for (Location print : prints) {
                player.spawnParticle(Particle.DUST, print, 2, 0.06, 0, 0.06, 0, PRINT);
            }
        }
        return true;
    }

    /** The nearest closed door, trapdoor or gate within four blocks of {@code near}, shown open to the player. */
    private void openDoor(Player player, Location near) {
        Block best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    Block b = near.getBlock().getRelative(dx, dy, dz);
                    if (!(b.getBlockData() instanceof Openable o) || o.isOpen()) continue;
                    if (o instanceof Door door && door.getHalf() == Bisected.Half.TOP) continue;
                    double d = b.getLocation().distanceSquared(near);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = b;
                    }
                }
            }
        }
        if (best == null) return;
        List<Block> blocks = best.getBlockData() instanceof Door ? List.of(best, best.getRelative(0, 1, 0)) : List.of(best);
        for (Block b : blocks) {
            BlockData data = b.getBlockData().clone();
            if (!(data instanceof Openable o)) continue;
            o.setOpen(true);
            player.sendBlockChange(b.getLocation(), data);
        }
    }

    /** Takes the body away at once, and puts back the lights its omen put out. */
    void end() {
        body.remove();
        if (omen != null) omen.end();
    }

    private static Vector3d vec(Vector v) {
        return new Vector3d(v.getX(), v.getY(), v.getZ());
    }

    private static Vector vec(Vector3d v) {
        return new Vector(v.x, v.y, v.z);
    }
}
