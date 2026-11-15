package com.Chagui68.entities.boss.rig;

import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * One look of a boss: its skeleton, its pieces, the moves it plays, and its weapon (the bone that
 * holds it and the stretch of it that cuts, from {@code weaponBase} to {@code weaponTip} in the
 * model's rest pose). A boss with two phases has two forms.
 *
 * <p>The moves named {@code idle}, {@code walk}, {@code run} and {@code dodge} are the ones
 * {@link RiggedBoss} plays between attacks; only {@code idle} is required.</p>
 */
public record RigForm(String key, String name, Rig rig, List<Piece> pieces, int[] owners, List<Rig.Move> moves,
                      int weaponBone, Vector3f weaponBase, Vector3f weaponTip, Color trail, Poser poser) {

    /** Where every piece is at a tick of a move; a model with cloth of its own overrides the rigid default. */
    public interface Poser {
        List<Transformation> pose(Rig.Move move, float tick, Matrix4f carry);
    }

    public static RigForm of(String key, String name, Rig rig, List<Piece> pieces, List<Rig.Move> moves,
                             String weaponBone, Vector3f weaponBase, Vector3f weaponTip, Color trail) {
        int[] owners = rig.owners(pieces);
        return new RigForm(key, name, rig, pieces, owners, List.copyOf(moves), rig.bone(weaponBone), weaponBase, weaponTip, trail,
                (move, tick, carry) -> rig.pose(move, tick, pieces, owners, carry));
    }

    public RigForm withPoser(Poser poser) {
        return new RigForm(key, name, rig, pieces, owners, moves, weaponBone, weaponBase, weaponTip, trail, poser);
    }

    /** The move called {@code id}, or null. */
    public Rig.Move move(String id) {
        for (Rig.Move move : moves) if (move.id().equals(id)) return move;
        return null;
    }

    public List<Transformation> pose(Rig.Move move, float tick, Matrix4f carry) {
        return poser.pose(move, tick, carry);
    }

    /** A point of the weapon at rest: 0 at its base, 1 at its tip. */
    public Vector3f weaponPoint(float along) {
        return new Vector3f(weaponBase).lerp(weaponTip, along);
    }

    /** The height of the body's middle, where the air round it hurts and its hits show. */
    public float middle() {
        return rig.bones().get(0).pivot().y;
    }

    /**
     * What {@code move} sounds and looks like at {@code t}, played every {@code step} ticks for displays
     * at {@code feet} facing {@code yaw} whose bones are {@code world}: the weapon's trail, the slashes
     * filling the air round the body, the shockwaves.
     */
    public void effects(Location feet, float yaw, Rig.Move move, float t, Matrix4f[] world, int step) {
        Location root = Rig.toWorld(feet, yaw, world[0].transformPosition(new Vector3f(0, .05f, 0)));
        for (int[] range : move.trails()) {
            if (t == range[0] || t == range[0] + 1) feet.getWorld().playSound(root, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.3f);
            if (t >= range[0] && t <= range[1]) {
                Vector3f tip = world[weaponBone].transformPosition(weaponPoint(1f));
                feet.getWorld().spawnParticle(Particle.DUST, Rig.toWorld(feet, yaw, tip), 4, .05, .05, .05, 0,
                        new Particle.DustOptions(trail, 1.1f));
            }
        }
        for (int[] range : move.slashes()) {
            if (t >= range[0] && t <= range[1]) {
                Location centre = Rig.toWorld(feet, yaw, world[0].transformPosition(new Vector3f(0, middle(), 0)));
                feet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, centre, 5, 1.4, 1.0, 1.4, 0);
                if (t % 4 == 0) feet.getWorld().playSound(centre, Sound.ENTITY_PLAYER_ATTACK_SWEEP, .7f, 1.6f);
            }
        }
        for (int at : move.rings()) {
            if (at > t - step && at <= t) {
                feet.getWorld().playSound(root, Sound.ENTITY_GENERIC_EXPLODE, .8f, 1.4f);
                feet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, root, 14, 1.6, .1, 1.6, 0);
            }
        }
    }

    /**
     * Shows the model in front of a viewer. With a move, side on so its travel crosses their view:
     * posed every two ticks with the game's own interpolation between, its effects heard and seen,
     * and removed a couple of seconds after it ends. Without one, facing them for {@code ticks}.
     */
    public void preview(Plugin plugin, Player viewer, Rig.Move move, long ticks) {
        Location eye = viewer.getLocation();
        double look = Math.toRadians(eye.getYaw());
        if (move == null) {
            RigBody body = RigBody.spawn(pieces, eye.clone().add(-Math.sin(look) * 4, 0, Math.cos(look) * 4), eye.getYaw() + 180f, "MSC_RigPreview");
            plugin.getServer().getScheduler().runTaskLater(plugin, body::remove, ticks);
            return;
        }
        float yaw = eye.getYaw() + 90f;
        double facing = Math.toRadians(yaw);
        // Eight blocks ahead, started back along its heading so the move plays out in front.
        Location feet = eye.clone().add(-Math.sin(look) * 8 + Math.sin(facing) * 3, 0, Math.cos(look) * 8 - Math.cos(facing) * 3);
        RigBody body = RigBody.spawn(pieces, feet, yaw, "MSC_RigPreview");
        int step = 2;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick += step;
                if (tick > move.ticks() + 40) {
                    body.remove();
                    cancel();
                    return;
                }
                float t = Math.min(tick, move.ticks());
                body.send(pose(move, t, new Matrix4f()), step);
                effects(feet, yaw, move, t, rig.bones(move, t), step);
            }
        }.runTaskTimer(plugin, 1L, step);
    }
}
