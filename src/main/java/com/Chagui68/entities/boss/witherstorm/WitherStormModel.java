package com.Chagui68.entities.boss.witherstorm;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One form of the Wither Storm, drawn the way Cracker's Wither Storm Mod draws it: the same part
 * tree, the same boxes and the same animation maths, so a box sits, turns and opens exactly where
 * the mod puts it.
 *
 * <p>The forms are the text files in {@code resources/witherstorm}, written by
 * {@code tools/wither-storm} from the mod's own model classes. A form is a tree of parts (a pivot
 * and a rest rotation each, in the mod's sixteenths of a block with y pointing down) whose boxes are
 * drawn with one block each; groups say which subtree is rendered under which extra scale, the way
 * the mod's renderer scales its mass, heads and tentacles differently.</p>
 *
 * <p>{@link #place} turns a {@link Pose} into one display transformation per box, in the space of a
 * display entity standing at the storm's feet and turned to its yaw: the display's own rotation does
 * the yaw, so a body that only turns never has to be sent again. Nothing here touches the server.</p>
 */
public final class WitherStormModel {

    /** How the mod renders a subtree: the mass, a head, a tentacle, or the wither it grew from. */
    public enum Kind { BASE, MASS, HEAD, TENTACLE }

    /** A model part: its parent, its pivot and its rest rotation (radians, applied Z, Y, X). */
    public record Part(String path, int parent, float x, float y, float z, float xRot, float yRot, float zRot) {
    }

    /** A box of a part, drawn with one block (or a wither skull for the old wither heads). */
    public record Box(int part, float x, float y, float z, float w, float h, float d, String material, boolean glow) {

        public boolean skull() {
            return "SKULL".equals(material);
        }

        /** A block whose faces differ, so it has to stand the right way up: the command block. */
        public boolean oriented() {
            return "COMMAND_BLOCK".equals(material);
        }
    }

    /** A subtree rendered on its own, under its own scale and tilt. */
    public record Group(Kind kind, int root, float scale, float rotX) {
    }

    /** A jawed head: the part it turns about, its two jaws and its animation phase. */
    public record Head(int index, int root, int upperJaw, int lowerJaw, float animOffset) {
    }

    /** A six-segment tentacle and the numbers that make it sway. */
    public record Tentacle(int base, int[] segments, float speed, float offset, float xRot, float yRot,
                           float xAng, float yAng, float reach) {
    }

    /** Where a box goes this frame: the display transformation, before the display's yaw. */
    public record Placement(Vector3f translation, Quaternionf rotation, Vector3f scale) {
    }

    /**
     * Everything the animation reads. Angles are degrees, as the server keeps them: head yaw is
     * relative to the body, pitch is positive looking down.
     */
    public static final class Pose {
        /** Ticks the storm has lived; the idle breathing and jaw sway run on it. */
        public float ticks;
        /** The tentacles' own clock, which the mod speeds up while the storm moves. */
        public float tentacleTicks;
        /** Body tilt, positive nose down, as the mod tilts a climbing or diving storm. */
        public float bodyPitch;
        /** Per head index: yaw relative to the body, pitch, how far the mouth is open (0..2), roll. */
        public final float[] headYaw = new float[3];
        public final float[] headPitch = new float[3];
        public final float[] mouth = new float[3];
        public final float[] roll = new float[3];
        /** How far a dead storm's jaws hang broken (0..1.5); 0 while it lives. */
        public float brokenJaw;
    }

    /** The mod renders every storm at twice its model size, lifted by 1.501 model units. */
    static final float MOD_SCALE = 2.0f;
    static final float MOD_LIFT = 1.501f;
    private static final float DEG = (float) (Math.PI / 180.0);

    private final String name;
    private final List<Part> parts;
    private final List<Box> boxes;
    private final List<Group> groups;
    private final List<Head> heads;
    private final List<Tentacle> tentacles;
    private final Map<String, Integer> index;

    private WitherStormModel(String name, List<Part> parts, List<Box> boxes, List<Group> groups, List<Head> heads,
                             List<Tentacle> tentacles, Map<String, Integer> index) {
        this.name = name;
        this.parts = List.copyOf(parts);
        this.boxes = List.copyOf(boxes);
        this.groups = List.copyOf(groups);
        this.heads = List.copyOf(heads);
        this.tentacles = List.copyOf(tentacles);
        this.index = Map.copyOf(index);
    }

    public String name() {
        return name;
    }

    public List<Part> parts() {
        return parts;
    }

    public List<Box> boxes() {
        return boxes;
    }

    public List<Group> groups() {
        return groups;
    }

    public List<Head> heads() {
        return heads;
    }

    public List<Tentacle> tentacles() {
        return tentacles;
    }

    /** The part with that path, or -1. */
    public int part(String path) {
        return index.getOrDefault(path, -1);
    }

    // ------------------------------------------------------------------ reading

    /** Reads a form shipped in the plugin jar, by its file name without the extension. */
    public static WitherStormModel load(String form) {
        String resource = "/witherstorm/" + form + ".txt";
        try (InputStream in = WitherStormModel.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("no Wither Storm form " + resource);
            }
            return parse(form, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + resource, e);
        }
    }

    public static WitherStormModel parse(String name, String text) {
        List<Part> parts = new ArrayList<>();
        List<Box> boxes = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        List<String[]> groupLines = new ArrayList<>();
        List<String[]> headLines = new ArrayList<>();
        List<String[]> tentacleLines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\\s+");
            switch (f[0]) {
                case "part" -> {
                    int slash = f[1].lastIndexOf('/');
                    int parent = slash < 0 ? -1 : index.getOrDefault(f[1].substring(0, slash), -1);
                    index.put(f[1], parts.size());
                    parts.add(new Part(f[1], parent, num(f[2]), num(f[3]), num(f[4]), num(f[5]), num(f[6]), num(f[7])));
                }
                case "box" -> {
                    Integer part = index.get(f[1]);
                    if (part == null) {
                        throw new IllegalArgumentException(name + ": box of unknown part " + f[1]);
                    }
                    boxes.add(new Box(part, num(f[2]), num(f[3]), num(f[4]), num(f[5]), num(f[6]), num(f[7]),
                            f[8], "1".equals(f[9])));
                }
                case "group" -> groupLines.add(f);
                case "head" -> headLines.add(f);
                case "tentacle" -> tentacleLines.add(f);
                default -> throw new IllegalArgumentException(name + ": unknown line " + line);
            }
        }
        List<Group> groups = new ArrayList<>();
        for (String[] f : groupLines) {
            groups.add(new Group(Kind.valueOf(f[1].toUpperCase(Locale.ROOT)), require(index, name, f[2]), num(f[3]), num(f[4])));
        }
        List<Head> heads = new ArrayList<>();
        for (String[] f : headLines) {
            heads.add(new Head(Integer.parseInt(f[1]), require(index, name, f[2]),
                    require(index, name, f[2] + "/upperJaw"), require(index, name, f[2] + "/lowerJaw"), num(f[3])));
        }
        List<Tentacle> tentacles = new ArrayList<>();
        for (String[] f : tentacleLines) {
            int[] segments = new int[6];
            String path = f[1];
            for (int s = 0; s < 6; s++) {
                path = path + "/segment" + (s + 1);
                segments[s] = require(index, name, path);
            }
            int base = require(index, name, f[1]);
            tentacles.add(new Tentacle(base, segments, num(f[3]), num(f[4]), num(f[5]),
                    num(f[6]), num(f[7]), num(f[8]), num(f[9])));
            // Each tentacle is rendered on its own, at its own scale (and the swollen hunchback's tilt).
            groups.add(new Group(Kind.TENTACLE, base, num(f[2]), num(f[10])));
        }
        return new WitherStormModel(name, parts, boxes, groups, heads, tentacles, index);
    }

    private static int require(Map<String, Integer> index, String name, String path) {
        Integer part = index.get(path);
        if (part == null) {
            throw new IllegalArgumentException(name + ": no part " + path);
        }
        return part;
    }

    private static float num(String s) {
        return Float.parseFloat(s);
    }

    // ------------------------------------------------------------------ posing

    /**
     * The model-to-display matrix: the mod's renderer turned half round (the display's yaw does the
     * rest), tilted, flipped upright, doubled and lifted. {@code size} scales the whole storm.
     */
    public static Matrix4f root(float bodyPitch, float size) {
        return new Matrix4f()
                .rotateY((float) Math.PI)
                .rotateX(bodyPitch * DEG)
                .scale(-1, -1, 1)
                .scale(MOD_SCALE * size)
                .translate(0, -MOD_LIFT, 0);
    }

    /** The matrix of every part this frame, or null for parts no group renders. */
    public Matrix4f[] partMatrices(Pose pose, float size) {
        float[][] rotations = new float[parts.size()][];
        float[][] pivots = new float[parts.size()][];
        animate(pose, rotations, pivots);

        Matrix4f root = root(pose.bodyPitch, size);
        Matrix4f[] matrices = new Matrix4f[parts.size()];
        for (Group group : groups) {
            Matrix4f base = new Matrix4f(root).scale(group.scale()).rotateX(group.rotX() * DEG);
            fill(group.root(), base, rotations, pivots, matrices);
        }
        return matrices;
    }

    /** One placement per box, in box order. */
    public Placement[] place(Pose pose, float size) {
        return place(partMatrices(pose, size));
    }

    /** One placement per box, from part matrices already worked out for this frame. */
    public Placement[] place(Matrix4f[] matrices) {
        Placement[] out = new Placement[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            Matrix4f m = matrices[box.part()];
            out[i] = m == null ? null : placement(box, m);
        }
        return out;
    }

    /** Where a point of a part (in its sixteenths) ends up this frame, in display space. */
    public static Vector3f point(Matrix4f partMatrix, float x, float y, float z) {
        return partMatrix.transformPosition(new Vector3f(x / 16f, y / 16f, z / 16f));
    }

    private static Placement placement(Box box, Matrix4f part) {
        float scale = part.getScale(new Vector3f()).x;
        // The part matrix carries the storm's scale, so its columns are not unit length.
        Quaternionf rotation = part.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f size = new Vector3f(box.w(), box.h(), box.d()).mul(scale / 16f);
        if (box.skull()) {
            // A skull item hangs below its anchor, half a block wide, its face on -z. The flip that
            // stands the model upright would turn it upside down, so it is turned back round first.
            Quaternionf upright = new Quaternionf(rotation).rotateZ((float) Math.PI);
            Vector3f centre = part.transformPosition(new Vector3f(
                    (box.x() + box.w() / 2f) / 16f, (box.y() + box.h() / 2f) / 16f, (box.z() + box.d() / 2f) / 16f));
            float side = (size.x + size.y + size.z) / 3f;
            Vector3f top = upright.transform(new Vector3f(0, side / 2f, 0)).add(centre);
            return new Placement(top, upright, new Vector3f(side * 2f));
        }
        Vector3f corner = part.transformPosition(new Vector3f(box.x() / 16f, box.y() / 16f, box.z() / 16f));
        if (box.oriented()) {
            // Same flip as the skulls: turned back round on its own z axis, the block fills the same
            // space but stands the right way up, its front (north, -z) towards the storm's front.
            Quaternionf upright = new Quaternionf(rotation).rotateZ((float) Math.PI);
            corner.add(rotation.transform(new Vector3f(size.x, size.y, 0)));
            return new Placement(corner, upright, size);
        }
        return new Placement(corner, rotation, size);
    }

    private void fill(int part, Matrix4f parent, float[][] rotations, float[][] pivots, Matrix4f[] out) {
        Part p = parts.get(part);
        float[] pivot = pivots[part] != null ? pivots[part] : new float[]{p.x(), p.y(), p.z()};
        float[] rot = rotations[part] != null ? rotations[part] : new float[]{p.xRot(), p.yRot(), p.zRot()};
        Matrix4f m = new Matrix4f(parent).translate(pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f);
        if (rot[0] != 0 || rot[1] != 0 || rot[2] != 0) {
            m.rotate(new Quaternionf().rotationZYX(rot[2], rot[1], rot[0]));
        }
        out[part] = m;
        for (int child = 0; child < parts.size(); child++) {
            if (parts.get(child).parent() == part) {
                fill(child, m, rotations, pivots, out);
            }
        }
    }

    /** The mod's setupAnimations, writing the rotations (and the tail's pivot) it overrides. */
    private void animate(Pose pose, float[][] rotations, float[][] pivots) {
        float idle = (float) Math.cos(pose.ticks * 0.1f);
        for (Head head : heads) {
            int i = head.index();
            rotations[head.root()] = new float[]{-pose.headPitch[i] * DEG, (float) Math.PI + pose.headYaw[i] * DEG, pose.roll[i]};
            float hinge = pose.mouth[i] * 0.3f;
            float sway = (float) Math.cos((pose.ticks + head.animOffset()) * 0.1f);
            float lower = (float) Math.cos(hinge) * 10f - 10f + (0.065f + 0.02f * sway) * (float) Math.PI - 0.5f;
            float broken = (float) Math.cos(pose.brokenJaw * 0.3f) * 10f - 10f;
            rotations[head.lowerJaw()] = new float[]{lower, 0, (i % 2 == 0 ? 1 : -1) * broken};
        }
        for (Tentacle t : tentacles) {
            float clock = pose.tentacleTicks + t.offset() * 10f;
            float f = (float) Math.cos(clock * t.speed() * 0.1f) * t.reach();
            float s = (float) Math.sin(clock * t.speed() / 2f * 0.1f) * t.reach();
            rotations[t.base()] = new float[]{f * s * 0.05f + t.xRot(), s * f * 0.05f + t.yRot(), 0};
            float[] bend = {-0.1f, 0.1f, 0.075f, 0.05f, 0.1f, 0.1f};
            for (int seg = 0; seg < 6; seg++) {
                rotations[t.segments()[seg]] = new float[]{f * bend[seg] + (seg == 0 ? 0 : t.xAng()), seg == 0 ? 0 : t.yAng(), 0};
            }
        }
        int ribcage = part("witherBase/ribcage");
        if (ribcage >= 0) {
            float rib = (0.065f + 0.05f * idle) * (float) Math.PI;
            rotations[ribcage] = new float[]{rib, 0, 0};
            int tail = part("witherBase/tail");
            if (tail >= 0) {
                pivots[tail] = new float[]{-2f, 6.9f + (float) Math.cos(rib) * 10f, -0.5f + (float) Math.sin(rib) * 10f};
                rotations[tail] = new float[]{(0.265f + 0.1f * idle) * (float) Math.PI, 0, 0};
            }
        }
        turnSkull("witherBase/center_head", 0, pose, rotations);
        turnSkull("witherBase/left_head", 1, pose, rotations);
        turnSkull("witherBase/right_head", 2, pose, rotations);
    }

    private void turnSkull(String path, int head, Pose pose, float[][] rotations) {
        int part = part(path);
        if (part >= 0) {
            rotations[part] = new float[]{pose.headPitch[head] * DEG, pose.headYaw[head] * DEG, 0};
        }
    }

    // ------------------------------------------------------------------ the parts the fight uses

    /**
     * A head the storm looks, shoots and pulls with: a jawed head, or one of the wither skulls the
     * hunchbacks still carry. Points are in the part's sixteenths; {@code size} is its width in them.
     */
    public record Gaze(int index, int part, Vector3f centre, Vector3f mouth, float size, boolean jawed) {
    }

    /** Every head of this form, by head index; the first is the one in the middle. */
    public List<Gaze> gazes() {
        List<Gaze> out = new ArrayList<>();
        for (Head head : heads) {
            // The jaw's box spans z 0..14 and its emitter sits at z 13.1..14.1, a sixteenth above the jaw line.
            out.add(new Gaze(head.index(), head.root(), new Vector3f(0, -1, 6), new Vector3f(0, -1, 13.6f), 12, true));
        }
        String[] skulls = {"witherBase/center_head", "witherBase/left_head", "witherBase/right_head"};
        for (int i = 0; i < skulls.length; i++) {
            int part = part(skulls[i]);
            if (part < 0 || hasHead(i)) {
                continue;
            }
            Box box = boxOf(part);
            if (box == null) {
                continue;
            }
            Vector3f centre = new Vector3f(box.x() + box.w() / 2f, box.y() + box.h() / 2f, box.z() + box.d() / 2f);
            out.add(new Gaze(i, part, centre, new Vector3f(centre.x, centre.y, box.z()), box.w(), false));
        }
        out.sort(java.util.Comparator.comparingInt(Gaze::index));
        return out;
    }

    private boolean hasHead(int index) {
        for (Head head : heads) {
            if (head.index() == index) return true;
        }
        return false;
    }

    private Box boxOf(int part) {
        for (Box box : boxes) {
            if (box.part() == part) return box;
        }
        return null;
    }

    /** The tip of a tentacle: the far end of its last segment. */
    public Vector3f tip(Matrix4f[] matrices, Tentacle tentacle) {
        int last = tentacle.segments()[5];
        Box box = boxOf(last);
        Matrix4f m = matrices[last];
        if (box == null || m == null) return null;
        return point(m, box.x() + box.w() / 2f, box.y() + box.h() / 2f, box.z());
    }

    /** The width of a gaze in blocks this frame. */
    public static float width(Matrix4f partMatrix, Gaze gaze) {
        return partMatrix.getScale(new Vector3f()).x * gaze.size() / 16f;
    }

    // ------------------------------------------------------------------ measuring

    /** The box every block of this pose fits in, as {minX, minY, minZ, maxX, maxY, maxZ}. */
    public float[] bounds(Pose pose, float size) {
        return bounds(pose, size, null);
    }

    /** As {@link #bounds(Pose, float)}, counting only the boxes of groups of one kind (null for all). */
    public float[] bounds(Pose pose, float size, Kind kind) {
        Matrix4f[] matrices = partMatrices(pose, size);
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Box box : boxes) {
            Matrix4f m = matrices[box.part()];
            if (m == null || (kind != null && kindOf(box.part()) != kind)) {
                continue;
            }
            for (int corner = 0; corner < 8; corner++) {
                Vector3f p = point(m, box.x() + ((corner & 1) == 0 ? 0 : box.w()),
                        box.y() + ((corner & 2) == 0 ? 0 : box.h()), box.z() + ((corner & 4) == 0 ? 0 : box.d()));
                b[0] = Math.min(b[0], p.x);
                b[1] = Math.min(b[1], p.y);
                b[2] = Math.min(b[2], p.z);
                b[3] = Math.max(b[3], p.x);
                b[4] = Math.max(b[4], p.y);
                b[5] = Math.max(b[5], p.z);
            }
        }
        return b;
    }

    /** The kind of group a part is rendered in, or null when no group renders it. */
    public Kind kindOf(int part) {
        for (int p = part; p >= 0; p = parts.get(p).parent()) {
            for (Group group : groups) {
                if (group.root() == p) {
                    return group.kind();
                }
            }
        }
        return null;
    }
}
