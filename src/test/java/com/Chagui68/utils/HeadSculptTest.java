package com.Chagui68.utils;

import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.HeadSculpt.Box;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.Chagui68.utils.DisplaySculpt.v;
import static org.junit.jupiter.api.Assertions.*;

class HeadSculptTest {

    private static final int RED = 0xff0000, BLUE = 0x0000ff, GREY = 0x808080;

    /** The pixel of a piece's skin at {@code x}, {@code y} of the 64x64 player skin, without its alpha. */
    private static int pixel(HeadSculpt s, Piece piece, int x, int y) {
        assertTrue(piece.skin().startsWith(HeadSculpt.LOCAL), "a skin nobody uploaded is named, not textured");
        return s.skins().get(piece.skin().substring(HeadSculpt.LOCAL.length()))[y * 64 + x] & 0xffffff;
    }

    @Test
    @DisplayName("A box's front picture is cut into its heads from the model's right to its left, on the face the game draws at +z")
    void frontIsCutAcrossTheHeads() {
        HeadSculpt s = new HeadSculpt();
        Box box = s.box(v(0, 1, 0), v(2, 1, 1), new Quaternionf(), 2, 1, 1, GREY);
        box.front.rect(0, 0, 8, 8, RED).rect(8, 0, 8, 8, BLUE);
        List<Piece> pieces = s.pieces(Map.of());
        assertEquals(2, pieces.size());
        for (Piece piece : pieces) {
            assertEquals(new Vector3f(1, 1, 1), piece.size());
            // A head's +z face wears the skin's back, at 24, 8.
            assertEquals(piece.center().x < 0 ? RED : BLUE, pixel(s, piece, 24, 8));
            assertEquals(GREY, pixel(s, piece, 8, 8), "the back is untouched");
        }
    }

    @Test
    @DisplayName("The top is drawn as seen from in front and above, which the skin holds turned half round")
    void topIsTurnedHalfRound() {
        HeadSculpt s = new HeadSculpt();
        Box box = s.box(v(0, 1, 0), v(1, 1, 1), new Quaternionf(), 1, 1, 1, GREY);
        box.top.set(0, 0, RED);
        box.bottom.set(7, 7, BLUE);
        Piece piece = s.pieces(Map.of()).get(0);
        assertEquals(RED, pixel(s, piece, 8 + 7, 7));
        assertEquals(BLUE, pixel(s, piece, 16, 0));
    }

    @Test
    @DisplayName("Heads that wear the same pixels share a skin, heads inside a box are left out, and an uploaded skin goes out as its texture")
    void skinsAreShared() {
        HeadSculpt s = new HeadSculpt();
        s.box(v(0, 1, 0), v(3, 3, 3), new Quaternionf(), 3, 3, 3, GREY);
        List<Piece> pieces = s.pieces(Map.of());
        assertEquals(26, pieces.size());
        // A plain cube is all one skin: its hidden faces are painted like the rest.
        assertEquals(1, s.skins().size());
        String name = s.skins().keySet().iterator().next();
        assertEquals("texture", s.pieces(Map.of(name, "texture")).get(0).skin());
    }

    @Test
    @DisplayName("A flipped box is its mirror image wearing the same heads: its outer face still looks out, its back shows at the front")
    void flipMirrorsABox() {
        HeadSculpt s = new HeadSculpt();
        Quaternionf lean = new Quaternionf().rotateZ(.3f);
        Box right = s.box(v(-1, 1, .2), v(1, 1, 1), lean, 1, 1, 1, GREY);
        Box left = s.box(v(-1, 1, .2), v(1, 1, 1), lean, 1, 1, 1, GREY).flip();
        right.right.fill(RED);
        left.right.fill(RED);
        List<Piece> pieces = s.pieces(Map.of());
        Piece r = pieces.get(0), l = pieces.get(1);
        assertEquals(r.skin(), l.skin());
        assertTrue(l.center().equals(new Vector3f(1, 1, .2f), 1e-6f));
        Vector3f outR = r.rotation().transform(new Vector3f(-1, 0, 0)), outL = l.rotation().transform(new Vector3f(-1, 0, 0));
        assertTrue(outL.equals(new Vector3f(-outR.x, outR.y, outR.z), 1e-5f), "the painted face looks out on either side");
        Vector3f frontR = r.rotation().transform(new Vector3f(0, 0, 1)), backL = l.rotation().transform(new Vector3f(0, 0, -1));
        assertTrue(backL.equals(new Vector3f(-frontR.x, frontR.y, frontR.z), 1e-5f), "the back of the flipped box is where the front was");
    }
}
