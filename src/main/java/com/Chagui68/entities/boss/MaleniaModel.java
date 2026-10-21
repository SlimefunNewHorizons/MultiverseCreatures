package com.Chagui68.entities.boss;

import com.Chagui68.utils.DisplaySculpt;
import com.Chagui68.utils.DisplaySculpt.Lathe;
import com.Chagui68.utils.DisplaySculpt.Paint;
import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.DisplaySculpt.Ring;
import com.Chagui68.utils.DisplaySculpt.Shape;
import com.Chagui68.utils.DisplaySculpt.Span;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.Chagui68.utils.DisplaySculpt.along;
import static com.Chagui68.utils.DisplaySculpt.lerp;
import static com.Chagui68.utils.DisplaySculpt.noise;
import static com.Chagui68.utils.DisplaySculpt.perpendicular;
import static com.Chagui68.utils.DisplaySculpt.v;

/**
 * Malenia, Blade of Miquella (Elden Ring), sculpted from block displays with {@link DisplaySculpt}: a
 * tall, slender body with round surfaces instead of a player's cubes. The domed winged helm with its
 * crest, nose guard and fans of brass feathers; the fur collar over a red capelet; a bodice with a lace
 * neckline, a crossing strap and two studded belts; a pleated robe opening on a pale underskirt and a
 * dark lace panel, edged and hemmed with looped lace, under a shorter tunic; the long dark crimson cape
 * with gold knotwork, and over it, standing off it, two layers of wavy red locks ending near her waist;
 * the gilded prosthetic arm with the Hand of Malenia; the bell sleeve with its lace cuff and the
 * bandaged hand; golden feet.
 *
 * <p>Every piece belongs to a bone of {@link MaleniaRig} (its part is the bone's name), so her attacks
 * move her as a skeleton. Units are blocks; she faces {@code +z} with her feet on the origin, her right
 * side on {@code -x}. The display entity's yaw turns the whole body.</p>
 */
public final class MaleniaModel {

    /** Tag on every display piece, so a preview or a boss can find its own body. */
    public static final String TAG = "MSC_MaleniaModel";
    // ---- Palette ----
    private static final Material BRASS = Material.RAW_GOLD_BLOCK;
    private static final Material BRASS_LIGHT = Material.GOLD_BLOCK;
    private static final Material BRASS_DARK = Material.YELLOW_TERRACOTTA;
    private static final Material ROBE = Material.TERRACOTTA;
    private static final Material ROBE_LIGHT = Material.ORANGE_TERRACOTTA;
    private static final Material PALE = Material.WHITE_TERRACOTTA;
    private static final Material CHAIN = Material.BROWN_TERRACOTTA;
    private static final Material LEATHER = Material.BROWN_CONCRETE;
    private static final Material LACE = Material.WHITE_CONCRETE;
    private static final Material FUR = Material.SMOOTH_SANDSTONE;
    private static final Material FUR_LIGHT = Material.MUSHROOM_STEM;
    private static final Material SCARLET = Material.RED_WOOL;
    private static final Material CAPE = Material.RED_CONCRETE;
    private static final Material CAPE_DARK = Material.NETHER_WART_BLOCK;
    private static final Material HAIR = Material.RED_CONCRETE_POWDER;
    private static final Material HAIR_DARK = Material.RED_WOOL;
    private static final Material HAIR_LIGHT = Material.REDSTONE_BLOCK;
    private static final Material SKIN = Material.WHITE_TERRACOTTA;
    private static final Material LIPS = Material.RED_TERRACOTTA;
    private static final Material STEEL = Material.IRON_BLOCK;
    private static final Material EDGE = Material.WHITE_CONCRETE;
    private static final Material WRAP = Material.BLACK_WOOL;
    // ---- Surfaces: {y, radius across, radius front to back, centre x, centre z} ----
    private static final Lathe TORSO = new Lathe(new float[][]{
            {1.44f, .158f, .120f, 0f, 0f}, {1.55f, .146f, .110f, 0f, 0f}, {1.67f, .158f, .118f, 0f, .004f},
            {1.77f, .176f, .138f, 0f, .014f}, {1.86f, .186f, .144f, 0f, .018f}, {1.94f, .196f, .128f, 0f, .008f},
            {2.01f, .204f, .112f, 0f, 0f}, {2.06f, .160f, .096f, 0f, 0f}, {2.10f, .065f, .058f, 0f, 0f}});
    private static final Lathe DRESS = new Lathe(new float[][]{
            {1.50f, .160f, .124f, 0f, 0f}, {1.36f, .192f, .142f, 0f, 0f}, {1.20f, .224f, .162f, 0f, .004f},
            {0.85f, .262f, .198f, 0f, .012f}, {0.45f, .308f, .240f, 0f, .020f}, {0.05f, .356f, .288f, 0f, .030f}});
    private static final Lathe TUNIC = new Lathe(new float[][]{
            {1.53f, .170f, .132f, 0f, 0f}, {1.32f, .212f, .158f, 0f, .002f},
            {1.12f, .244f, .180f, 0f, .006f}, {0.93f, .268f, .202f, 0f, .010f}});
    private static final Lathe CAPE_SURFACE = new Lathe(new float[][]{
            {2.04f, .222f, .168f, 0f, -.035f}, {1.86f, .268f, .214f, 0f, -.050f}, {1.40f, .338f, .284f, 0f, -.075f},
            {0.90f, .418f, .344f, 0f, -.100f}, {0.45f, .488f, .394f, 0f, -.118f}, {0.03f, .548f, .438f, 0f, -.132f}});
    private static final Lathe HELM = new Lathe(new float[][]{
            {2.300f, .134f, .152f, 0f, .006f}, {2.360f, .144f, .162f, 0f, .009f}, {2.430f, .145f, .162f, 0f, .009f},
            {2.500f, .130f, .146f, 0f, .005f}, {2.555f, .097f, .110f, 0f, .002f}, {2.590f, .052f, .060f, 0f, 0f},
            {2.606f, .012f, .014f, 0f, 0f}});
    private static final Lathe FACE = new Lathe(new float[][]{
            {2.195f, .036f, .040f, 0f, .040f}, {2.235f, .068f, .078f, 0f, .022f},
            {2.290f, .086f, .096f, 0f, .012f}, {2.350f, .094f, .104f, 0f, .009f}});
    private static final Lathe PAULDRON = new Lathe(new float[][]{
            {1.875f, .118f, .136f, -.238f, -.005f}, {1.950f, .128f, .146f, -.235f, -.005f},
            {2.020f, .122f, .140f, -.232f, -.005f}, {2.075f, .095f, .112f, -.228f, -.005f},
            {2.110f, .050f, .064f, -.222f, -.005f}, {2.124f, .012f, .015f, -.220f, -.005f}});

