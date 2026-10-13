package com.Chagui68.entities.monster;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure kinematics of the Monster: eighty player heads exported from BDEngine, a rig that says which
 * heads move together, and the poses it takes. Shared by the game and the model viewer's export, so
 * the viewer shows exactly the matrices the displays get.
 *
 * <p><strong>The heads keep their shape.</strong> Every head keeps the full matrix it was built with,
 * shear included: a part moves its heads rigidly, so nothing is ever decomposed into a translation,
 * a rotation and a scale. A head hangs below its anchor ({@code y} from -0.5 to 0 in its own units),
 * like every BDEngine head this plugin draws.</p>
 *
 * <p><strong>The frame.</strong> The model is centred on its feet and turned to face {@code +z}, its
 * right hand on {@code -x}. A pose is drawn from its anchor as {@code T(lift) * tilt}; the game puts
 * that anchor in the world (the floor, the underside of a ceiling, the top of a block) and turns it
 * to face the player. The rig's own file, {@code /monster/rig.json}, documents the format.</p>
 */
public final class MonsterRig {

    /** What a pose's anchor is in the world. */
    public enum Place {
        /** The floor it stands, crouches or sits on. */
        GROUND,
        /** The underside of the block above: it hangs or clings below it. */
        CEILING,
        /** The top of a block or chest it lies, sits or perches on. */
        BLOCK,
        /** A point in the air, for a player who is flying. */
        AIR,
        /** Only played once it has been seen. */
        ACTION;

        static Place of(String name) {
            return valueOf(name.toUpperCase(java.util.Locale.ROOT));
        }
    }

    /**
     * One pose. {@code at}, {@code yaw} and {@code player} only place it round the viewer's player;
     * the game finds the anchor in the world. {@code turns} holds [pitch, yaw, roll] degrees per part.
     * {@code space} is the room a tight pose was made to fit: blocks wide (x), deep (z) and tall, or
     * {@code null} for a pose that needs open ground. {@code base} is the top it lies on, for a pose on
     * blocks: 1 x 1, or 2 x 1 (a double chest) along its own x. {@code world} is the blocks the viewer
     * shows round it, or {@code null} for open ground.
     */
    public record Pose(String name, Place place, Vector3f at, float yaw, Vector3f lift, Vector3f tilt,
                       Map<String, Vector3f> turns, float[] player, float[] space, float[] base, World world) {

        /** True for a pose that lies on two blocks side by side, a double chest. */
        public boolean wide() {
            return base != null && base[0] >= 2;
        }

        /** The name a command takes: the pose's name with dashes for spaces. */
        public String id() {
            return name.replace(' ', '-');
        }

        /** How far a part turns in this pose; nothing when the pose leaves it as built. */
        public Vector3f turn(String part) {
            Vector3f t = turns.get(part);
            return t == null ? new Vector3f() : new Vector3f(t);
        }

        /**
         * The pose {@code share} of the way from this one to {@code to}: lift, tilt and every joint
         * eased together, so the body moves as one instead of every head sliding on its own.
         */
        public Pose blend(Pose to, float share) {
            Map<String, Vector3f> mixed = new HashMap<>();
            for (String part : union(turns.keySet(), to.turns.keySet())) {
                mixed.put(part, turn(part).lerp(to.turn(part), share));
            }
            return new Pose(to.name, to.place, new Vector3f(at).lerp(to.at, share), yaw + (to.yaw - yaw) * share,
                    new Vector3f(lift).lerp(to.lift, share), new Vector3f(tilt).lerp(to.tilt, share), mixed, to.player,
                    to.space, to.base, to.world);
        }

        /** This pose raised (or lowered) by {@code dy} on its anchor. */
        public Pose lifted(float dy) {
            return new Pose(name, place, at, yaw, new Vector3f(lift).add(0, dy, 0), tilt, turns, player, space, base, world);
        }

        /** This pose with some joints turned further, for a stride or a sway on top of it. */
        public Pose plus(Map<String, Vector3f> extra) {
            Map<String, Vector3f> mixed = new HashMap<>();
            for (String part : union(turns.keySet(), extra.keySet())) {
                Vector3f more = extra.get(part);
                mixed.put(part, more == null ? turn(part) : turn(part).add(more));
            }
            return new Pose(name, place, at, yaw, lift, tilt, mixed, player, space, base, world);
        }

        private static List<String> union(java.util.Set<String> a, java.util.Set<String> b) {
            List<String> all = new ArrayList<>(a);
            for (String s : b) {
                if (!a.contains(s)) all.add(s);
            }
            return all;
        }
    }

