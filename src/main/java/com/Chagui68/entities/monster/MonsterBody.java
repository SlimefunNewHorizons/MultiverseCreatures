package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Pose;
import com.Chagui68.utils.DisplaySuit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Monster in the world: one item display per head of {@link MonsterRig}, every one given the
 * full matrix the rig computes for it, so the body keeps the shape it was built with.
 *
 * <p>All the displays stand on one point {@link #BELOW} blocks under the anchor, and that offset is
 * put back in their matrices. A display is only drawn while its box is on screen, and the box grows
 * up from where the display stands: from down there, one box covers a body standing on the anchor
 * and one hanging from a ceiling alike. Since that point may be inside the floor, the displays are
 * lit as the anchor is, not as the block they stand in.</p>
 *
 * <p>Every display is non persistent and tagged {@link #TAG}: a crash or a restart leaves nothing
 * behind, and {@code /msc kill} finds them.</p>
 */
final class MonsterBody {

    static final String TAG = "MSC_Monster";
    /** How far under the anchor the displays stand. */
    static final float BELOW = 3.5f;
    /** The box each display is culled by: wide and tall enough for every pose. */
    private static final float BOX = 8f;

    /** One head item per skin, built once: a skin is decoded and resolved only the first time. */
    private static final Map<String, ItemStack> HEADS = new HashMap<>();

    private final MonsterRig rig;
    private final List<ItemDisplay> parts = new ArrayList<>();
    private World world;
    private Vector origin;

    MonsterBody(MonsterRig rig) {
        this.rig = rig;
    }

    /** Builds the body at {@code anchor}, turned by {@code yaw} radians, in {@code pose}. */
    void spawn(World world, Vector anchor, float yaw, Pose pose) {
        remove();
        this.world = world;
        this.origin = new Vector(anchor.getX(), anchor.getY() - BELOW, anchor.getZ());
        Matrix4f[] heads = rig.heads(pose, root(anchor, yaw, pose));
        Location at = origin.toLocation(world, 0f, 0f);
        Display.Brightness light = lightAt(world, anchor);
        for (int i = 0; i < rig.size(); i++) {
            ItemStack head = HEADS.computeIfAbsent(rig.textures().get(i),
                    texture -> DisplaySuit.head("MSC_Monster", texture, "Monster"));
            Matrix4f matrix = heads[i];
            parts.add(world.spawn(at, ItemDisplay.class, display -> {
                display.setPersistent(false);
                display.addScoreboardTag(TAG);
                display.setItemStack(head);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                display.setBillboard(Display.Billboard.FIXED);
                display.setBrightness(light);
                display.setShadowRadius(0f);
                display.setDisplayWidth(BOX);
                display.setDisplayHeight(BOX);
                display.setTeleportDuration(2);
                display.setTransformationMatrix(matrix);
            }));
        }
    }

    /**
     * Shows {@code pose} at {@code anchor}, turned by {@code yaw} radians, the client sliding there
     * over {@code ticks}.
     */
    void show(Vector anchor, float yaw, Pose pose, int ticks) {
        if (world == null) return;
        Vector wanted = new Vector(anchor.getX(), anchor.getY() - BELOW, anchor.getZ());
        if (wanted.distanceSquared(origin) > 1.0e-6) {
            origin = wanted;
            Location at = origin.toLocation(world, 0f, 0f);
            for (ItemDisplay part : parts) {
                if (part.isValid()) part.teleport(at);
            }
        }
        Matrix4f[] heads = rig.heads(pose, root(anchor, yaw, pose));
        for (int i = 0; i < parts.size(); i++) {
            ItemDisplay part = parts.get(i);
            if (!part.isValid()) continue;
            part.setInterpolationDelay(0);
            part.setInterpolationDuration(ticks);
            part.setTransformationMatrix(heads[i]);
        }
    }

    /** Folds every head down to nothing where it is: the body is gone from sight until it is shown again. */
    void hide() {
        Matrix4f none = new Matrix4f().scaling(0f);
        for (ItemDisplay part : parts) {
            if (!part.isValid()) continue;
            part.setInterpolationDelay(0);
            part.setInterpolationDuration(0);
            part.setTransformationMatrix(none);
        }
    }

    /** Lights the body as the block at {@code anchor} is lit. */
    void light(Vector anchor) {
        if (world == null) return;
        Display.Brightness light = lightAt(world, anchor);
        for (ItemDisplay part : parts) {
            if (part.isValid()) part.setBrightness(light);
        }
    }

    /** The matrix that carries the rig from the displays' point to the anchor, turned and posed. */
    private Matrix4f root(Vector anchor, float yaw, Pose pose) {
        return new Matrix4f()
                .translation((float) (anchor.getX() - origin.getX()), (float) (anchor.getY() - origin.getY()),
                        (float) (anchor.getZ() - origin.getZ()))
                .rotateY(yaw)
                .mul(MonsterRig.body(pose));
    }

    /**
     * The light of the air at the anchor. An anchor on a ceiling is the underside of a solid block,
     * which holds no light of its own, so the block under it is read instead.
     */
    private static Display.Brightness lightAt(World world, Vector anchor) {
        Block block = world.getBlockAt(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
        for (int step = 0; step < 2 && block.getType().isOccluding(); step++) {
            block = block.getRelative(0, -1, 0);
        }
        return new Display.Brightness(block.getLightFromBlocks(), block.getLightFromSky());
    }

    /** True while every head of the body is in the world. */
    boolean alive() {
        if (parts.isEmpty()) return false;
        for (ItemDisplay part : parts) {
            if (!part.isValid()) return false;
        }
        return true;
    }

    void remove() {
        for (ItemDisplay part : parts) {
            if (part.isValid()) part.remove();
        }
        parts.clear();
    }
}
