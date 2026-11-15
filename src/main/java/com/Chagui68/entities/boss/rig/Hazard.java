package com.Chagui68.entities.boss.rig;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A timed effect of a boss: a mine, a falling rock, a brand on a player. Its boss ticks it from its
 * own loop and ends it when the fight does, so no effect outlives its boss or needs a task of its own.
 */
public interface Hazard {

    /** One tick; false once the effect is over. */
    boolean tick();

    /** Takes down whatever the effect put in the world. */
    default void end() {
    }

    /**
     * A warned burst: a ring on the ground for {@code fuse} ticks, then everyone inside it is hurt and
     * thrown. It may follow a player (a brand), and may show a block that sits there or falls from
     * {@code dropFrom} blocks up (a keg, a meteor): one display, moved by one packet.
     */
    final class Blast implements Hazard {
        private final RiggedBoss boss;
        private final RiggedBoss.Instance inst;
        private final Location center;
        private final Player follow;
        private final int fuse;
        private final double radius, damage, launch;
        private final Particle.DustOptions dust;
        private BlockDisplay prop;
        private int age;

        public Blast(RiggedBoss boss, RiggedBoss.Instance inst, Location center, Player follow, int fuse, double radius,
                     double damage, double launch, Color color, Material block, double dropFrom) {
            this.boss = boss;
            this.inst = inst;
            this.center = center.clone();
            this.follow = follow;
            this.fuse = fuse;
            this.radius = radius;
            this.damage = damage;
            this.launch = launch;
            this.dust = new Particle.DustOptions(color, 1.2f);
            if (block != null) {
                float size = (float) Math.min(1.2, .35 + radius * .15);
                Vector3f rest = new Vector3f(-size / 2, (float) dropFrom, -size / 2);
                prop = center.getWorld().spawn(center, BlockDisplay.class, d -> {
                    d.setBlock(block.createBlockData());
                    d.setPersistent(false);
                    d.setTransformation(new Transformation(rest, new Quaternionf(), new Vector3f(size), new Quaternionf()));
                    d.addScoreboardTag(boss.bodyTag());
                });
            }
        }

        @Override
        public boolean tick() {
            age++;
            if (follow != null) {
                if (!follow.isOnline() || follow.isDead() || !follow.getWorld().equals(center.getWorld())) return false;
                center.setX(follow.getX());
                center.setY(follow.getY());
                center.setZ(follow.getZ());
                if (prop != null) prop.teleport(center);
            }
            if (age == 2 && prop != null && follow == null) {
                // The whole fall in one packet: the game slides the block down over the fuse.
                Transformation at = prop.getTransformation();
                prop.setInterpolationDelay(0);
                prop.setInterpolationDuration(Math.max(1, fuse - 2));
                prop.setTransformation(new Transformation(new Vector3f(at.getTranslation().x, 0, at.getTranslation().z),
                        at.getLeftRotation(), at.getScale(), at.getRightRotation()));
            }
            if (age < fuse) {
                // The ring closes in as the fuse burns, quicker at the end.
                if (age % (age > fuse * .6 ? 2 : 4) == 0) {
                    int points = (int) Math.max(8, Math.min(20, radius * 6));
                    for (int i = 0; i < points; i++) {
                        double a = Math.PI * 2 * i / points + age * .15;
                        center.getWorld().spawnParticle(Particle.DUST, center.getX() + Math.cos(a) * radius, center.getY() + .15,
                                center.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0, dust);
                    }
                }
                return true;
            }
            center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, .6, 0), (int) Math.max(1, radius), radius * .35, .3, radius * .35, 0);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, .8, 0), 24, radius * .5, .6, radius * .5, 0, dust);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, .8f + (float) Math.min(.8, 2 / radius));
            for (Player p : boss.playersNear(center, radius + 1)) {
                Location l = p.getLocation();
                if (Math.abs(l.getY() - center.getY()) > 3 || Math.hypot(l.getX() - center.getX(), l.getZ() - center.getZ()) > radius) continue;
                boss.hurt(inst, p, damage);
                if (launch > 0) boss.launch(p, launch);
            }
            return false;
        }

        @Override
        public void end() {
            if (prop != null) prop.remove();
            prop = null;
        }
    }

}