    private static final Pattern TEXTURE = Pattern.compile("value:\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern MATRIX = Pattern.compile("transformation:\\[([^\\]]+)]");
    private static final String RIG = "/monster/rig.json";

    private static volatile MonsterRig loaded;

    private final String key;
    private final String name;
    private final String group;
    private final List<String> textures;
    private final List<Matrix4f> rest;
    private final List<String> partNames;
    private final int[] parents;
    private final Vector3f[] pivots;
    private final int[] owners;
    private final Map<String, Pose> poses;
    private final Vector3f eyes;
    private final Vector3f palm;
    private final float[] player;

    private MonsterRig(String key, String name, String group, List<String> textures, List<Matrix4f> rest,
                       List<String> partNames, int[] parents, Vector3f[] pivots, int[] owners,
                       Map<String, Pose> poses, Vector3f eyes, Vector3f palm, float[] player) {
        this.key = key;
        this.name = name;
        this.group = group;
        this.textures = List.copyOf(textures);
        this.rest = rest;
        this.partNames = List.copyOf(partNames);
        this.parents = parents;
        this.pivots = pivots;
        this.owners = owners;
        this.poses = Collections.unmodifiableMap(poses);
        this.eyes = eyes;
        this.palm = palm;
        this.player = player;
    }

    /** The rig the plugin ships, read once. */
    public static MonsterRig get() {
        MonsterRig rig = loaded;
        if (rig == null) {
            synchronized (MonsterRig.class) {
                if (loaded == null) {
                    JsonObject json = JsonParser.parseString(read(RIG)).getAsJsonObject();
                    loaded = parse(json, read(json.get("model").getAsString()));
                }
                rig = loaded;
            }
        }
        return rig;
    }

    // ------------------------------------------------------------------ reading

