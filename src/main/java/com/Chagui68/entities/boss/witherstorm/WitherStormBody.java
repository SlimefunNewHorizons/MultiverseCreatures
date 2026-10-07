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
    private final BeamState[] beamStates = new BeamState[3];
    /** A hidden beam is not sent again: drawBeams asks every tick for every head. */
    private final boolean[] beamHidden = {true, true, true};

    /**
     * Where a head's beam is going and where it was a frame ago. The client slides a display's corner
     * along a straight line while it turns it, so a long segment swung through a big angle in one
     * three-tick slide drifts off its neighbours. The beam is therefore stepped here, every tick, along
     * the arc, and each step is only a one-tick slide.
     */
    private static final class BeamState {
        final Vector3f fromOrigin = new Vector3f(), toOrigin = new Vector3f();
        final Vector3f fromAim = new Vector3f(), toAim = new Vector3f();
        float fromLength, toLength, fromStart, toStart, fromEnd, toEnd;
        int step;
        Quaternionf last = new Quaternionf();
    }
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
     *
     * <p>Called every tick. {@code fresh} marks the frames on which the targets are new (the body's
     * own frames, {@code frameTicks} apart); in between, the beam moves on towards them one tick at a
     * time, the way the body slides, so the beam and the mouth it leaves stay together.</p>
     */
    void beam(int head, Vector3f origin, Vector3f direction, float length, float startWidth, float endWidth,
              float yaw, int frameTicks, boolean fresh) {
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

        beamHidden[head] = false;
        BeamState st = beamStates[head];
        Vector3f want = new Vector3f(direction).normalize();
        if (st == null) {
            st = beamStates[head] = new BeamState();
            st.fromOrigin.set(origin);
            st.fromAim.set(want);
            st.fromLength = length;
            st.fromStart = startWidth;
            st.fromEnd = endWidth;
            fresh = true;
            st.step = frameTicks;
        } else if (fresh) {
            float k = (float) st.step / frameTicks;
            // Where it stands now becomes where it starts from: no jump if the frame was cut short.
            st.fromOrigin.set(st.toOrigin).sub(st.fromOrigin).mul(k).add(st.fromOrigin);
            st.fromAim.lerp(st.toAim, k).normalize();
            st.fromLength += (st.toLength - st.fromLength) * k;
            st.fromStart += (st.toStart - st.fromStart) * k;
            st.fromEnd += (st.toEnd - st.fromEnd) * k;
            st.step = 0;
        }
        if (fresh) {
            st.toOrigin.set(origin);
            st.toAim.set(want);
            st.toLength = length;
            st.toStart = startWidth;
            st.toEnd = endWidth;
        }
        st.step = Math.min(frameTicks, st.step + 1);
        float k = (float) st.step / frameTicks;
        Vector3f o = new Vector3f(st.fromOrigin).lerp(st.toOrigin, k);
        Vector3f aim = new Vector3f(st.fromAim).lerp(st.toAim, k);
        if (aim.lengthSquared() < 1.0e-8f) aim.set(st.toAim);
        aim.normalize();
        float len = st.fromLength + (st.toLength - st.fromLength) * k;
        float w0 = st.fromStart + (st.toStart - st.fromStart) * k;
        float w1 = st.fromEnd + (st.toEnd - st.fromEnd) * k;

        // The heads face -z in the model, the one direction a shortest-arc rotation cannot turn to from +z
        // without picking a spin at random, so the facing is built from an up vector. Its sign follows the
        // last one sent: q and -q are the same turn, but the client would slide between them the long way.
        Quaternionf facing = facing(aim);
        if (facing.x * st.last.x + facing.y * st.last.y + facing.z * st.last.z + facing.w * st.last.w < 0) {
            facing.set(-facing.x, -facing.y, -facing.z, -facing.w);
        }
        st.last.set(facing);

        for (int i = 0; i < BEAM_PIECES; i++) {
            float from, to, width;
            if (i == BEAM_SEGMENTS) {
                from = 0;
                to = len;
                width = Math.max(0.08f, w0 * 0.6f);
            } else {
                from = len * i / BEAM_SEGMENTS;
                to = len * (i + 1) / BEAM_SEGMENTS;
                width = w0 + (w1 - w0) * (i + 0.5f) / BEAM_SEGMENTS;
            }
            Vector3f corner = facing.transform(new Vector3f(-width / 2, -width / 2, from)).add(o);
            BlockDisplay piece = pieces[i];
            if (piece == null || !piece.isValid()) continue;
            piece.setInterpolationDelay(0);
            piece.setInterpolationDuration(1);
            piece.setTransformation(new Transformation(corner, new Quaternionf(facing),
                    new Vector3f(width, width, Math.max(0.01f, to - from)), new Quaternionf()));
        }
    }

    /** A rotation taking +z to {@code direction} that keeps the cone's sides level, whatever the direction. */
    static Quaternionf facing(Vector3f direction) {
        Vector3f z = new Vector3f(direction).normalize();
        Vector3f x = new Vector3f(0, 1, 0).cross(z);
        if (x.lengthSquared() < 1.0e-6f) x.set(1, 0, 0);
        x.normalize();
        Vector3f y = new Vector3f(z).cross(x);
        return new Quaternionf().setFromNormalized(new org.joml.Matrix3f(x.x, x.y, x.z, y.x, y.y, y.z, z.x, z.y, z.z));
    }

    /** Puts a head's beam out; its displays stay, shrunk to nothing, for the next time. */
    void hideBeam(int head) {
        if (head < 0 || head >= beams.length || beams[head] == null || beamHidden[head]) return;
        beamHidden[head] = true;
        beamStates[head] = null;
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
