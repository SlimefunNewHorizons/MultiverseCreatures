package com.Chagui68.entities.boss.witherstorm;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The displays one form of the storm is drawn with: a block display for every box of its model, an
 * item display holding a wither skull for each of the old wither heads, and the translucent purple
 * cones of its tractor beams.
 *
 * <p>Every display rides the storm's anchor. A passenger is carried by its vehicle on the client, so
 * moving the whole body costs the server one teleport of the anchor; only the boxes that moved more
 * than the eye can tell in a frame are sent again, and the client slides them there over the frame.
 * Turning the body is the one thing that touches every display, so it is done in steps of a few
 * degrees that the client smooths over {@link #TURN_TICKS}. Nothing here is persistent: a restart
 * loses the body and the boss builds it again round its saved anchor.</p>
 */
final class WitherStormBody {

    /** Tag on every display of a storm's body. */
    static final String PART_TAG = "MSC_WitherStormPart";
    /** Ticks the client takes to turn the body to a new yaw; the storm turns it no more often. */
    static final int TURN_TICKS = 10;

    /** A box closer than 3 cm and 1 degree to where it was is not sent again. */
    private static final float MOVED = 0.0009f;
    private static final float TURNED = 4.0e-5f;
    /** Segments of a beam's cone, from the mouth out, and the bright core down its middle. */
    private static final int BEAM_SEGMENTS = 3;
    private static final int BEAM_PIECES = BEAM_SEGMENTS + 1;

    private final WitherStormModel model;
    private final Display[] displays;
    private final WitherStormModel.Placement[] sent;
    private final BlockDisplay[][] beams = new BlockDisplay[3][];
    private ArmorStand anchor;
    private String ownerTag;
    private float viewRange;

    WitherStormBody(WitherStormModel model) {
        this.model = model;
        this.displays = new Display[model.boxes().size()];
        this.sent = new WitherStormModel.Placement[displays.length];
    }

    WitherStormModel model() {
        return model;
    }

    /** Builds the body round the anchor, every box at its first placement. */
    void spawn(ArmorStand anchor, WitherStormModel.Placement[] placements, String ownerTag, float viewRange) {
        this.anchor = anchor;
        this.ownerTag = ownerTag;
        this.viewRange = viewRange;
        Location at = anchor.getLocation();
        World world = at.getWorld();
        for (int i = 0; i < displays.length; i++) {
            WitherStormModel.Placement placement = placements[i];
            if (placement == null) {
                continue;
            }
            WitherStormModel.Box box = model.boxes().get(i);
            Display display = box.skull()
                    ? world.spawn(at, ItemDisplay.class, d -> {
                        d.setItemStack(new ItemStack(Material.WITHER_SKELETON_SKULL));
                        d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                        configure(d, box.glow(), transformation(placement));
                    })
                    : world.spawn(at, BlockDisplay.class, d -> {
                        d.setBlock(block(box.material()).createBlockData());
                        configure(d, box.glow(), transformation(placement));
                    });
            displays[i] = display;
            sent[i] = placement;
            anchor.addPassenger(display);
        }
    }

    private void configure(Display display, boolean glow, Transformation transformation) {
        display.setPersistent(false);
        display.addScoreboardTag(PART_TAG);
        display.addScoreboardTag(ownerTag);
        display.setTransformation(transformation);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(TURN_TICKS);
        // A display with no size is never culled: the anchor is the only thing the client sees moving,
        // and the far boxes of a hundred-block body must not vanish because the anchor left the screen.
        display.setDisplayWidth(0f);
        display.setDisplayHeight(0f);
        display.setViewRange(viewRange);
        display.setShadowRadius(0f);
        if (glow) {
            display.setBrightness(new Display.Brightness(15, 15));
        }
    }

    static Material block(String name) {
        Material material = Material.matchMaterial(name);
        return material != null && material.isBlock() ? material : Material.BLACK_CONCRETE;
    }

    static Transformation transformation(WitherStormModel.Placement p) {
        return new Transformation(new Vector3f(p.translation()), new Quaternionf(p.rotation()),
                new Vector3f(p.scale()), new Quaternionf());
    }

    /**
     * Sends the boxes that moved since the last frame, sliding there over {@code ticks}.
     *
     * @return how many displays were updated
     */
    int update(WitherStormModel.Placement[] placements, int ticks) {
        int updated = 0;
        for (int i = 0; i < displays.length; i++) {
            Display display = displays[i];
            WitherStormModel.Placement placement = placements[i];
            if (display == null || placement == null || !display.isValid() || same(sent[i], placement)) {
                continue;
            }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(ticks);
            display.setTransformation(transformation(placement));
            sent[i] = placement;
            updated++;
        }
        return updated;
    }

    /** Turns every display to the storm's yaw; the client turns them over {@link #TURN_TICKS}. */
    void turn(float yaw) {
        for (Display display : displays) {
            if (display != null && display.isValid()) {
                display.setRotation(yaw, 0f);
            }
        }
        for (BlockDisplay[] beam : beams) {
            if (beam == null) continue;
            for (BlockDisplay piece : beam) {
                if (piece != null && piece.isValid()) piece.setRotation(yaw, 0f);
            }
        }
    }

    // ------------------------------------------------------------------ tractor beams

    /**
     * Draws a head's tractor beam: a cone of purple glass from the mouth out along {@code direction},
     * {@code length} long and {@code endWidth} wide at its far end, with a brighter core. Points are in
     * the display space (before the yaw), like the boxes.
     */
    void beam(int head, Vector3f origin, Vector3f direction, float length, float startWidth, float endWidth,
              float yaw, int ticks) {
        if (head < 0 || head >= beams.length || anchor == null || !anchor.isValid()) return;
        BlockDisplay[] pieces = beams[head];
        if (pieces == null) {
            pieces = new BlockDisplay[BEAM_PIECES];
            Location at = anchor.getLocation();
            for (int i = 0; i < BEAM_PIECES; i++) {
                Material glass = i == BEAM_SEGMENTS ? Material.MAGENTA_STAINED_GLASS : Material.PURPLE_STAINED_GLASS;
                pieces[i] = at.getWorld().spawn(at, BlockDisplay.class, d -> {
                    d.setBlock(glass.createBlockData());
                    configure(d, true, hidden());
                    d.setRotation(yaw, 0f);
                });
                anchor.addPassenger(pieces[i]);
            }
            beams[head] = pieces;
        }
        Quaternionf facing = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f(direction).normalize());
        for (int i = 0; i < BEAM_PIECES; i++) {
            float from, to, width;
            if (i == BEAM_SEGMENTS) {
                from = 0;
                to = length;
                width = Math.max(0.08f, startWidth * 0.6f);
            } else {
                from = length * i / BEAM_SEGMENTS;
                to = length * (i + 1) / BEAM_SEGMENTS;
                width = startWidth + (endWidth - startWidth) * (i + 0.5f) / BEAM_SEGMENTS;
            }
            Vector3f corner = facing.transform(new Vector3f(-width / 2, -width / 2, from)).add(origin);
            BlockDisplay piece = pieces[i];
            if (piece == null || !piece.isValid()) continue;
            piece.setInterpolationDelay(0);
            piece.setInterpolationDuration(ticks);
            piece.setTransformation(new Transformation(corner, new Quaternionf(facing),
                    new Vector3f(width, width, Math.max(0.01f, to - from)), new Quaternionf()));
        }
    }

    /** Puts a head's beam out; its displays stay, shrunk to nothing, for the next time. */
    void hideBeam(int head) {
        if (head < 0 || head >= beams.length || beams[head] == null) return;
        for (BlockDisplay piece : beams[head]) {
            if (piece == null || !piece.isValid()) continue;
            piece.setInterpolationDelay(0);
            piece.setInterpolationDuration(3);
            piece.setTransformation(hidden());
        }
    }

    private static Transformation hidden() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.001f), new Quaternionf());
    }

    // ------------------------------------------------------------------ the rest

    /** Shrinks every box to nothing where it stands, over {@code ticks}: the old body of an evolution. */
    void shrink(int ticks) {
        for (int i = 0; i < displays.length; i++) {
            Display display = displays[i];
            WitherStormModel.Placement p = sent[i];
            if (display == null || p == null || !display.isValid()) continue;
            Vector3f centre = new Vector3f(p.scale()).mul(0.5f);
            new Quaternionf(p.rotation()).transform(centre).add(p.translation());
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(ticks);
            display.setTransformation(new Transformation(centre, new Quaternionf(p.rotation()),
                    new Vector3f(0.001f), new Quaternionf()));
        }
        for (int head = 0; head < beams.length; head++) hideBeam(head);
    }

    /** The displays still standing, for a collapse. */
    List<Display> standing() {
        List<Display> out = new ArrayList<>();
        for (Display display : displays) {
            if (display != null && display.isValid()) out.add(display);
        }
        return out;
    }

    /** Where a display's box is drawn, relative to the anchor before its yaw. */
    Vector3f centreOf(Display display) {
        for (int i = 0; i < displays.length; i++) {
            if (displays[i] == display && sent[i] != null) {
                WitherStormModel.Placement p = sent[i];
                return new Quaternionf(p.rotation()).transform(new Vector3f(p.scale()).mul(0.5f)).add(p.translation());
            }
        }
        return new Vector3f();
    }

    void remove() {
        for (int i = 0; i < displays.length; i++) {
            if (displays[i] != null && displays[i].isValid()) {
                displays[i].remove();
            }
            displays[i] = null;
        }
        for (int head = 0; head < beams.length; head++) {
            if (beams[head] == null) continue;
            for (BlockDisplay piece : beams[head]) {
                if (piece != null && piece.isValid()) piece.remove();
            }
            beams[head] = null;
        }
    }

    static boolean same(WitherStormModel.Placement a, WitherStormModel.Placement b) {
        if (a == null) return false;
        Quaternionf p = a.rotation(), q = b.rotation();
        // Written out rather than Quaternionf.dot: its signature changed between JOML releases, and a
        // call compiled against one fails on a server that ships the other.
        float dot = p.x * q.x + p.y * q.y + p.z * q.z + p.w * q.w;
        return a.translation().distanceSquared(b.translation()) < MOVED
                && a.scale().distanceSquared(b.scale()) < MOVED
                && Math.abs(dot) > 1 - TURNED;
    }
}