    /**
     * Reads a rig and the export it rigs.
     *
     * @throws IllegalArgumentException when a head has no part or two, a part names a missing parent,
     *                                  a pose turns a part the rig does not have, or the export does not
     *                                  have one skin per transformation
     */
    static MonsterRig parse(JsonObject json, String command) {
        List<String> textures = new ArrayList<>();
        Matcher texture = TEXTURE.matcher(command);
        while (texture.find()) {
            textures.add(texture.group(1));
        }
        List<Matrix4f> rest = rest(command);
        if (textures.size() != rest.size()) {
            throw new IllegalArgumentException(textures.size() + " skins for " + rest.size() + " transformations");
        }

        JsonObject parts = json.getAsJsonObject("parts");
        List<String> partNames = new ArrayList<>(parts.keySet());
        int[] parents = new int[partNames.size()];
        Vector3f[] pivots = new Vector3f[partNames.size()];
        int[] owners = new int[rest.size()];
        java.util.Arrays.fill(owners, -1);
        for (int p = 0; p < partNames.size(); p++) {
            JsonObject part = parts.getAsJsonObject(partNames.get(p));
            JsonElement parent = part.get("parent");
            parents[p] = parent == null || parent.isJsonNull() ? -1 : partNames.indexOf(parent.getAsString());
            if (parent != null && !parent.isJsonNull() && parents[p] < 0) {
                throw new IllegalArgumentException(partNames.get(p) + " hangs from a missing part " + parent);
            }
            pivots[p] = vector(part.getAsJsonArray("pivot"));
            for (JsonElement range : part.getAsJsonArray("heads")) {
                JsonArray r = range.getAsJsonArray();
                for (int i = r.get(0).getAsInt(); i <= r.get(1).getAsInt(); i++) {
                    if (i < 0 || i >= owners.length) {
                        throw new IllegalArgumentException(partNames.get(p) + " claims head " + i
                                + ", but the export has " + owners.length);
                    }
                    if (owners[i] >= 0) {
                        throw new IllegalArgumentException("head " + i + " is in " + partNames.get(owners[i])
                                + " and " + partNames.get(p));
                    }
                    owners[i] = p;
                }
            }
        }
        for (int i = 0; i < owners.length; i++) {
            if (owners[i] < 0) {
                throw new IllegalArgumentException("head " + i + " belongs to no part");
            }
        }

        float[] player = floats(json.getAsJsonArray("player"));
        Map<String, Pose> poses = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("poses").entrySet()) {
            poses.put(entry.getKey(), pose(entry.getKey(), entry.getValue().getAsJsonObject(), partNames, player));
        }
        return new MonsterRig(json.get("key").getAsString(), json.get("name").getAsString(),
                json.get("group").getAsString(), textures, rest, partNames, parents, pivots, owners, poses,
                vector(json.getAsJsonArray("eyes")), vector(json.getAsJsonArray("palm")), player);
    }

    private static Pose pose(String name, JsonObject json, List<String> parts, float[] player) {
        Map<String, Vector3f> turns = new HashMap<>();
        for (String key : json.keySet()) {
            switch (key) {
                case "place", "at", "yaw", "lift", "tilt", "player", "space", "base", "world" -> {
                }
                default -> {
                    if (!parts.contains(key)) {
                        throw new IllegalArgumentException("pose " + name + " turns an unknown part " + key);
                    }
                    turns.put(key, vector(json.getAsJsonArray(key)));
                }
            }
        }
        float[] stand = json.has("player") ? floats(json.getAsJsonArray("player")) : player;
        float[] at = {stand.length > 0 ? stand[0] : 0, stand.length > 1 ? stand[1] : 0,
                stand.length > 2 ? stand[2] : 0, stand.length > 3 ? stand[3] : 0};
        return new Pose(name, Place.of(json.get("place").getAsString()), optional(json, "at"),
                json.has("yaw") ? json.get("yaw").getAsFloat() : 0f, optional(json, "lift"), optional(json, "tilt"),
                turns, at, json.has("space") ? floats(json.getAsJsonArray("space")) : null,
                json.has("base") ? floats(json.getAsJsonArray("base")) : null,
                json.has("world") ? World.of(json.getAsJsonObject("world")) : null);
    }

    /**
     * The blocks the viewer shows round a pose, in the viewer's coordinates: a floor under
     * {@code y = 0} with holes in it, and boxes of blocks (stone, or a chest) from one corner up to,
     * not including, the other.
     */
    public record World(List<int[]> blocks, List<Boolean> chests, List<int[]> holes) {

        static World of(JsonObject json) {
            List<int[]> blocks = new ArrayList<>(), holes = new ArrayList<>();
            List<Boolean> chests = new ArrayList<>();
            if (json.has("blocks")) {
                for (JsonElement e : json.getAsJsonArray("blocks")) {
                    JsonArray a = e.getAsJsonArray();
                    int[] b = new int[6];
                    for (int i = 0; i < 6; i++) b[i] = a.get(i).getAsInt();
                    blocks.add(b);
                    chests.add(a.size() > 6 && a.get(6).getAsString().equals("chest"));
                }
            }
            if (json.has("holes")) {
                for (JsonElement e : json.getAsJsonArray("holes")) {
                    JsonArray a = e.getAsJsonArray();
                    holes.add(new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
                }
            }
            return new World(List.copyOf(blocks), List.copyOf(chests), List.copyOf(holes));
        }

        /** These blocks to plan in. */
        public MonsterSpace.Blocks space() {
            MonsterSpace.Blocks space = new MonsterSpace.Blocks(true);
            for (int[] h : holes) space.hole(h[0], h[1], h[2]);
            for (int[] b : blocks) space.fill(b[0], b[1], b[2], b[3], b[4], b[5]);
            return space;
        }
    }

    /**
     * The export's matrices, centred on the model's feet and turned round to face {@code +z}. The box
     * a head fills is its anchor and the cube under it.
     */
    private static List<Matrix4f> rest(String command) {
        List<Matrix4f> raw = new ArrayList<>();
        Matcher matrix = MATRIX.matcher(command);
        while (matrix.find()) {
            String[] n = matrix.group(1).split(",");
            if (n.length != 16) {
                throw new IllegalArgumentException("a transformation has " + n.length + " numbers, not 16");
            }
            float[] v = new float[16];
            for (int i = 0; i < 16; i++) {
                v[i] = Float.parseFloat(n[i].trim().replace("f", "").replace("F", ""));
            }
            // Row major in the command, column major in JOML.
            raw.add(new Matrix4f(v[0], v[4], v[8], v[12], v[1], v[5], v[9], v[13],
                    v[2], v[6], v[10], v[14], v[3], v[7], v[11], v[15]));
        }
        float[] b = bounds(raw);
        Matrix4f centre = new Matrix4f().rotationY((float) Math.PI)
                .translate(-(b[0] + b[3]) / 2, -b[1], -(b[2] + b[5]) / 2);
        List<Matrix4f> rest = new ArrayList<>();
        for (Matrix4f m : raw) {
            rest.add(new Matrix4f(centre).mul(m));
        }
        return List.copyOf(rest);
    }

    // ------------------------------------------------------------------ posing

    /** How the body sits on its anchor in a pose: lifted, then tilted. */
    public static Matrix4f body(Pose pose) {
        return turn(new Matrix4f().translation(pose.lift()), pose.tilt());
    }

    /** Where the viewer puts a pose round its player: moved to {@code at}, turned by {@code yaw}. */
    public static Matrix4f viewer(Pose pose) {
        return new Matrix4f().translation(pose.at()).rotateY((float) Math.toRadians(pose.yaw()));
    }

    /** Every part's matrix, carried by {@code root}: its parent's, then its own turn about its pivot. */
    public Matrix4f[] parts(Pose pose, Matrix4f root) {
        Matrix4f[] world = new Matrix4f[partNames.size()];
        for (int p = 0; p < world.length; p++) {
            place(p, pose, root, world);
        }
        return world;
    }

    private Matrix4f place(int p, Pose pose, Matrix4f root, Matrix4f[] world) {
        if (world[p] == null) {
            Matrix4f m = new Matrix4f(parents[p] < 0 ? root : place(parents[p], pose, root, world));
            m.translate(pivots[p]);
            turn(m, pose.turn(partNames.get(p)));
            m.translate(new Vector3f(pivots[p]).negate());
            world[p] = m;
        }
        return world[p];
    }

    /** The matrix of every head, in the export's order. */
    public Matrix4f[] heads(Pose pose, Matrix4f root) {
        Matrix4f[] parts = parts(pose, root);
        Matrix4f[] heads = new Matrix4f[rest.size()];
        for (int i = 0; i < heads.length; i++) {
            heads[i] = new Matrix4f(parts[owners[i]]).mul(rest.get(i));
        }
        return heads;
    }

    /**
     * Its face alone: the heads of {@code HEAD} and {@code JAW} in {@code pose}, scaled by {@code scale}
     * round its eyes and carried there by {@code place}; every other head folded to nothing.
     */
    public Matrix4f[] face(Pose pose, Matrix4f place, float scale) {
        Vector3f e = eyes(pose, body(pose));
        Matrix4f root = new Matrix4f(place).scale(scale).translate(-e.x, -e.y, -e.z).mul(body(pose));
        Matrix4f[] heads = heads(pose, root);
        int head = partNames.indexOf("HEAD"), jaw = partNames.indexOf("JAW");
        for (int i = 0; i < heads.length; i++) {
            if (owners[i] != head && owners[i] != jaw) heads[i] = new Matrix4f().scaling(0f);
        }
        return heads;
    }

    /** A point of one part (given where the model was built) where the pose puts it. */
    public Vector3f point(Pose pose, Matrix4f root, String part, Vector3f local) {
        int p = partNames.indexOf(part);
        if (p < 0) throw new IllegalArgumentException("no part " + part);
        return parts(pose, root)[p].transformPosition(new Vector3f(local));
    }

    /** Where it looks from in a pose. */
    public Vector3f eyes(Pose pose, Matrix4f root) {
        return point(pose, root, "HEAD", eyes);
    }

    /** The palm of the right hand, the one it covers the player's view with. */
    public Vector3f palm(Pose pose, Matrix4f root) {
        return point(pose, root, "HAND_R", palm);
    }

    /** The middle of its chest in a pose. */
    public Vector3f chest(Pose pose, Matrix4f root) {
        return point(pose, root, "TORSO", new Vector3f(0f, 2.4f, -0.4f));
    }

    /** The box the whole body fills: {@code minX, minY, minZ, maxX, maxY, maxZ}. */
    public float[] bounds(Pose pose, Matrix4f root) {
        return bounds(List.of(heads(pose, root)));
    }

    /** The box a set of heads fill, each head being its anchor and the cube hanging under it. */
    public static float[] bounds(List<Matrix4f> heads) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Vector3f corner = new Vector3f();
        for (Matrix4f m : heads) {
            for (int c = 0; c < 8; c++) {
                m.transformPosition((c & 1) == 0 ? -0.25f : 0.25f, (c & 2) == 0 ? 0f : -0.5f,
                        (c & 4) == 0 ? -0.25f : 0.25f, corner);
                b[0] = Math.min(b[0], corner.x);
                b[1] = Math.min(b[1], corner.y);
                b[2] = Math.min(b[2], corner.z);
                b[3] = Math.max(b[3], corner.x);
                b[4] = Math.max(b[4], corner.y);
                b[5] = Math.max(b[5], corner.z);
            }
        }
        return b;
    }

    /** Turns by [pitch, yaw, roll] degrees: yaw, then pitch, then roll. Pitch above 0 bends forward. */
    public static Matrix4f turn(Matrix4f m, Vector3f degrees) {
        return m.rotateY((float) Math.toRadians(degrees.y))
                .rotateX((float) Math.toRadians(degrees.x))
                .rotateZ((float) Math.toRadians(degrees.z));
    }

    // ------------------------------------------------------------------ accessors

    public String key() {
        return key;
    }

    public String name() {
        return name;
    }

    public String group() {
        return group;
    }

    /** The base64 skin of every head, in the export's order. */
    public List<String> textures() {
        return textures;
    }

    public int size() {
        return rest.size();
    }

    public List<String> partNames() {
        return partNames;
    }

    /** The part a head moves with. */
    public String partOf(int head) {
        return partNames.get(owners[head]);
    }

    /** The index, in {@link #partNames()}, of the part a head moves with. */
    public int ownerOf(int head) {
        return owners[head];
    }

    /** The index of a part's parent, or -1 for a part that hangs from the body itself. */
    public int parentOf(int part) {
        return parents[part];
    }

    /** The joint a part turns about. */
    public Vector3f pivot(int part) {
        return new Vector3f(pivots[part]);
    }

    /** A head's matrix as built: centred on the feet and facing {@code +z}. */
    public Matrix4f rest(int head) {
        return new Matrix4f(rest.get(head));
    }

    public Map<String, Pose> poses() {
        return poses;
    }

    public Pose pose(String name) {
        Pose pose = poses.get(name);
        if (pose == null) throw new IllegalArgumentException("no pose " + name);
        return pose;
    }

    /** The pose a command names, by its {@link Pose#id()} (any case), or {@code null}. */
    public Pose byId(String id) {
        for (Pose pose : poses.values()) {
            if (pose.id().equalsIgnoreCase(id)) return pose;
        }
        return null;
    }

    /** The poses it may wait in at one kind of place. */
    public List<Pose> poses(Place place) {
        List<Pose> out = new ArrayList<>();
        for (Pose pose : poses.values()) {
            if (pose.place() == place) out.add(pose);
        }
        return out;
    }

    /** Where the viewer's player stands by default: x, y, z. */
    public float[] player() {
        return player.clone();
    }

    // ------------------------------------------------------------------ helpers

    private static Vector3f optional(JsonObject json, String key) {
        return json.has(key) ? vector(json.getAsJsonArray(key)) : new Vector3f();
    }

    private static Vector3f vector(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static String read(String resource) {
        try (InputStream in = MonsterRig.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is missing from the plugin");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + resource, e);
        }
    }
}
