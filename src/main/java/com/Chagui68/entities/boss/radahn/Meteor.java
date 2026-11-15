package com.Chagui68.entities.boss.radahn;

import com.Chagui68.entities.boss.rig.Hazard;
import com.Chagui68.entities.boss.rig.RiggedBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The star Radahn comes back down as. It is seen coming for the whole of its fall: a ball of burning
 * stone that slides down the sky from far off, a ring burnt on the ground round where it will land,
 * and a warning on the screen of everyone still inside that ring. It hurts nobody itself: its boss
 * lands the blow on the tick its own move comes down, which is the tick this ends on.
 */
final class Meteor implements Hazard {

    /** Where the star is first seen, from the place it lands: far off and high. */
    private static final Vector3f FAR = new Vector3f(74, 96, -52);
    /** The ball and the stones that fly with it: its block, its size, and where it rides off the ball's middle. */
    private static final Material[] STONE = {Material.MAGMA_BLOCK, Material.BLACKSTONE, Material.CRYING_OBSIDIAN, Material.MAGMA_BLOCK, Material.OBSIDIAN};
    private static final float[] SIZE = {9f, 4f, 3.4f, 3f, 3.6f};
    private static final Vector3f[] RIDES = {new Vector3f(), new Vector3f(4.6f, 1.5f, 1f), new Vector3f(-3.8f, 2.6f, -2.4f),
            new Vector3f(1f, -3.2f, 4.2f), new Vector3f(-1.6f, 3.4f, 3.6f)};
    private static final Particle.DustOptions RING = new Particle.DustOptions(Color.fromRGB(255, 40, 30), 2.2f);

    private final RiggedBoss boss;
    private final Location center;
    private final double radius;
    private final int fuse;
    private final List<BlockDisplay> stones = new ArrayList<>();
    private int age;

    /** A star that lands on {@code center} in {@code fuse} ticks; the ring round it is {@code radius} blocks. */
    Meteor(RiggedBoss boss, Location center, double radius, int fuse) {
        this.boss = boss;
        this.center = center.clone();
        this.radius = radius;
        this.fuse = fuse;
        for (int k = 0; k < STONE.length; k++) {
            Material block = STONE[k];
            Transformation far = placed(k, FAR, 0);
            stones.add(center.getWorld().spawn(center, BlockDisplay.class, d -> {
                d.setBlock(block.createBlockData());
                d.setPersistent(false);
                // Seen from as far as it starts, and lit like the fire it is whatever the hour.
                d.setViewRange(4f);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setTransformation(far);
                d.addScoreboardTag(boss.bodyTag());
            }));
        }
    }

    /** Stone {@code k} with the ball's middle at {@code at} from the centre, tumbled {@code turns} of the way round. */
    private static Transformation placed(int k, Vector3f at, float turns) {
        Quaternionf tumble = new Quaternionf().rotateXYZ(.6f * k + turns * 5f, .9f * k + turns * 3f, .4f * k);
        Vector3f corner = new Vector3f(at).add(RIDES[k]).sub(tumble.transform(new Vector3f(SIZE[k] / 2)));
        return new Transformation(corner, tumble, new Vector3f(SIZE[k]), new Quaternionf());
    }

    @Override
    public boolean tick() {
        age++;
        World world = center.getWorld();
        if (age == 2) {
            // The whole fall in one packet each: the game slides the stones down the sky over the fuse.
            for (int k = 0; k < stones.size(); k++) {
                BlockDisplay stone = stones.get(k);
                stone.setInterpolationDelay(0);
                stone.setInterpolationDuration(Math.max(1, fuse - 2));
                stone.setTransformation(placed(k, new Vector3f(0, SIZE[0] / 2 - 1, 0), 1));
            }
            world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 4f, .5f);
        }
        int left = fuse - age;
        // The ring on the ground, quicker as the star nears.
        if (age % (left < 40 ? 2 : 5) == 0) {
            int points = (int) Math.max(24, radius * 4.5);
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points + age * .02;
                world.spawnParticle(Particle.DUST, center.getX() + Math.cos(a) * radius, center.getY() + .25, center.getZ() + Math.sin(a) * radius,
                        1, 0, .1, 0, 0, RING);
            }
        }
        // Its tail, where it is now on its way down: seen from any distance.
        float fallen = Math.max(0, Math.min(1, (age - 2) / (float) Math.max(1, fuse - 2)));
        Location star = center.clone().add(FAR.x * (1 - fallen), FAR.y * (1 - fallen) + SIZE[0] / 2, FAR.z * (1 - fallen));
        world.spawnParticle(Particle.FLAME, star, 14, 2.5, 2.5, 2.5, .08, null, true);
        world.spawnParticle(Particle.LARGE_SMOKE, star, 6, 2.5, 2.5, 2.5, .04, null, true);
        if (age % 10 == 1) {
            for (Player p : boss.playersNear(center, radius + 48)) {
                double out = radius - Math.hypot(p.getX() - center.getX(), p.getZ() - center.getZ());
                if (out >= 0) warn(p, out, left);
            }
        }
        return age < fuse;
    }

    /** Tells a player still inside the ring to leave it, and how far the edge is. */
    private static void warn(Player p, double out, int left) {
        p.showTitle(Title.title(Component.text("A STAR IS FALLING", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Get out of the red ring", NamedTextColor.GOLD),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(200))));
        p.sendActionBar(Component.text((int) Math.ceil(out) + " blocks to the edge  -  " + (left + 19) / 20 + "s", NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, left < 60 ? 1.8f : 1.2f);
    }

    @Override
    public void end() {
        stones.forEach(BlockDisplay::remove);
        stones.clear();
    }
}
