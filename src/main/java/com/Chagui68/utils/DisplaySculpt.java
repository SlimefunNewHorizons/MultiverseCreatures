package com.Chagui68.utils;

import org.bukkit.Material;
import org.bukkit.util.Transformation;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Sculpts a model out of block displays the way a modeller would: round surfaces (a torso, a skirt,
 * a sleeve, a helm) lofted through rings, curved ribbons (hair, feathers, straps, petals), discs and
 * decals laid on a curved surface. Every quad of a surface becomes one thin block turned to lie on
 * it, so a body is drawn as a shell of small tiles instead of a few big cubes.
 *
 * <p>Units are blocks. Models face {@code +z} with their feet on the origin, so the right side is
 * {@code -x}. Nothing here touches the server: the result is a list of {@link Piece}s.</p>
 */
public final class DisplaySculpt {

    /** One block display: the part it belongs to, its block, its centre, its turn and its size. */
    public record Piece(String part, Material material, Vector3f center, Quaternionf rotation, Vector3f size,
                        boolean glow) {

        /** The display transform that draws this piece from the model's origin. */
        public Transformation transformation() {
            Vector3f half = new Vector3f(size).mul(0.5f);
            Vector3f translation = new Vector3f(center).sub(rotation.transform(half, new Vector3f()));
            return new Transformation(translation, new Quaternionf(rotation), new Vector3f(size), new Quaternionf());
        }
    }

    /** A ring of a loft: an ellipse round {@code center} in the plane of {@code u} and {@code w}. */
    public record Ring(Vector3f center, Vector3f u, Vector3f w, float ru, float rw) {

        /** A level ring round the y axis: angle 0 is the front (+z), a quarter turn is her left (+x). */
        public static Ring level(float y, float ru, float rw, float cx, float cz) {
            return new Ring(new Vector3f(cx, y, cz), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), ru, rw);
        }

        public Vector3f at(double angle) {
            return new Vector3f(center)
                    .add(new Vector3f(u).mul((float) (ru * Math.sin(angle))))
                    .add(new Vector3f(w).mul((float) (rw * Math.cos(angle))));
        }