    // ---- Joints, shared with the rig ----
    static final Vector3f SHOULDER_R = v(-.212, 2.015, -.005), ELBOW_R = v(-.248, 1.640, 0);
    static final Vector3f WRIST_R = v(-.250, 1.300, .085), FIST_R = v(-.247, 1.238, .118);
    static final Vector3f SHOULDER_L = v(.205, 2.010, -.010), ELBOW_L = v(.240, 1.650, -.025);
    static final Vector3f WRIST_L = v(.256, 1.315, .010);
    static final Vector3f HIP_R = v(-.085, 1.24, -.005), KNEE_R = v(-.09, .70, .02);
    static final Vector3f HIP_L = v(.085, 1.24, -.005), KNEE_L = v(.09, .70, .02);
    /** The Hand of Malenia points low and ahead, a little across her body. */
    static final Vector3f SWORD = v(-.12, -.42, 1).normalize();
    /** How far the blade reaches from the guard. */
    static final float BLADE_LENGTH = 1.15f;

    /** The gold knotwork low on the cape's back. */
    private static final String[] KNOT = {
            "....g.....g.....g....",
            "...g.g...g.g...g.g...",
            "..g...g.g...g.g...g..",
            ".g..g..g..g..g..g..g.",
            "g..g.g...g.g...g.g..g",
            ".g..g..g..g..g..g..g.",
            "..g...g.g...g.g...g..",
            "...g.g...ggg...g.g...",
            "....g...g...g...g....",
            "...g.g.g..g..g.g.g...",
            "..g...g..g.g..g...g..",
            ".g...g..g...g..g...g.",
            "g...g..g.....g..g...g",
            ".ggg..g.......g..ggg.",
            "......g.......g......",
    };

    private static final List<Piece> PIECES = build();

    private MaleniaModel() {
    }

    /** The model at rest. */
    public static List<Piece> pieces() {
        return PIECES;
    }

    private static List<Piece> build() {
        DisplaySculpt s = new DisplaySculpt();
        feet(s);
        dress(s);
        tunic(s);
        torso(s);
        collar(s);
        face(s);
        helm(s);
        rightArm(s);
        sleeve(s);
        katana(s);
        capelet(s);
        cape(s);
        longHair(s);
        return List.copyOf(s.pieces());
    }

    // =====================================================================================
    // The body
    // =====================================================================================

    /** Golden prosthetic feet peeking from under the hem: ankle, long instep and four toes. */
    private static void feet(DisplaySculpt s) {
        for (int side : new int[]{-1, 1}) {
            s.part(side < 0 ? "SHIN_R" : "SHIN_L");
            float x = side * .085f;
            s.loft(along(v(x, .17, -.01), v(x, .06, 0), new float[]{.034f, .036f}, new float[]{.034f, .040f}, v(1, 0, 0)),
                    6, null, null, (c, r) -> BRASS_DARK, .012f);
            s.loft(along(v(x, .035, -.05), v(x, .028, .175), new float[]{.032f, .042f, .040f}, new float[]{.028f, .030f, .020f},
                    v(0, 1, 0)), 6, null, null, (c, r) -> BRASS, .012f);
            for (int k = 0; k < 4; k++) {
                float dx = side * (k - 1.5f) * .016f;
                s.bar(BRASS_LIGHT, v(x + dx, .02, .17), v(x + dx * 1.2f, .012, .228), .013f, .013f, v(1, 0, 0));
            }
        }
    }

    /**
     * The long robe: pleated panels flaring from the waist, open at the front on a pale underskirt
     * and a dark lace panel, its edges laced and looped and its hem banded with lace.
     */
    private static void dress(DisplaySculpt s) {
        s.part("SKIRT");
        List<Ring> rings = DRESS.ringsDown();
        int n = rings.size();
        Span open = row -> {
            double h = opening(rings.get(row).center().y);
            return new double[]{h, Math.PI * 2 - h};
        };
        Shape pleats = (c, r, a) -> {
            float depth = r / (float) (n - 1);
            float hem = r == n - 1 ? .012f * (float) Math.sin(c * 1.7) : 0f;
            return new float[]{1f + (c % 2 == 0 ? .03f : -.012f) * depth, hem};
        };
        s.loft(rings, 20, open, pleats, (c, r) -> c % 2 == 0 ? ROBE : ROBE_LIGHT, .014f);

        // The underskirt and the lace panel stop higher, so her feet show below them.
        List<Ring> inner = new ArrayList<>();
        for (float y : new float[]{1.48f, 1.20f, .85f, .45f, .13f}) inner.add(DRESS.at(y));
        for (int side : new int[]{-1, 1}) {
            Span half = row -> {
                double h = opening(inner.get(row).center().y) + .03;
                return side > 0 ? new double[]{.10, h} : new double[]{Math.PI * 2 - h, Math.PI * 2 - .10};
            };
            s.loft(inner, 3, half, (c, r, a) -> new float[]{.975f, 0f}, (c, r) -> PALE, .01f);
        }
        s.loft(inner, 2, row -> new double[]{-.115, .115}, (c, r, a) -> new float[]{.958f, 0f}, (c, r) -> CHAIN, .01f);
        int row = 0;
        for (float y = 1.42f; y > .17f; y -= .07f, row++) {
            double[] at = row % 2 == 0 ? new double[]{-.055, .055} : new double[]{0};
            for (double a : at) {
                s.quad(ROBE_LIGHT, inside(y - .014f, a - .028, .963f), inside(y - .014f, a + .028, .963f),
                        inside(y + .014f, a - .028, .963f), inside(y + .014f, a + .028, .963f), .006f, v(Math.sin(a), 0, Math.cos(a)));
            }
        }

        // Lace down both edges of the opening, looped on its inner side.
        for (int side : new int[]{-1, 1}) {
            List<Vector3f> spine = new ArrayList<>(), sides = new ArrayList<>();
            for (float y : new float[]{1.49f, 1.20f, .90f, .60f, .30f, .07f}) {
                double a = side * opening(y);
                spine.add(DRESS.point(a, y, .016f));
                sides.add(tangent(DRESS, a, y));
            }
            s.ribbon(spine, sides, widths(spine.size(), .028f), .008f, (c, r) -> LACE);
            for (float y = 1.42f; y > .16f; y -= .085f) {
                double a = side * (opening(y) - .05);
                loop(s, DRESS.point(a, y, .018f), DRESS.point(a, y, 1f).sub(DRESS.point(a, y, 0f)), .022f);
            }
        }
        double h = opening(.05f);
        s.loft(List.of(DRESS.at(.05f), DRESS.at(.085f)), 20, r -> new double[]{h, Math.PI * 2 - h},
                (c, r, a) -> new float[]{1.035f, 0f}, (c, r) -> LACE, .008f);
    }

