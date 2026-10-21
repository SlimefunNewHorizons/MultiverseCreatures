package com.Chagui68.entities.boss;

import com.Chagui68.utils.DisplaySculpt;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaleniaModelTest {

    /** min x, y, z, max x, y, z of the drawn pieces, in blocks. */
    static float[] bounds(List<DisplaySculpt.Piece> pieces) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (DisplaySculpt.Piece piece : pieces) {
            Transformation t = piece.transformation();
            Matrix4f m = new Matrix4f().translate(t.getTranslation()).rotate(t.getLeftRotation()).scale(t.getScale());
            for (int i = 0; i < 8; i++) {
                Vector3f c = m.transformPosition(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
                b[0] = Math.min(b[0], c.x); b[1] = Math.min(b[1], c.y); b[2] = Math.min(b[2], c.z);
                b[3] = Math.max(b[3], c.x); b[4] = Math.max(b[4], c.y); b[5] = Math.max(b[5], c.z);
            }
        }
        return b;
    }

    @Test
    @DisplayName("She stands on the ground, 2.6 to 3.2 blocks tall, nothing out past the katana")
    void fitsHerFrame() {
        float[] b = bounds(MaleniaModel.pieces());
        assertEquals(0f, b[1], .03f, "she stands on the ground");
        assertTrue(b[4] > 2.55f && b[4] < 3.2f, "her height: " + b[4]);
        for (int i : new int[]{0, 2, 3, 5}) assertTrue(Math.abs(b[i]) < 2.2f, "her reach: " + b[i]);
    }

    @Test
    @DisplayName("Every piece is a real, finite tile on one of her bones, within a boss's budget of displays")
    void piecesAreSound() {
        List<DisplaySculpt.Piece> pieces = MaleniaModel.pieces();
        assertTrue(pieces.size() > 600 && pieces.size() < 2000, "pieces: " + pieces.size());
        for (DisplaySculpt.Piece p : pieces) {
            assertTrue(p.size().x > 0 && p.size().y > 0 && p.size().z > 0, "a piece of " + p.part() + " has no size");
            assertTrue(p.center().isFinite() && p.rotation().isFinite(), "a piece of " + p.part() + " is not finite");
            assertDoesNotThrow(() -> MaleniaRig.Bone.valueOf(p.part()), p.part() + " is not a bone");
        }
    }

    @Test
    @DisplayName("Every attack starts from her stance, keeps its keys in order, and never sinks her through the floor")
    void attacksAreSound() {
        for (MaleniaRig.Move move : MaleniaRig.MOVES) {
            for (Matrix4f bone : MaleniaRig.bones(move, 0)) {
                assertTrue(bone.equals(new Matrix4f(), 1e-4f), move.id() + " does not start at rest");
            }
            move.bones().forEach((bone, keys) -> {
                for (int i = 1; i < keys.size(); i++) {
                    assertTrue(keys.get(i).tick() > keys.get(i - 1).tick(), move.id() + ": " + bone + "'s keys are out of order");
                }
                assertTrue(keys.get(keys.size() - 1).tick() <= move.ticks(), move.id() + ": " + bone + " keys past the end");
            });
            float[] reach = MaleniaViewerExport.reach(MaleniaModel.pieces(), move);
            assertTrue(reach[1] > -.5f, move.id() + " goes " + reach[1] + " below the floor");
        }
    }

    @Test
    @DisplayName("The Waterfowl Dance whirls her through the air across an area, her cape streaming behind")
    void waterfowlWhirls() {
        MaleniaRig.Move dance = MaleniaRig.move("waterfowl");
        float[] reach = MaleniaViewerExport.reach(MaleniaModel.pieces(), dance);
        assertTrue(reach[3] - reach[0] > 4f && reach[4] > 3.5f, "she covers an area in the air");
        float spin = MaleniaRig.sample(dance.bones().get(MaleniaRig.Bone.ROOT), 60)[1] - MaleniaRig.sample(dance.bones().get(MaleniaRig.Bone.ROOT), 59)[1];
        assertTrue(Math.abs(spin) >= 80, "she spins about a quarter turn a tick: " + spin);
        assertTrue(MaleniaRig.follow(dance, 60, false)[0] > 40, "the cape streams out as she whirls");
        assertEquals(0f, MaleniaRig.follow(MaleniaRig.move("idle"), 0, false)[0], 1e-4f, "the cape hangs at rest");
    }

    @Test
    @DisplayName("Posing a piece moves it with its bone and never stretches it")
    void posingIsRigid() {
        DisplaySculpt.Piece piece = MaleniaModel.pieces().get(0);
        Matrix4f turn = new Matrix4f().translate(1, 2, 3).rotateY(1.1f).rotateX(.4f);
        Transformation posed = MaleniaRig.posed(piece, turn);
        assertTrue(posed.getScale().equals(piece.transformation().getScale(), 1e-5f), "the piece keeps its size");
        assertTrue(MaleniaRig.posed(piece, new Matrix4f()).getTranslation().equals(piece.transformation().getTranslation(), 1e-5f),
                "at rest the piece stays where it is");
    }

    @Test
    @DisplayName("The robe flares as one cloth: its tiles stretch to close the gaps instead of tearing apart")
    void skirtStaysWhole() {
        List<DisplaySculpt.Piece> pieces = MaleniaModel.pieces();
        int[] owners = MaleniaRig.owners(pieces);
        for (String id : new String[]{"idle", "whirlwind", "waterfowl", "leap"}) {
            MaleniaRig.Move move = MaleniaRig.move(id);
            for (int tick = 0; tick <= move.ticks(); tick += 4) {
                List<Transformation> posed = MaleniaRig.pose(move, tick, pieces, owners);
                for (int i = 0; i < pieces.size(); i++) {
                    if (owners[i] != MaleniaRig.Bone.SKIRT.ordinal()) continue;
                    float restGap = Float.MAX_VALUE;
                    int nearest = -1;
                    for (int j = 0; j < pieces.size(); j++) {
                        if (j == i || owners[j] != owners[i]) continue;
                        float d = pieces.get(i).center().distance(pieces.get(j).center());
                        if (d < restGap) {
                            restGap = d;
                            nearest = j;
                        }
                    }
                    // Only tiles that touch at rest: the lace loops stand apart and simply ride the cloth.
                    if (restGap > Math.max(max(pieces.get(i).size()), max(pieces.get(nearest).size()))) continue;
                    float gap = centre(posed.get(i)).distance(centre(posed.get(nearest)));
                    // Neighbours may drift apart only as far as the tiles themselves stretch to cover it.
                    float stretch = Math.max(stretch(posed.get(i), pieces.get(i)), stretch(posed.get(nearest), pieces.get(nearest)));
                    assertTrue(gap <= restGap * stretch * 1.1f + .01f,
                            id + " at " + tick + ": a tile of the robe tears " + gap + " away from its neighbour, " + restGap + " at rest: "
                                    + pieces.get(i).center() + " / " + pieces.get(nearest).center() + " -> " + centre(posed.get(i)) + " / " + centre(posed.get(nearest)));
                }
            }
        }
    }

    private static Vector3f centre(Transformation t) {
        return t.getLeftRotation().transform(new Vector3f(t.getScale()).mul(.5f)).add(t.getTranslation());
    }

    private static float max(Vector3f v) {
        return Math.max(v.x, Math.max(v.y, v.z));
    }

    private static float stretch(Transformation posed, DisplaySculpt.Piece rest) {
        Vector3f s = new Vector3f(posed.getScale()).div(rest.size());
        return Math.max(s.x, Math.max(s.y, s.z));
    }

    @Test
    @DisplayName("A tile laid on a quad sits on it, and a loft's tiles face away from its axis")
    void sculptPlacesTiles() {
        DisplaySculpt s = new DisplaySculpt();
        s.quad(org.bukkit.Material.STONE, new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(1, 1, 0),
                .1f, new Vector3f(0, 0, 1));
        DisplaySculpt.Piece tile = s.pieces().get(0);
        assertEquals(-.05f, tile.center().z, 1e-4f, "its outer face lies on the quad");
        assertEquals(1f, tile.size().y, .03f);
        s.loft(List.of(DisplaySculpt.Ring.level(0f, 1f, 1f, 0f, 0f), DisplaySculpt.Ring.level(1f, 1f, 1f, 0f, 0f)), 8, null, null,
                (c, r) -> org.bukkit.Material.STONE, .1f);
        for (DisplaySculpt.Piece p : s.pieces().subList(1, 9)) {
            Vector3f normal = p.rotation().transform(new Vector3f(0, 0, 1));
            Vector3f out = new Vector3f(p.center().x, 0, p.center().z);
            assertTrue(normal.dot(out) > 0, "a loft tile faces outwards");
        }
    }
}
