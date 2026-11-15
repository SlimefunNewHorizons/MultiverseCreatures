package com.Chagui68.entities.boss.rig;

import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.DisplaySuit;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A model's block displays in the world.
 *
 * <p>They are never saved, so a crash or a chunk unload leaves no body behind. A pose is sent piece
 * by piece, and a piece that has not moved since the last pose is not sent at all: a boss standing
 * still, or swinging one arm, costs the players' connections only what actually changed.</p>
 *
 * <p>A piece with a skin is drawn as a textured player head filling the piece's box.</p>
 */
public final class RigBody {

    /** How close two transformations are to count as the same: under a hundredth of a pixel. */
    private static final float EPSILON = 1e-4f;

    private final List<Display> displays;
    private final boolean[] heads;
    private final Transformation[] sent;

    private RigBody(List<Display> displays, boolean[] heads) {
        this.displays = displays;
        this.heads = heads;
        this.sent = new Transformation[displays.size()];
    }

    /** Spawns {@code pieces} at {@code feet}, facing {@code yaw}, every display tagged {@code tag}. */
    public static RigBody spawn(List<Piece> pieces, Location feet, float yaw, String tag) {
        Location at = feet.clone();
        at.setYaw(yaw);
        at.setPitch(0f);
        List<Display> out = new ArrayList<>(pieces.size());
        boolean[] heads = new boolean[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            heads[i] = piece.skin() != null;
            if (heads[i]) {
                out.add(at.getWorld().spawn(at, ItemDisplay.class, display -> {
                    display.setItemStack(DisplaySuit.head("msc_rig", piece.skin(), tag));
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    display.setTransformation(head(piece.transformation()));
                    display.setPersistent(false);
                    display.addScoreboardTag(tag);
                }));
                continue;
            }
            out.add(at.getWorld().spawn(at, BlockDisplay.class, display -> {
                display.setBlock(piece.material().createBlockData());
                display.setTransformation(piece.transformation());
                display.setPersistent(false);
                if (piece.glow()) display.setBrightness(new Display.Brightness(15, 15));
                display.addScoreboardTag(tag);
            }));
        }
        return new RigBody(out, heads);
    }

    /**
     * The item display transformation that draws a player head over a block display's box: the head
     * item is a cube half its scale wide hanging below its anchor, so the anchor is the middle of the
     * box's top face and the scale is twice the box.
     */
    static Transformation head(Transformation box) {
        Quaternionf rotation = box.getLeftRotation();
        Vector3f size = box.getScale();
        Vector3f anchor = rotation.transform(new Vector3f(size.x * .5f, size.y, size.z * .5f)).add(box.getTranslation());
        return new Transformation(anchor, new Quaternionf(rotation), new Vector3f(size).mul(2f), new Quaternionf());
    }

    /** Whether the displays are still in the world: a chunk unload takes them. */
    public boolean valid() {
        return !displays.isEmpty() && displays.get(0).isValid();
    }

    /** Sends every piece that moved its pose, reached over {@code ticks}. */
    public void send(List<Transformation> posed, int ticks) {
        for (int i = 0; i < displays.size(); i++) {
            Transformation to = heads[i] ? head(posed.get(i)) : posed.get(i);
            if (sent[i] != null && same(sent[i], to)) continue;
            sent[i] = to;
            Display display = displays.get(i);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(ticks);
            display.setTransformation(to);
        }
    }

    /** Takes the body out of sight, or brings it back: a display seen from no distance at all is not drawn. */
    public void hide(boolean hidden) {
        for (Display display : displays) display.setViewRange(hidden ? 0f : 1f);
    }

    /** Moves every display to {@code anchor}; the next pose is sent whole. */
    public void moveTo(Location anchor) {
        for (Display display : displays) display.teleport(anchor);
        java.util.Arrays.fill(sent, null);
    }

    public void remove() {
        displays.forEach(Display::remove);
        displays.clear();
    }

    static boolean same(Transformation a, Transformation b) {
        return a.getTranslation().equals(b.getTranslation(), EPSILON) && a.getScale().equals(b.getScale(), EPSILON)
                && a.getLeftRotation().equals(b.getLeftRotation(), EPSILON);
    }
}