    /** How far the robe's opening reaches either side of the front at height {@code y}, in radians. */
    private static double opening(float y) {
        return lerp(.16, .56, Math.max(0, Math.min(1, (1.50 - y) / 1.45)));
    }

    /** A point inside the robe at {@code mul} of its radius: the layers under the opening. */
    private static Vector3f inside(float y, double a, float mul) {
        Ring ring = DRESS.at(y);
        return ring.at(a).sub(ring.center()).mul(mul).add(ring.center());
    }

    /** The tunic over the robe, opening wider towards its hem, laced along its edges and looped at its hem. */
    private static void tunic(DisplaySculpt s) {
        s.part("SKIRT");
        List<Ring> rings = TUNIC.ringsDown();
        Span open = row -> {
            double h = lerp(.26, .95, row / 3.0);
            return new double[]{h, Math.PI * 2 - h};
        };
        Shape folds = (c, r, a) -> new float[]{1f + (c % 3 == 0 ? .022f : 0f) * r / 3f, r == 3 ? .02f * (float) Math.sin(c * 2.1) : 0f};
        s.loft(rings, 18, open, folds, (c, r) -> c % 3 == 0 ? ROBE_LIGHT : ROBE, .012f);
        for (int side : new int[]{-1, 1}) {
            List<Vector3f> spine = new ArrayList<>(), sides = new ArrayList<>();
            for (int k = 0; k <= 3; k++) {
                float y = rings.get(k).center().y;
                double a = side * lerp(.26, .95, k / 3.0);
                spine.add(TUNIC.point(a, y, .016f));
                sides.add(tangent(TUNIC, a, y));
            }
            s.ribbon(spine, sides, widths(4, .025f), .008f, (c, r) -> LACE);
            for (int k = 1; k < 8; k++) {
                float y = lerp(1.50f, .97f, k / 8f);
                double a = side * (lerp(.26, .95, (1.53 - y) / .60) - .05);
                loop(s, TUNIC.point(a, y, .018f), TUNIC.point(a, y, 1f).sub(TUNIC.point(a, y, 0f)), .02f);
            }
        }
        for (int k = 0; k < 24; k++) {
            double a = lerp(.95, Math.PI * 2 - .95, (k + .5) / 24);
            loop(s, TUNIC.point(a, .915f, .014f), TUNIC.point(a, .915f, 1f).sub(TUNIC.point(a, .915f, 0f)), .026f);
        }
    }

    /** The bodice: pleated, with the bust, a lace neckline, the crossing strap and two studded belts. */
    private static void torso(DisplaySculpt s) {
        s.part("CHEST");
        s.loft(TORSO.rings(), 12, null, null, (c, r) -> c % 2 == 0 ? ROBE : ROBE_LIGHT, .016f);
        bust(s, .072f, .122f, ROBE);
        // Lace down the front of the bodice and a V from the shoulders, with a lace knot.
        s.ribbon(List.of(TORSO.point(0, 1.98f, .03f), TORSO.point(0, 1.86f, .03f), TORSO.point(0, 1.74f, .022f),
                        TORSO.point(0, 1.62f, .018f)),
                List.of(v(1, 0, 0), v(1, 0, 0), v(1, 0, 0), v(1, 0, 0)), widths(4, .03f), .008f, (c, r) -> LACE);
        for (int side : new int[]{-1, 1}) {
            List<Vector3f> spine = new ArrayList<>(), sides = new ArrayList<>();
            for (int k = 0; k <= 3; k++) {
                double a = side * lerp(.42, .04, k / 3.0);
                float y = lerp(2.0f, 1.76f, k / 3f);
                spine.add(TORSO.point(a, y, .032f));
                sides.add(v(0, 1, 0).cross(v(Math.sin(a), 0, Math.cos(a))).add(0, side * .4f, 0));
            }
            s.ribbon(spine, sides, widths(4, .02f), .008f, (c, r) -> LACE);
        }
        s.disc(LACE, TORSO.point(0, 1.70f, .024f), v(0, 0, 1), .026f, .008f);

        // The strap from her left shoulder to her right hip.
        List<Vector3f> strap = new ArrayList<>(), across = new ArrayList<>();
        for (int k = 0; k <= 7; k++) {
            double a = lerp(1.0, -.85, k / 7.0);
            float y = lerp(2.0f, 1.50f, k / 7f);
            strap.add(TORSO.point(a, y, k > 1 && k < 6 ? .036f : .018f));
        }
        for (int k = 0; k <= 7; k++) {
            Vector3f along = new Vector3f(strap.get(Math.min(7, k + 1))).sub(strap.get(Math.max(0, k - 1)));
            double a = lerp(1.0, -.85, k / 7.0);
            across.add(v(Math.sin(a), 0, Math.cos(a)).cross(along));
        }
        s.ribbon(strap, across, widths(8, .034f), .01f, (c, r) -> LEATHER);

        // Two belts above the tunic, the upper one studded with brass, a leather loop hanging between.
        for (float[] belt : new float[][]{{1.452f, 1.488f}, {1.502f, 1.530f}}) {
            s.loft(List.of(grown(TORSO, belt[0], .024f), grown(TORSO, belt[1], .024f)), 14, null, null, (c, r) -> LEATHER, .012f);
        }
        for (double a : new double[]{-.8, -.4, 0, .4, .8}) {
            Ring lo = grown(TORSO, 1.505f, .036f), hi = grown(TORSO, 1.528f, .036f);
            s.quad(BRASS_LIGHT, lo.at(a - .06), lo.at(a + .06), hi.at(a - .06), hi.at(a + .06), .01f, v(Math.sin(a), 0, Math.cos(a)));
        }
        List<Vector3f> loop = new ArrayList<>(), loopSides = new ArrayList<>();
        for (int k = 0; k <= 5; k++) {
            double a = lerp(-.35, .35, k / 5.0);
            float y = 1.45f - .07f * (float) Math.sin(Math.PI * k / 5);
            loop.add(TUNIC.point(a, y, .024f));
            loopSides.add(v(0, 1, 0));
        }
        s.ribbon(loop, loopSides, widths(6, .02f), .008f, (c, r) -> LEATHER);
    }

    /** The two breasts, as smooth front-facing domes {@code x} either side of the middle. */
    private static void bust(DisplaySculpt s, float x, float z, Material m) {
        for (int side : new int[]{-1, 1}) {
            float cx = side * x;
            s.loft(List.of(Ring.level(1.77f, .032f, .026f, cx, z + .008f), Ring.level(1.83f, .064f, .052f, cx, z),
                    Ring.level(1.89f, .040f, .032f, cx, z + .006f)), 6, r -> new double[]{-1.7, 1.7}, null, (c, r) -> m, .012f);
        }
    }