        /** The ring's axis: up for a level ring. */
        public Vector3f axis() {
            return new Vector3f(w).cross(u).normalize();
        }
    }

    /** Rings along a straight limb from {@code a} to {@code b}, one per radius, frame turned by {@code hint}. */
    public static List<Ring> along(Vector3f a, Vector3f b, float[] ru, float[] rw, Vector3f hint) {
        Vector3f d = new Vector3f(b).sub(a).normalize();
        Vector3f u = new Vector3f(d).cross(hint);
        if (u.lengthSquared() < 1e-6f) u = new Vector3f(d).cross(new Vector3f(1, 0, 0));
        u.normalize();
        Vector3f w = new Vector3f(u).cross(d).normalize();
        List<Ring> rings = new ArrayList<>(ru.length);
        for (int i = 0; i < ru.length; i++) {
            float t = ru.length == 1 ? 0f : i / (float) (ru.length - 1);
            rings.add(new Ring(new Vector3f(a).lerp(b, t), u, w, ru[i], rw[i]));
        }
        return rings;
    }

    /**
     * A surface of revolution round the y axis, from its rings' heights, radii and centres: it can be
     * lofted and anything can be laid on it with {@link #point}.
     */
    public static final class Lathe {
        private final float[][] rings;

        /** Rows of {y, ru, rw, cx, cz}, in any order. */
        public Lathe(float[][] rows) {
            rings = Arrays.stream(rows).sorted(Comparator.comparingDouble(r -> r[0])).toArray(float[][]::new);
        }

        public List<Ring> rings() {
            List<Ring> out = new ArrayList<>(rings.length);
            for (float[] r : rings) out.add(Ring.level(r[0], r[1], r[2], r[3], r[4]));
            return out;
        }

        /** The rings from the top down, the order a hanging cloth is lofted in. */
        public List<Ring> ringsDown() {
            List<Ring> out = rings();
            java.util.Collections.reverse(out);
            return out;
        }

        public float top() {
            return rings[rings.length - 1][0];
        }

        public float bottom() {
            return rings[0][0];
        }

        /** The ring at height {@code y}, between the two it lies between. */
        public Ring at(float y) {
            float cy = Math.max(rings[0][0], Math.min(top(), y));
            for (int i = 0; i < rings.length - 1; i++) {
                float[] lo = rings[i], hi = rings[i + 1];
                if (cy <= hi[0]) {
                    float t = hi[0] == lo[0] ? 0f : (cy - lo[0]) / (hi[0] - lo[0]);
                    return Ring.level(cy, lerp(lo[1], hi[1], t), lerp(lo[2], hi[2], t),
                            lerp(lo[3], hi[3], t), lerp(lo[4], hi[4], t));
                }
            }
            float[] r = rings[rings.length - 1];
            return Ring.level(r[0], r[1], r[2], r[3], r[4]);
        }

        /** A point on the surface at {@code angle} and height {@code y}, {@code lift} blocks out from it. */
        public Vector3f point(double angle, float y, float lift) {
            Ring ring = at(y);
            Vector3f p = ring.at(angle);
            Vector3f out = new Vector3f((float) (Math.sin(angle) * ring.rw()), 0f, (float) (Math.cos(angle) * ring.ru()));
            if (out.lengthSquared() > 1e-9f) p.add(out.normalize().mul(lift));
            return p;
        }
    }

    /** Which block a quad of a loft or a ribbon is drawn with: by column and row. */
    public interface Paint {
        Material at(int column, int row);
    }

    /** How a loft departs from its rings, per column and row: {radius multiplier, rise along the axis}. */
    public interface Shape {
        float[] at(int column, int row, double angle);
    }

    /** The angles a loft spans at one ring: {from, to}, so an opening can widen as it goes down. */
    public interface Span {
        double[] at(int row);
    }

    private final List<Piece> pieces = new ArrayList<>();
    private String part = "BODY";
    private boolean glow;

    /** The part the next pieces belong to. */
    public DisplaySculpt part(String part) {
        this.part = part;
        return this;
    }

    /** Whether the next pieces are drawn fully lit. */
    public DisplaySculpt glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public List<Piece> pieces() {
        return pieces;
    }

    /** A plain box: centre, size along its own axes, and its turn. */
    public void box(Material m, Vector3f center, Vector3f size, Quaternionf rotation) {
        pieces.add(new Piece(part, m, new Vector3f(center), new Quaternionf(rotation), new Vector3f(size), glow));
    }

    /** A bar from {@code a} to {@code b}: {@code width} along {@code side}, {@code depth} across both. */
    public void bar(Material m, Vector3f a, Vector3f b, float width, float depth, Vector3f side) {
        Vector3f v = new Vector3f(b).sub(a);
        float len = v.length();
        if (len < 1e-5f) return;
        v.div(len);
        Vector3f u = new Vector3f(side).sub(new Vector3f(v).mul(side.dot(v)));
        if (u.lengthSquared() < 1e-8f) u = perpendicular(v);
        u.normalize();
        Vector3f n = new Vector3f(u).cross(v);
        Quaternionf q = new Matrix3f(u, v, n).getNormalizedRotation(new Quaternionf());
        pieces.add(new Piece(part, m, new Vector3f(a).add(b).mul(0.5f), q, new Vector3f(width, len, depth), glow));
    }

    /**
     * A tile spanning the quad {@code b0 b1} (its lower edge) to {@code t0 t1} (its upper edge), {@code
     * thickness} deep. With {@code outward} set, the tile's outer face lies on the quad and the tile
     * sinks inwards; without it the tile is centred on the quad.
     */
    public void quad(Material m, Vector3f b0, Vector3f b1, Vector3f t0, Vector3f t1, float thickness, Vector3f outward) {
        Vector3f mb = new Vector3f(b0).add(b1).mul(0.5f);
        Vector3f mt = new Vector3f(t0).add(t1).mul(0.5f);
        Vector3f v = new Vector3f(mt).sub(mb);
        float h = v.length();
        if (h < 1e-5f) return;
        v.div(h);
        Vector3f u = new Vector3f(b1).sub(b0).add(new Vector3f(t1).sub(t0)).mul(0.5f);
        u.sub(new Vector3f(v).mul(u.dot(v)));
        if (u.lengthSquared() < 1e-10f) return;
        u.normalize();
        // A touch wider than the quad, so neighbouring tiles close their seams.
        float width = Math.max(b0.distance(b1), t0.distance(t1)) * 1.06f + 0.002f;
        Vector3f n = new Vector3f(u).cross(v);
        if (outward != null && n.dot(outward) < 0) {
            n.negate();
            u.negate();
        }
        Vector3f c = new Vector3f(mb).add(mt).mul(0.5f);
        if (outward != null) c.sub(new Vector3f(n).mul(thickness / 2));
        Quaternionf q = new Matrix3f(u, v, n).getNormalizedRotation(new Quaternionf());
        pieces.add(new Piece(part, m, c, q, new Vector3f(width, h * 1.02f, thickness), glow));
    }

    /**
     * A surface through {@code rings}, {@code columns} tiles round each, spanning the angles {@code
     * span} gives per ring (null: all the way round). {@code shape} and {@code paint} may vary it.
     */
    public void loft(List<Ring> rings, int columns, Span span, Shape shape, Paint paint, float thickness) {
        for (int r = 0; r < rings.size() - 1; r++) {
            Ring lo = rings.get(r), hi = rings.get(r + 1);
            double[] sLo = span == null ? new double[]{0, Math.PI * 2} : span.at(r);
            double[] sHi = span == null ? new double[]{0, Math.PI * 2} : span.at(r + 1);
            for (int c = 0; c < columns; c++) {
                double a0 = lerp(sLo[0], sLo[1], c / (double) columns), a1 = lerp(sLo[0], sLo[1], (c + 1) / (double) columns);
                double b0 = lerp(sHi[0], sHi[1], c / (double) columns), b1 = lerp(sHi[0], sHi[1], (c + 1) / (double) columns);
                Vector3f p00 = shaped(lo, a0, c, r, shape), p01 = shaped(lo, a1, c, r, shape);
                Vector3f p10 = shaped(hi, b0, c, r + 1, shape), p11 = shaped(hi, b1, c, r + 1, shape);
                Vector3f mid = new Vector3f(p00).add(p01).add(p10).add(p11).mul(0.25f);
                Vector3f axisPoint = new Vector3f(lo.center()).add(hi.center()).mul(0.5f);
                Vector3f axis = new Vector3f(hi.center()).sub(lo.center());
                Vector3f out = new Vector3f(mid).sub(axisPoint);
                if (axis.lengthSquared() > 1e-10f) {
                    axis.normalize();
                    out.sub(new Vector3f(axis).mul(out.dot(axis)));
                }
                quad(paint.at(c, r), p00, p01, p10, p11, thickness, out);
            }
        }
    }

    private static Vector3f shaped(Ring ring, double angle, int column, int row, Shape shape) {
        Vector3f p = ring.at(angle);
        if (shape == null) return p;
        float[] s = shape.at(column, row, angle);
        Vector3f radial = new Vector3f(p).sub(ring.center()).mul(s[0]);
        return new Vector3f(ring.center()).add(radial).add(ring.axis().mul(s[1]));
    }

    /**
     * A ribbon down {@code spine}: at each point it is {@code widths[i]} wide along {@code sides[i]}
     * and {@code thickness} deep, centred on the spine. Paint takes the segment as its row.
     */
    public void ribbon(List<Vector3f> spine, List<Vector3f> sides, float[] widths, float thickness, Paint paint) {
        for (int i = 0; i < spine.size() - 1; i++) {
            Vector3f s0 = new Vector3f(sides.get(i)).normalize().mul(widths[i] / 2);
            Vector3f s1 = new Vector3f(sides.get(i + 1)).normalize().mul(widths[i + 1] / 2);
            Vector3f p0 = spine.get(i), p1 = spine.get(i + 1);
            quad(paint.at(0, i), new Vector3f(p0).sub(s0), new Vector3f(p0).add(s0),
                    new Vector3f(p1).sub(s1), new Vector3f(p1).add(s1), thickness, null);
        }
    }

    /** A round disc of radius {@code r} facing {@code normal}: an octagon of four crossed tiles. */
    public void disc(Material m, Vector3f center, Vector3f normal, float r, float thickness) {
        Vector3f n = new Vector3f(normal).normalize();
        Vector3f e1 = perpendicular(n);
        Vector3f e2 = new Vector3f(n).cross(e1);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 4;
            Vector3f da = new Vector3f(e1).mul((float) Math.cos(a)).add(new Vector3f(e2).mul((float) Math.sin(a)));
            Vector3f db = new Vector3f(n).cross(da);
            Quaternionf q = new Matrix3f(da, db, n).getNormalizedRotation(new Quaternionf());
            pieces.add(new Piece(part, m, new Vector3f(center), q, new Vector3f(2 * r, 0.828f * r, thickness), glow));
        }
    }

    /**
     * Pixel art laid on a lathe's surface: column {@code c} of row {@code r} covers the angles from
     * {@code a0} onwards {@code da} each, and the heights from {@code top} down {@code dy} each. Runs of
     * one character are drawn as one tile; characters missing from {@code palette} are left empty.
     */
    public void decal(Lathe surface, String[] art, double a0, double da, float top, float dy, float lift,
                      float thickness, java.util.Map<Character, Material> palette) {
        for (int r = 0; r < art.length; r++) {
            String row = art[r];
            int c = 0;
            while (c < row.length()) {
                char ch = row.charAt(c);
                int end = c;
                while (end < row.length() && row.charAt(end) == ch) end++;
                Material m = palette.get(ch);
                if (m != null) {
                    double from = a0 + c * da, to = a0 + end * da;
                    float y0 = top - (r + 1) * dy, y1 = top - r * dy;
                    quad(m, surface.point(from, y0, lift), surface.point(to, y0, lift),
                            surface.point(from, y1, lift), surface.point(to, y1, lift), thickness,
                            surface.point((from + to) / 2, (y0 + y1) / 2, 1f).sub(surface.point((from + to) / 2, (y0 + y1) / 2, 0f)));
                }
                c = end;
            }
        }
    }

    // ---- vector helpers for the models ----

    public static Vector3f v(double x, double y, double z) {
        return new Vector3f((float) x, (float) y, (float) z);
    }

    /** Any unit vector at right angles to {@code n}. */
    public static Vector3f perpendicular(Vector3f n) {
        Vector3f helper = Math.abs(n.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        return new Vector3f(n).cross(helper).normalize();
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** A deterministic value in [0, 1) for {@code i}, so a model looks hand-made yet never changes. */
    public static float noise(int i) {
        double x = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
        return (float) (x - Math.floor(x));
    }
}
