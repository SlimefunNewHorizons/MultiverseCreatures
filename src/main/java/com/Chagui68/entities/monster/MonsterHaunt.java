package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterScript.Event;
import com.Chagui68.entities.monster.MonsterScript.Frame;
import com.Chagui68.entities.monster.MonsterScript.Phase;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.joml.Vector3f;

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
 * faster once it comes for them. Only that player hears it.</p>
 */
final class MonsterHaunt {

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

    MonsterHaunt(MonsterRig rig, MonsterSpots spots, Player player, MonsterSpots.Spot spot,
                 MonsterScript.Settings settings, Random random, MonsterScript.Behavior forced) {
        this.rig = rig;
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
            end();
            return false;
        }
        breathe(player, frame);
        if (frame.face()) {
            body.face(vec(frame.anchor()), frame.yaw(), frame.facePitch(), frame.faceScale(), frame.pose());
            if (ticks % 4 == 0) {
                player.playSound(player.getEyeLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 0.5f);
            }
        } else if (frame.visible()) {
            body.show(vec(frame.anchor()), frame.yaw(), frame.pose(), 2);
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
                player.playSound(eye, Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 0.5f);
                player.playSound(eye, Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 1f, 0.6f);
                player.playSound(eye, Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.6f, 1.4f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, settings.screamTicks(), 3, false, false, false));
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
        player.playSound(at, Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, coming ? 1f : 0.7f, pitch);
        if (!coming && random.nextInt(3) == 0) {
            player.playSound(at, Sound.ENTITY_WARDEN_SNIFF, SoundCategory.HOSTILE, 0.35f, 0.6f);
        }
    }

    /** Takes the body away at once. */
    void end() {
        body.remove();
    }

    private static Vector3d vec(Vector v) {
        return new Vector3d(v.getX(), v.getY(), v.getZ());
    }

    private static Vector vec(Vector3d v) {
        return new Vector(v.x, v.y, v.z);
    }
}