    /** The fur collar: two rows of tufts round the neck and shoulders, each one turned its own way. */
    private static void collar(DisplaySculpt s) {
        s.part("CHEST");
        int k = 0;
        float[][] rows = {{2.062f, .205f, .150f, 16f, 0f}, {2.108f, .150f, .115f, 12f, .26f}};
        for (float[] row : rows) {
            for (int i = 0; i < (int) row[3]; i++, k++) {
                double a = i * 2 * Math.PI / row[3] + row[4];
                Vector3f c = Ring.level(row[0], row[1], row[2], 0f, -.005f).at(a);
                float size = .075f + .02f * noise(k);
                Quaternionf q = new Quaternionf().rotateY((float) a).rotateX(-.35f + .5f * noise(k + 40))
                        .rotateZ(.6f * (noise(k + 80) - .5f));
                s.box(k % 2 == 0 ? FUR : FUR_LIGHT, c, v(size * 1.2f, size * .8f, size), q);
            }
        }
    }

    /** What the helm leaves of her face: jaw, chin and lips, over a slender neck. */
    private static void face(DisplaySculpt s) {
        s.part("HEAD");
        neck(s);
        s.loft(FACE.rings(), 8, r -> new double[]{-1.35, 1.35}, null, (c, r) -> SKIN, .012f);
        lips(s, FACE);
    }

    private static void neck(DisplaySculpt s) {
        s.loft(along(v(0, 2.05, -.01), v(0, 2.22, .01), new float[]{.046f, .044f}, new float[]{.046f, .044f}, v(1, 0, 0)),
                6, null, null, (c, r) -> SKIN, .012f);
    }

    private static void lips(DisplaySculpt s, Lathe face) {
        s.quad(LIPS, face.point(-.17, 2.250f, .003f), face.point(.17, 2.250f, .003f),
                face.point(-.17, 2.258f, .003f), face.point(.17, 2.258f, .003f), .006f, v(0, 0, 1));
        s.quad(LIPS, face.point(-.15, 2.238f, .003f), face.point(.15, 2.238f, .003f),
                face.point(-.15, 2.249f, .003f), face.point(.15, 2.249f, .003f), .006f, v(0, 0, 1));
    }

    /**
     * The winged helm: a dome down to her nose at the front and her nape at the back, with an
     * engraved band, a gilt rim, a crest over the top, a nose guard and brow ridges, and on each side
     * a fan of eight brass feathers over six coverts.
     */
    private static void helm(DisplaySculpt s) {
        s.part("HEAD");
        Shape rim = (c, r, a) -> new float[]{1f, r == 0 ? (float) (Math.cos(a) > 0 ? .035 * Math.cos(a) : .055 * Math.cos(a)) : 0f};
        s.loft(HELM.rings(), 16, null, rim, (c, r) -> r == 1 ? BRASS_DARK : BRASS, .014f);
        Ring rimLo = HELM.at(2.300f);
        Ring rimHi = grown(HELM, 2.318f, 0f);
        List<Ring> trim = List.of(new Ring(rimLo.center(), rimLo.u(), rimLo.w(), rimLo.ru() + .008f, rimLo.rw() + .008f),
                new Ring(rimHi.center(), rimHi.u(), rimHi.w(), rimHi.ru() + .008f, rimHi.rw() + .008f));
        s.loft(trim, 16, null, (c, r, a) -> new float[]{1f, (float) (Math.cos(a) > 0 ? .035 * Math.cos(a) : .055 * Math.cos(a))},
                (c, r) -> BRASS_LIGHT, .01f);
        List<Vector3f> crest = new ArrayList<>(), crestSides = new ArrayList<>();
        for (float y : new float[]{2.37f, 2.45f, 2.52f, 2.57f}) crest.add(HELM.point(0, y, .012f));
        crest.add(v(0, 2.618, .005));
        for (float y : new float[]{2.57f, 2.52f, 2.45f, 2.37f, 2.30f}) crest.add(HELM.point(Math.PI, y, .012f));
        for (int k = 0; k < crest.size(); k++) crestSides.add(v(1, 0, 0));
        s.ribbon(crest, crestSides, widths(crest.size(), .03f), .012f, (c, r) -> BRASS_LIGHT);
        s.bar(BRASS_LIGHT, HELM.point(0, 2.40f, .014f), v(0, 2.30, HELM.point(0, 2.36f, 0f).z + .022f), .024f, .016f, v(1, 0, 0));
        // The neck guard flares out over her hair at the back.
        s.loft(List.of(HELM.at(2.300f), Ring.level(2.215f, .168f, .186f, 0f, -.006f)), 10,
                r -> new double[]{1.75, Math.PI * 2 - 1.75}, (c, r, a) -> new float[]{1f, r == 0 ? (float) (.055 * Math.cos(a)) : 0f},
                (c, r) -> c % 2 == 0 ? BRASS : BRASS_DARK, .012f);
        for (int side : new int[]{-1, 1}) {
            for (float y0 : new float[]{2.47f, 2.40f}) {
                List<Vector3f> scroll = new ArrayList<>(), scrollSides = new ArrayList<>();
                for (int k = 0; k <= 3; k++) {
                    double a = side * lerp(.55, 2.1, k / 3.0);
                    float y = y0 - .03f * (float) Math.sin(Math.PI * k / 3) * (y0 > 2.45f ? 1 : -1);
                    scroll.add(HELM.point(a, y, .008f));
                    scrollSides.add(v(0, 1, 0));
                }
                s.ribbon(scroll, scrollSides, widths(4, .012f), .008f, (c, r) -> BRASS_DARK);
            }
        }
        for (int side : new int[]{-1, 1}) {
            List<Vector3f> brow = new ArrayList<>(), browSides = new ArrayList<>();
            for (int k = 0; k <= 3; k++) {
                double a = side * lerp(.12, .85, k / 3.0);
                float y = 2.372f + .012f * (float) Math.sin(Math.PI * k / 3);
                brow.add(HELM.point(a, y, .01f));
                browSides.add(v(0, 1, 0));
            }
            s.ribbon(brow, browSides, widths(4, .016f), .01f, (c, r) -> BRASS_DARK);
        }
        for (int side : new int[]{-1, 1}) {
            Vector3f base = v(side * .138, 2.455, -.025);
            Vector3f back = v(side * .55, 0, -1).normalize();
            Vector3f up = v(side * .38, 1, .05).normalize();
            Vector3f normal = new Vector3f(back).cross(up).normalize();
            float[] angles = {14, 27, 40, 53, 66, 79, 92, 105};
            float[] lengths = {.20f, .27f, .33f, .38f, .41f, .39f, .33f, .25f};
            for (int i = 0; i < angles.length; i++) {
                feather(s, base, back, up, normal, angles[i], lengths[i], 0f, i % 2 == 0 ? BRASS : BRASS_DARK, .055f, .012f);
            }
            for (int i = 0; i < 6; i++) {
                float len = .12f + .06f * (float) Math.sin(i / 5.0 * Math.PI);
                feather(s, base, back, up, normal, 20 + i * 16f, len, side * .014f, BRASS_LIGHT, .045f, .01f);
            }
            s.disc(BRASS_LIGHT, base, v(side, 0, 0), .034f, .02f);
            s.disc(BRASS_DARK, new Vector3f(base).add(side * .012f, 0, 0), v(side, 0, 0), .02f, .01f);
        }
    }

