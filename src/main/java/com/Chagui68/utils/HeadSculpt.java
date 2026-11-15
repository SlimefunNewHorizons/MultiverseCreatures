package com.Chagui68.utils;

import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.Material;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sculpts a model out of textured player heads. A box is cut into a grid of heads and each of its
 * faces is painted as one picture, eight pixels to a head, so a plate a few heads wide carries
 * pixel art no block has. Heads that wear the same pixels share one skin.
 *
 * <p>Units and axes are {@link DisplaySculpt}'s: blocks, the model facing {@code +z}, its right side
 * on {@code -x}. A face's picture is drawn as it is seen from outside the box, its rows from the
 * top. The top and the bottom are both drawn as seen from above by someone standing in front of the
 * model: its back at the top of the picture, its right on the left.</p>
 *
 * <p>A player head only shows a skin Mojang hosts, so a skin is named after its pixels and
 * {@link #pieces} takes the texture each name was uploaded as.</p>
 */
public final class HeadSculpt {

    /** How a piece names a skin nobody has uploaded: this, then the skin's name. */
    public static final String LOCAL = "local:";

    /** The picture on one face of a box: opaque RGB, rows from the top. */
    public static final class Art {
        public final int w, h;
        private final int[] px;

        private Art(int w, int h, int rgb) {
            this.w = w;
            this.h = h;
            this.px = new int[w * h];
            Arrays.fill(px, rgb);
        }

        /** The pixel at {@code x}, {@code y}, or -1 off the picture. */
        public int get(int x, int y) {
            return x < 0 || y < 0 || x >= w || y >= h ? -1 : px[y * w + x];
        }

        /** Paints one pixel; off the picture it does nothing. */
        public Art set(int x, int y, int rgb) {
            if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = rgb;
            return this;
        }

        public Art fill(int rgb) {
            Arrays.fill(px, rgb);
            return this;
        }

        public Art rect(int x, int y, int width, int height, int rgb) {
            for (int j = y; j < y + height; j++) for (int i = x; i < x + width; i++) set(i, j, rgb);
            return this;
        }

        /** Pixel art from {@code x}, {@code y}: a character is its ink of {@code inks}, and any other leaves its pixel alone. */
        public Art draw(int x, int y, Map<Character, Integer> inks, String... rows) {
            for (int j = 0; j < rows.length; j++) {
                for (int i = 0; i < rows[j].length(); i++) {
                    Integer rgb = inks.get(rows[j].charAt(i));
                    if (rgb != null) set(x + i, y + j, rgb);
                }
            }
            return this;
        }

        /** Copies the left half over the right half, mirrored: a face drawn once that is the same on both sides. */
        public Art mirror() {
            for (int j = 0; j < h; j++) for (int i = 0; i < w / 2; i++) px[j * w + w - 1 - i] = px[j * w + i];
            return this;
        }

        /** Takes another picture's pixels, mirrored left to right or not: the two faces of a plate show each other mirrored. */
        public Art copy(Art from, boolean mirrored) {
            for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) set(i, j, from.get(mirrored ? from.w - 1 - i : i, j));
            return this;
        }
    }

    /** A box of heads and the six pictures on it. */
    public static final class Box {
        private final String part;
        private final Vector3f center, size;
        private final Quaternionf rotation;
        private final int nx, ny, nz, inside;
        public final Art front, back, left, right, top, bottom;

        private Box(String part, Vector3f center, Vector3f size, Quaternionf rotation, int nx, int ny, int nz, int rgb) {
            this.part = part;
            this.center = new Vector3f(center);
            this.size = new Vector3f(size);
            this.rotation = new Quaternionf(rotation);
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.inside = rgb;
            front = new Art(nx * 8, ny * 8, rgb);
            back = new Art(nx * 8, ny * 8, rgb);
            left = new Art(nz * 8, ny * 8, rgb);
            right = new Art(nz * 8, ny * 8, rgb);
            top = new Art(nx * 8, nz * 8, rgb);
            bottom = new Art(nx * 8, nz * 8, rgb);
        }

        /** The four pictures round the box. */
        public List<Art> sides() {
            return List.of(front, back, left, right);
        }

        /** All six. */
        public List<Art> faces() {
            return List.of(front, back, left, right, top, bottom);
        }

        /**
         * Moves the box to the model's other side: its mirror image, turned half round on its own
         * axis so it wears the very same heads. Its {@code right} still looks out, and its
         * {@code back} is what shows at the front.
         */
        public Box flip() {
            center.x = -center.x;
            rotation.set(rotation.x, -rotation.y, -rotation.z, rotation.w).rotateY((float) Math.PI);
            return this;
        }

        private void cut(List<Piece> out, Map<String, int[]> skins, Map<String, String> textures) {
            Vector3f head = new Vector3f(size.x / nx, size.y / ny, size.z / nz);
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    for (int k = 0; k < nz; k++) {
                        if (i > 0 && i < nx - 1 && j > 0 && j < ny - 1 && k > 0 && k < nz - 1) continue;
                        int[] skin = new int[64 * 64];
                        int row = (ny - 1 - j) * 8;
                        // The game draws a head's skin with its face on -z and its right ear on +x.
                        tile(skin, 24, 8, k == nz - 1 ? front : null, i * 8, row, false);
                        tile(skin, 8, 8, k == 0 ? back : null, (nx - 1 - i) * 8, row, false);
                        tile(skin, 0, 8, i == nx - 1 ? left : null, (nz - 1 - k) * 8, row, false);
                        tile(skin, 16, 8, i == 0 ? right : null, k * 8, row, false);
                        // Its top and bottom are both held as seen from above with the face at the foot of the picture.
                        tile(skin, 8, 0, j == ny - 1 ? top : null, i * 8, k * 8, true);
                        tile(skin, 16, 0, j == 0 ? bottom : null, i * 8, k * 8, true);
                        String name = name(skin);
                        skins.putIfAbsent(name, skin);
                        Vector3f local = new Vector3f((i + .5f) / nx - .5f, (j + .5f) / ny - .5f, (k + .5f) / nz - .5f).mul(size);
                        out.add(new Piece(part, Material.PLAYER_HEAD, rotation.transform(local).add(center), new Quaternionf(rotation),
                                new Vector3f(head), false, textures.getOrDefault(name, LOCAL + name)));
                    }
                }
            }
        }

        /** Copies the 8x8 tile of {@code art} at {@code ax}, {@code ay} to the skin at {@code sx}, {@code sy}; a face inside the box takes its plain colour. */
        private void tile(int[] skin, int sx, int sy, Art art, int ax, int ay, boolean turned) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int rgb = art == null ? inside : turned ? art.get(ax + 7 - x, ay + 7 - y) : art.get(ax + x, ay + y);
                    skin[(sy + y) * 64 + sx + x] = 0xff000000 | rgb;
                }
            }
        }
    }

    private final List<Box> boxes = new ArrayList<>();
    private final Map<String, int[]> skins = new LinkedHashMap<>();
    private String part = "BODY";

    /** The part the next boxes belong to. */
    public HeadSculpt part(String part) {
        this.part = part;
        return this;
    }

    /** A box cut into {@code nx} by {@code ny} by {@code nz} heads, every face painted {@code rgb} to begin with. */
    public Box box(Vector3f center, Vector3f size, Quaternionf rotation, int nx, int ny, int nz, int rgb) {
        Box box = new Box(part, center, size, rotation, nx, ny, nz, rgb);
        boxes.add(box);
        return box;
    }

    /** A bar of {@code heads} heads from {@code a} to {@code b}: {@code width} along {@code side}, {@code depth} across both, as {@link DisplaySculpt#bar}. */
    public Box bar(Vector3f a, Vector3f b, float width, float depth, Vector3f side, int heads, int rgb) {
        Vector3f along = new Vector3f(b).sub(a);
        float length = along.length();
        along.div(length);
        Vector3f wide = new Vector3f(side).sub(new Vector3f(along).mul(side.dot(along)));
        if (wide.lengthSquared() < 1e-8f) wide = DisplaySculpt.perpendicular(along);
        wide.normalize();
        Quaternionf turn = new Matrix3f(wide, along, new Vector3f(wide).cross(along)).getNormalizedRotation(new Quaternionf());
        return box(new Vector3f(a).add(b).mul(.5f), new Vector3f(width, length, depth), turn, 1, heads, 1, rgb);
    }

    /**
     * Every head as a piece. A skin is named after its pixels; {@code textures} holds the texture
     * each name was uploaded as, and a name it lacks goes out as {@link #LOCAL} and the name.
     */
    public List<Piece> pieces(Map<String, String> textures) {
        List<Piece> out = new ArrayList<>();
        skins.clear();
        for (Box box : boxes) box.cut(out, skins, textures);
        return out;
    }

    /** The skins of the last {@link #pieces}: name to the 64x64 ARGB pixels of a player skin, only its head painted. */
    public Map<String, int[]> skins() {
        return skins;
    }

    /** A skin's name: the first sixteen hex digits of the SHA-256 of its head's pixels. */
    private static String name(int[] skin) {
        ByteBuffer bytes = ByteBuffer.allocate(32 * 16 * 4);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 32; x++) bytes.putInt(skin[y * 64 + x]);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes.array());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02x", digest[i]));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
