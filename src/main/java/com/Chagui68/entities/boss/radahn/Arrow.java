package com.Chagui68.entities.boss.radahn;

import com.Chagui68.entities.boss.rig.Hazard;
import com.Chagui68.entities.boss.rig.RiggedBoss;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An arrow of Radahn's greatbow: a rod that flies the way it was loosed, or, with a quarry, turns
 * after that player once it has flown {@code armed} ticks. It turns only a few degrees a tick, so a
 * sprint to the side shakes it off for a while, and it comes round again until it is spent. It
 * strikes the first player it touches and is gone; a wall stops it.
 */
final class Arrow implements Hazard {

    /** What an arrow does to the player it strikes. */
    interface Strike {
        void hit(Player player);
    }

    private final RiggedBoss boss;
    private final Location at;
    private final Vector velocity;
    private final Player quarry;
    private final int armed;
    private final double turn, radius;
    private final Strike strike;
    private final Particle.DustOptions dust;
    private final BlockDisplay rod;
    private int age, left;

    /**
     * @param velocity blocks a tick, and the way it is loosed
     * @param quarry   the player it turns after, or null to fly straight
     * @param degrees  how far it can turn in a tick
     * @param life     ticks before it is spent
     * @param radius   how near a player it must pass to strike
     * @param size     1 for an arrow as long as a player is tall
     */
    Arrow(RiggedBoss boss, Location from, Vector velocity, Player quarry, int armed, double degrees, int life, double radius,
          float size, Color color, Strike strike) {
        this.boss = boss;
        this.at = from.clone();
        this.velocity = velocity.clone();
        this.quarry = quarry;
        this.armed = armed;
        this.turn = Math.toRadians(degrees);
        this.left = life;
        this.radius = radius;
        this.strike = strike;
        this.dust = new Particle.DustOptions(color, .9f);
        at.setDirection(velocity);
        // A lightning rod laid along its flight, head first: the block's own shape is the arrow. The
        // display looks where the arrow flies, so only its place and its facing are ever sent.
        float thick = .9f * size, length = 1.6f * size;
        rod = at.getWorld().spawn(at, BlockDisplay.class, d -> {
            d.setBlock(Material.LIGHTNING_ROD.createBlockData());
            d.setPersistent(false);
            d.setTeleportDuration(1);
            d.setTransformation(new Transformation(new Vector3f(-thick / 2, thick / 2, -length / 2),
                    new Quaternionf().rotateX((float) (Math.PI / 2)), new Vector3f(thick, length, thick), new Quaternionf()));
            d.addScoreboardTag(boss.bodyTag());
        });
    }

    @Override
    public boolean tick() {
        age++;
        if (quarry != null && age > armed && quarry.isOnline() && !quarry.isDead() && quarry.getWorld().equals(at.getWorld())) {
            steer(velocity, quarry.getLocation().add(0, 1, 0).toVector().subtract(at.toVector()), turn);
        }
        if (--left < 0) return false;
        // In steps shorter than a player is wide, so a fast arrow cannot pass through one between two ticks.
        int steps = (int) Math.max(1, Math.ceil(velocity.length() / .5));
        Vector step = velocity.clone().multiply(1.0 / steps);
        for (int k = 0; k < steps; k++) {
            at.add(step);
            if (!at.isChunkLoaded() || at.getBlock().getType().isSolid()) return false;
            for (Player p : boss.playersNear(at, radius + 2)) {
                if (!p.getBoundingBox().expand(radius).contains(at.toVector())) continue;
                strike.hit(p);
                return false;
            }
        }
        at.setDirection(velocity);
        rod.teleport(at);
        at.getWorld().spawnParticle(Particle.DUST, at, 2, .04, .04, .04, 0, dust);
        return true;
    }

    @Override
    public void end() {
        rod.remove();
    }

    /** Turns {@code velocity} towards {@code to} by at most {@code max} radians, keeping its speed. */
    static void steer(Vector velocity, Vector to, double max) {
        double speed = velocity.length();
        if (speed < 1e-6 || to.lengthSquared() < 1e-9) return;
        Vector have = velocity.clone().multiply(1 / speed), want = to.clone().normalize();
        double angle = Math.acos(Math.max(-1, Math.min(1, have.dot(want))));
        if (angle <= max) {
            velocity.copy(want.multiply(speed));
            return;
        }
        double sin = Math.sin(angle);
        if (sin < 1e-6) {
            // Dead astern there is no one way round: tip it up, and the next tick has one.
            velocity.setY(velocity.getY() + speed * .05);
            velocity.multiply(speed / velocity.length());
            return;
        }
        double by = max / angle;
        velocity.copy(have.multiply(Math.sin((1 - by) * angle) / sin).add(want.multiply(Math.sin(by * angle) / sin)).multiply(speed));
    }
}