    /** A feather fanning out at {@code angle} degrees from {@code back} towards {@code up}, curling back at its tip. */
    private static void feather(DisplaySculpt s, Vector3f base, Vector3f back, Vector3f up, Vector3f normal,
                                float angle, float length, float offset, Material m, float width, float thickness) {
        List<Vector3f> spine = new ArrayList<>(), sides = new ArrayList<>();
        Vector3f p = new Vector3f(base).add(new Vector3f(normal).mul(offset));
        spine.add(new Vector3f(p));
        for (int j = 0; j < 3; j++) {
            double a = Math.toRadians(angle - 7 * j);
            Vector3f dir = new Vector3f(back).mul((float) Math.cos(a)).add(new Vector3f(up).mul((float) Math.sin(a))).normalize();
            if (j == 0) sides.add(new Vector3f(normal).cross(dir));
            p.add(new Vector3f(dir).mul(length / 3));
            spine.add(new Vector3f(p));
            sides.add(new Vector3f(normal).cross(dir));
        }
        s.ribbon(spine, sides, new float[]{width * .55f, width, width * .8f, width * .25f}, thickness, (c, r) -> m);
    }

    /**
     * The gilded prosthetic arm, in both phases: a domed pauldron with a gilt rim, an engraved band,
     * two lames and a medallion; a banded upper arm, a round elbow cop with a fan; an engraved
     * vambrace and a fist of jointed fingers closed round the hilt.
     */
    private static void rightArm(DisplaySculpt s) {
        s.part("ARM_R");
        s.loft(PAULDRON.rings(), 12, null, null, (c, r) -> r == 0 ? BRASS_LIGHT : r == 2 ? BRASS_DARK : BRASS, .014f);
        for (int k = 0; k < 2; k++) {
            final int lame = k;
            float top = 1.872f - k * .045f, shrink = k * .006f;
            s.loft(List.of(Ring.level(top, .114f - shrink, .130f - shrink, -.240f, -.005f),
                            Ring.level(top - .05f, .104f - shrink, .122f - shrink, -.244f, -.005f)),
                    6, r -> new double[]{Math.PI * 1.05, Math.PI * 1.95}, null, (c, r) -> lame == 0 ? BRASS : BRASS_DARK, .012f);
        }
        Vector3f medallion = PAULDRON.point(-Math.PI / 2, 1.965f, .004f);
        Vector3f facing = v(-1, .25, 0);
        s.disc(BRASS_LIGHT, medallion, facing, .046f, .012f);
        s.disc(BRASS_DARK, new Vector3f(medallion).add(-.007f, .002f, 0), facing, .031f, .008f);
        s.disc(BRASS_LIGHT, new Vector3f(medallion).add(-.012f, .003f, 0), facing, .013f, .008f);

        Vector3f top = new Vector3f(SHOULDER_R).add(0, -.07f, 0);
        Vector3f upper = new Vector3f(ELBOW_R).sub(top).normalize();
        s.loft(along(top, ELBOW_R, new float[]{.060f, .057f, .052f}, new float[]{.062f, .059f, .054f}, v(0, 0, 1)),
                8, null, null, (c, r) -> BRASS, .012f);
        for (float t : new float[]{.38f, .72f}) {
            Vector3f c = new Vector3f(top).lerp(ELBOW_R, t);
            s.loft(along(new Vector3f(c).sub(new Vector3f(upper).mul(.012f)), new Vector3f(c).add(new Vector3f(upper).mul(.012f)),
                    new float[]{.066f, .064f}, new float[]{.068f, .066f}, v(0, 0, 1)), 8, null, null, (cc, r) -> BRASS_DARK, .01f);
        }

        s.part("FOREARM_R");
        s.loft(along(new Vector3f(ELBOW_R).sub(new Vector3f(upper).mul(.045f)), new Vector3f(ELBOW_R).add(new Vector3f(upper).mul(.045f)),
                new float[]{.042f, .066f, .066f, .042f}, new float[]{.042f, .066f, .066f, .042f}, v(0, 0, 1)),
                6, null, null, (c, r) -> BRASS_LIGHT, .012f);
        for (int k = 0; k < 3; k++) {
            Vector3f dir = v(-.55, .25 - .25 * k, -.75).normalize();
            s.bar(BRASS, new Vector3f(ELBOW_R).add(new Vector3f(dir).mul(.04f)), new Vector3f(ELBOW_R).add(new Vector3f(dir).mul(.115f)),
                    .032f, .01f, v(0, 1, 0).cross(dir));
        }
        Vector3f fore = new Vector3f(WRIST_R).sub(ELBOW_R).normalize();
        s.loft(along(ELBOW_R, WRIST_R, new float[]{.056f, .061f, .058f, .046f}, new float[]{.058f, .064f, .060f, .048f}, v(1, 0, 0)),
                8, null, null, (c, r) -> BRASS, .012f);
        Vector3f out = v(-1, 0, 0).sub(new Vector3f(fore).mul(-fore.x)).normalize();
        for (int k = -1; k <= 1; k++) {
            float turn = k * .7f;
            Vector3f o = new Vector3f(out).mul((float) Math.cos(turn)).add(new Vector3f(fore).cross(out).mul((float) Math.sin(turn)));
            s.bar(BRASS_DARK, new Vector3f(ELBOW_R).add(new Vector3f(fore).mul(.05f)).add(new Vector3f(o).mul(.066f)),
                    new Vector3f(WRIST_R).sub(new Vector3f(fore).mul(.03f)).add(new Vector3f(o).mul(.052f)),
                    .012f, .01f, new Vector3f(fore).cross(o));
        }
        s.loft(along(new Vector3f(WRIST_R).sub(new Vector3f(fore).mul(.016f)), new Vector3f(WRIST_R).add(new Vector3f(fore).mul(.016f)),
                new float[]{.056f, .054f}, new float[]{.058f, .056f}, v(1, 0, 0)), 8, null, null, (c, r) -> BRASS_LIGHT, .01f);

        // The fist: palm and back plate on the outer side of the hilt, four fingers curled round it, the thumb over them.
        s.part("HAND_R");
        Vector3f d = SWORD;
        Vector3f pOut = v(-1, 0, 0).sub(new Vector3f(d).mul(-d.x)).normalize();
        Vector3f pUp = new Vector3f(d).cross(pOut).normalize();
        Vector3f palm = new Vector3f(FIST_R).add(new Vector3f(pOut).mul(.032f));
        s.bar(BRASS_DARK, new Vector3f(palm).sub(new Vector3f(d).mul(.042f)), new Vector3f(palm).add(new Vector3f(d).mul(.042f)), .062f, .026f, pUp);
        Vector3f back = new Vector3f(FIST_R).add(new Vector3f(pOut).mul(.048f));
        s.bar(BRASS_LIGHT, new Vector3f(back).sub(new Vector3f(d).mul(.036f)), new Vector3f(back).add(new Vector3f(d).mul(.036f)), .05f, .008f, pUp);
        s.bar(BRASS_DARK, WRIST_R, new Vector3f(palm).sub(new Vector3f(d).mul(.035f)), .05f, .045f, pUp);
        float[] around = {0f, 75f, 150f, 215f};
        float[] radius = {.034f, .031f, .029f, .027f};
        for (int k = 0; k < 4; k++) {
            float off = -.027f + k * .018f;
            for (int j = 0; j < 3; j++) {
                s.bar(BRASS, aroundHilt(off, around[j], radius[j], d, pOut, pUp), aroundHilt(off, around[j + 1], radius[j + 1], d, pOut, pUp),
                        .016f, .016f, d);
            }
        }
        Vector3f thumb0 = new Vector3f(FIST_R).add(new Vector3f(pOut).mul(.02f)).add(new Vector3f(pUp).mul(-.03f)).sub(new Vector3f(d).mul(.03f));
        Vector3f thumb1 = new Vector3f(FIST_R).add(new Vector3f(pUp).mul(-.036f)).add(new Vector3f(pOut).mul(-.006f));
        Vector3f thumb2 = new Vector3f(FIST_R).add(new Vector3f(pUp).mul(-.022f)).add(new Vector3f(pOut).mul(-.03f)).add(new Vector3f(d).mul(.018f));
        s.bar(BRASS, thumb0, thumb1, .018f, .018f, d);
        s.bar(BRASS, thumb1, thumb2, .016f, .016f, d);
    }

    private static Vector3f aroundHilt(float along, float degrees, float r, Vector3f d, Vector3f pOut, Vector3f pUp) {
        double a = Math.toRadians(degrees);
        return new Vector3f(FIST_R).add(new Vector3f(d).mul(along))
                .add(new Vector3f(pOut).mul((float) (Math.cos(a) * r))).add(new Vector3f(pUp).mul((float) (Math.sin(a) * r)));
    }

    /** The robe's bell sleeve, its lace cuff with loops hanging from it, and the pale bandaged hand. */
    private static void sleeve(DisplaySculpt s) {
        s.part("ARM_L");
        Paint pleat = (c, r) -> c % 2 == 0 ? ROBE : ROBE_LIGHT;
        s.loft(along(new Vector3f(SHOULDER_L).add(0, .03f, 0), ELBOW_L, new float[]{.070f, .066f, .061f},
                new float[]{.068f, .064f, .060f}, v(0, 0, 1)), 8, null, null, pleat, .012f);
        s.part("FOREARM_L");
        s.loft(along(ELBOW_L, WRIST_L, new float[]{.061f, .066f, .074f, .085f}, new float[]{.060f, .065f, .072f, .082f},
                v(0, 0, 1)), 8, null, null, pleat, .012f);
        Vector3f d = new Vector3f(WRIST_L).sub(ELBOW_L).normalize();
        s.loft(along(new Vector3f(WRIST_L).sub(new Vector3f(d).mul(.01f)), new Vector3f(WRIST_L).add(new Vector3f(d).mul(.022f)),
                new float[]{.088f, .091f}, new float[]{.085f, .088f}, v(0, 0, 1)), 10, null, null, (c, r) -> LACE, .01f);
        Ring hem = along(new Vector3f(WRIST_L).add(new Vector3f(d).mul(.034f)), new Vector3f(WRIST_L).add(new Vector3f(d).mul(.05f)),
                new float[]{.09f, .09f}, new float[]{.087f, .087f}, v(0, 0, 1)).get(0);
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5;
            Vector3f c = hem.at(a);
            loop(s, c, new Vector3f(c).sub(hem.center()), .022f);
        }
        hand(s, d, true);
    }

    /** Her left hand hanging open: palm, four slightly curled fingers and the thumb, bandaged or bare. */
    private static void hand(DisplaySculpt s, Vector3f d, boolean bandaged) {
        Vector3f palmTop = new Vector3f(WRIST_L).add(new Vector3f(d).mul(.03f));
        Vector3f palmEnd = new Vector3f(palmTop).add(0, -.085f, .008f);
        s.bar(SKIN, palmTop, palmEnd, .056f, .026f, v(0, 0, 1));
        if (bandaged) {
            for (float t : new float[]{.15f, .5f, .8f}) {
                Vector3f c = new Vector3f(palmTop).lerp(palmEnd, t);
                s.bar(LACE, new Vector3f(c).add(0, .007f, 0), new Vector3f(c).add(0, -.007f, 0), .06f, .03f, v(0, 0, 1));
            }
        }
        for (int k = 0; k < 4; k++) {
            float z = -.021f + k * .014f;
            Vector3f base = new Vector3f(palmEnd).add(-.002f, .004f, z);
            Vector3f mid = new Vector3f(base).add(-.004f, -.032f, .004f);
            Vector3f tip = new Vector3f(mid).add(-.008f, -.026f, .01f);
            s.bar(SKIN, base, mid, .013f, .013f, v(0, 0, 1));
            s.bar(bandaged && k == 1 ? LACE : SKIN, mid, tip, .012f, .012f, v(0, 0, 1));
        }
        s.bar(SKIN, new Vector3f(palmTop).add(-.01f, -.02f, .03f), new Vector3f(palmTop).add(-.016f, -.06f, .046f), .014f, .014f, v(1, 0, 0));
    }

    /**
     * The Hand of Malenia: a diamond-wrapped hilt with its pommel, a gilt guard and collar, and a long
     * blade curving gently away from its glowing edge to a point.
     */
    private static void katana(DisplaySculpt s) {
        s.part("HAND_R");
        Vector3f d = SWORD;
        Vector3f edge = v(0, -1, 0).sub(new Vector3f(d).mul(-d.y)).normalize();
        Vector3f flat = new Vector3f(d).cross(edge).normalize();
        Vector3f a = new Vector3f(FIST_R).sub(new Vector3f(d).mul(.10f)), b = new Vector3f(FIST_R).add(new Vector3f(d).mul(.075f));
        s.bar(WRAP, a, b, .032f, .026f, edge);
        s.bar(WRAP, a, b, .026f, .032f, edge);
        Vector3f diagonal = new Vector3f(edge).add(flat).normalize();
        for (int k = 0; k < 6; k++) {
            Vector3f c = new Vector3f(a).add(new Vector3f(d).mul(.02f + k * .028f));
            s.bar(BRASS_LIGHT, new Vector3f(c).sub(new Vector3f(d).mul(.006f)), new Vector3f(c).add(new Vector3f(d).mul(.006f)), .036f, .03f, diagonal);
        }
        s.bar(BRASS_LIGHT, new Vector3f(a).sub(new Vector3f(d).mul(.014f)), new Vector3f(a).add(new Vector3f(d).mul(.002f)), .04f, .034f, edge);
        s.disc(BRASS_LIGHT, new Vector3f(FIST_R).add(new Vector3f(d).mul(.082f)), d, .048f, .012f);
        s.disc(BRASS_DARK, new Vector3f(FIST_R).add(new Vector3f(d).mul(.09f)), d, .03f, .006f);
        s.bar(BRASS, new Vector3f(FIST_R).add(new Vector3f(d).mul(.09f)), new Vector3f(FIST_R).add(new Vector3f(d).mul(.13f)), .036f, .016f, edge);

        s.glow(true);
        List<Vector3f> blade = new ArrayList<>(), edgeLine = new ArrayList<>(), sides = new ArrayList<>();
        float[] heights = new float[7], edges = new float[7];
        for (int j = 0; j <= 6; j++) {
            float t = j / 6f;
            float h = lerp(.034f, .026f, t);
            Vector3f spine = bladePoint(t);
            blade.add(spine);
            edgeLine.add(new Vector3f(spine).add(new Vector3f(edge).mul(h / 2 - .004f)));
            sides.add(new Vector3f(edge));
            heights[j] = h;
            edges[j] = .008f;
        }
        s.ribbon(blade, sides, heights, .007f, (c, r) -> STEEL);
        s.ribbon(edgeLine, sides, edges, .0085f, (c, r) -> EDGE);
        Vector3f tip = new Vector3f(blade.get(6)).add(new Vector3f(d).mul(.012f));
        Vector3f c1 = new Vector3f(d).add(edge).normalize(), c2 = new Vector3f(d).sub(edge).normalize();
        s.box(STEEL, tip, v(.02, .02, .007), new Matrix3f(c1, c2, new Vector3f(c1).cross(c2)).getNormalizedRotation(new Quaternionf()));
        s.glow(false);
    }

    /** A point along the blade's spine at rest, from the collar ({@code t} 0) to the tip (1): the sword trail follows these. */
    static Vector3f bladePoint(float t) {
        Vector3f d = SWORD;
        Vector3f edge = v(0, -1, 0).sub(new Vector3f(d).mul(-d.y)).normalize();
        Vector3f start = new Vector3f(FIST_R).add(new Vector3f(d).mul(.12f));
        return start.add(new Vector3f(d).mul(BLADE_LENGTH * t)).sub(new Vector3f(edge).mul(.035f * t * t));
    }

    /** The red capelet over her shoulders, under the fur, draped lower on her left breast. */
    private static void capelet(DisplaySculpt s) {
        s.part("CHEST");
        List<Ring> rings = List.of(Ring.level(2.075f, .212f, .156f, 0f, -.004f), Ring.level(1.99f, .252f, .184f, 0f, -.004f),
                Ring.level(1.90f, .270f, .198f, 0f, 0f));
        Shape drape = (c, r, a) -> {
            double bell = Math.max(0, Math.cos((a - .95) * 1.8));
            return new float[]{1f, r == 2 ? (float) (-.07 * bell * bell) : 0f};
        };
        s.loft(rings, 16, null, drape, (c, r) -> c % 4 == 0 ? CAPE_DARK : SCARLET, .012f);
    }

    /** The long dark crimson cape: wrapping her back and flanks, folded, ragged at the hem, gold knotwork low on it. */
    private static void cape(DisplaySculpt s) {
        s.part("CAPE");
        List<Ring> rings = CAPE_SURFACE.ringsDown();
        int n = rings.size();
        Span wrap = row -> {
            double h = lerp(1.30, 1.02, row / (double) (n - 1));
            return new double[]{h, Math.PI * 2 - h};
        };
        Shape folds = (c, r, a) -> {
            float k = r / (float) (n - 1);
            float rise = r == n - 1 ? .03f * noise(c) + (c % 4 == 0 ? .03f : 0f) : 0f;
            return new float[]{1f + .035f * k * (float) Math.sin(c * 1.15 + .4), rise};
        };
        s.loft(rings, 18, wrap, folds, (c, r) -> c == 0 || c == 17 ? SCARLET : c % 3 == 1 ? CAPE : CAPE_DARK, .014f);
        s.decal(CAPE_SURFACE, KNOT, Math.PI - .62, 1.24 / 21, .95f, .05f, .03f, .006f, Map.of('g', BRASS_LIGHT));
    }

    /**
     * Her long wavy hair: a mass at the nape, then twenty locks in two layers falling down her back,
     * each a lock with depth (two crossed ribbons) that stands off the cape and waves on its own,
     * the inner layer ending at her shoulder blades and the outer one near her waist, and a lock over
     * each shoulder.
     */
    private static void longHair(DisplaySculpt s) {
        s.part("HAIR");
        nape(s);
        int locks = 20;
        for (int i = 0; i < locks; i++) {
            boolean inner = i % 2 == 0;
            double a = Math.PI + lerp(-.78, .78, (i + .5) / locks) + .05 * (noise(i + 7) - .5);
            float end = inner ? 1.48f + .1f * noise(i) : 1.12f + .24f * noise(i + 3);
            float lift = inner ? .045f : .08f + .04f * noise(i + 5);
            float phase = i * 1.7f;
            List<Vector3f> spine = new ArrayList<>(), across = new ArrayList<>(), out = new ArrayList<>();
            float[] w = new float[7];
            for (int j = 0; j <= 6; j++) {
                float t = j / 6f;
                float y = lerp(2.31f, end, t);
                double aj = Math.PI + (a - Math.PI) * (1 - .35 * t) + .07 * Math.sin(t * 6.5 + phase);
                float lj = lift * Math.min(1f, t * 3) + .03f * t * (float) Math.cos(t * 6.5 + phase) + (j == 6 ? .03f : 0f);
                Vector3f p = hairPoint(aj, y, lj);
                spine.add(p);
                across.add(hairPoint(aj + .01, y, lj).sub(hairPoint(aj - .01, y, lj)));
                out.add(hairPoint(aj, y, lj + .01f).sub(p));
                w[j] = j == 6 ? .016f : lerp(inner ? .1f : .075f, .035f, t);
            }
            lock(s, spine, across, out, w, i % 5 == 0 ? HAIR_LIGHT : i % 3 == 1 ? HAIR_DARK : HAIR);
        }
        for (int side : new int[]{-1, 1}) {
            List<Vector3f> spine = new ArrayList<>(), across = new ArrayList<>(), out = new ArrayList<>();
            for (int j = 0; j <= 4; j++) {
                float t = j / 4f;
                double a = side * lerp(1.40, 1.12, t);
                float y = lerp(2.31f, 1.86f, t);
                Vector3f p = y > 2.06f ? Ring.level(y, .165f, .183f, 0f, .004f).at(a) : TORSO.point(a, y, .085f);
                spine.add(p);
                across.add(v(Math.cos(a), 0, -Math.sin(a)));
                out.add(v(Math.sin(a), 0, Math.cos(a)));
            }
            lock(s, spine, across, out, new float[]{.06f, .055f, .05f, .04f, .015f}, HAIR);
        }
    }

    /** A lock of hair: a flat ribbon and a narrower one across it, so it has depth from every side. */
    private static void lock(DisplaySculpt s, List<Vector3f> spine, List<Vector3f> across, List<Vector3f> out, float[] w, Material m) {
        s.ribbon(spine, across, w, .02f, (c, r) -> m);
        float[] fin = new float[w.length];
        for (int i = 0; i < w.length; i++) fin[i] = w[i] * .5f;
        s.ribbon(spine, out, fin, .018f, (c, r) -> m);
    }

    /** The hair under the helm's rim, down the back of her neck to her shoulders. */
    private static void nape(DisplaySculpt s) {
        s.loft(List.of(Ring.level(2.335f, .150f, .168f, 0f, .004f), Ring.level(2.20f, .150f, .160f, 0f, -.02f),
                        Ring.level(2.07f, .175f, .165f, 0f, -.045f)), 10,
                r -> new double[]{1.15, Math.PI * 2 - 1.15}, null, (c, r) -> c % 2 == 0 ? HAIR : HAIR_DARK, .02f);
    }

    /** Where hair lies at {@code angle} and height {@code y}: round her head above her shoulders, over the cape below. */
    private static Vector3f hairPoint(double angle, float y, float lift) {
        Vector3f onCape = CAPE_SURFACE.point(angle, Math.min(y, CAPE_SURFACE.top()), lift);
        if (y <= 2.04f) return onCape;
        float k = Math.min(1f, (y - 2.04f) / .27f);
        k = k * k * (3 - 2 * k);
        return onCape.lerp(Ring.level(y, .160f + lift, .178f + lift, 0f, 0f).at(angle), k);
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /** A lathe's ring at {@code y}, {@code by} blocks wider all round. */
    private static Ring grown(Lathe lathe, float y, float by) {
        Ring r = lathe.at(y);
        return new Ring(r.center(), r.u(), r.w(), r.ru() + by, r.rw() + by);
    }

    /** The horizontal direction round a lathe at {@code angle} and {@code y}. */
    private static Vector3f tangent(Lathe lathe, double angle, float y) {
        return lathe.point(angle + .01, y, 0f).sub(lathe.point(angle - .01, y, 0f));
    }

    private static float[] widths(int count, float width) {
        float[] w = new float[count];
        java.util.Arrays.fill(w, width);
        return w;
    }

    /** A loop of lace: a little diamond standing on the cloth at {@code at}, facing {@code out}. */
    private static void loop(DisplaySculpt s, Vector3f at, Vector3f out, float size) {
        Vector3f n = new Vector3f(out).normalize();
        Vector3f flatUp = v(0, 1, 0).sub(new Vector3f(n).mul(n.y));
        if (flatUp.lengthSquared() < 1e-6f) flatUp = perpendicular(n);
        flatUp.normalize();
        Vector3f side = new Vector3f(flatUp).cross(n);
        Vector3f c1 = new Vector3f(flatUp).add(side).normalize(), c2 = new Vector3f(flatUp).sub(side).normalize();
        Vector3f c3 = new Vector3f(c1).cross(c2).normalize();
        s.box(LACE, at, v(size * .7f, size * .7f, .006f), new Matrix3f(c1, c2, c3).getNormalizedRotation(new Quaternionf()));
    }

    // =====================================================================================
    // In the world
    // =====================================================================================

    /**
     * Spawns {@code pieces} at {@code feet}, facing {@code yaw}. The
     * pieces are not saved with the world, so a crash or a chunk unload never leaves a body behind.
     */
    public static List<BlockDisplay> spawn(List<Piece> pieces, Location feet, float yaw) {
        Location at = feet.clone();
        at.setYaw(yaw);
        at.setPitch(0f);
        List<BlockDisplay> out = new ArrayList<>(pieces.size());
        for (Piece piece : pieces) {
            out.add(at.getWorld().spawn(at, BlockDisplay.class, display -> {
                display.setBlock(piece.material().createBlockData());
                display.setTransformation(piece.transformation());
                display.setPersistent(false);
                if (piece.glow()) display.setBrightness(new Display.Brightness(15, 15));
                display.addScoreboardTag(TAG);
                display.addScoreboardTag(TAG + "_" + piece.part());
            }));
        }
        return out;
    }

    /** Shows her in front of a viewer for {@code ticks}, turned to face them. */
    public static void preview(Plugin plugin, Player viewer, long ticks) {
        List<BlockDisplay> body = spawn(PIECES, ahead(viewer), viewer.getLocation().getYaw() + 180f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> body.forEach(BlockDisplay::remove), ticks);
    }

    static void animate(BlockDisplay display, Transformation to, int ticks) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(to);
    }

    /** Four blocks ahead of the viewer, from the yaw alone, so looking up or down still puts her ahead. */
    private static Location ahead(Player viewer) {
        Location eye = viewer.getLocation();
        double yaw = Math.toRadians(eye.getYaw());
        return eye.clone().add(-Math.sin(yaw) * 4, 0, Math.cos(yaw) * 4);
    }
}
