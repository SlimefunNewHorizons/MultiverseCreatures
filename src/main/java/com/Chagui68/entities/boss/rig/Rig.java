package com.Chagui68.entities.boss.rig;

import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.Location;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A skeleton for a model sculpted from block displays, and the keyframed moves it plays.
 *
 * <p>Every piece of a model belongs to a {@link Bone} (its part is the bone's name). A bone turns
 * about its pivot, carrying its children: the root carries the whole body and moves it through the
 * air. A {@link Move} is a set of keyframes per bone (pitch, yaw and roll in degrees, eased from one
 * key to the next) plus where the root travels, and what happens along the way: the ticks the weapon
 * cuts between, the ticks the air round the body hurts, the ticks a shockwave rings out, and named
 * cues its boss answers.</p>
 *
 * <p>Some bones move on their own unless a move keys them, by name: {@code CAPE} and {@code HAIR}
 * stream back as the body travels, falls or spins ({@link #follow}); {@code LEG_R}, {@code SHIN_R},
 * {@code LEG_L}, {@code SHIN_L}, which must be the last four bones, stride, crouch and tuck
 * ({@link #legs}). {@code SKIRT} is left to the model.</p>
 *
 * <p>Turns are applied yaw, then pitch, then roll. A hanging limb raised forward has a negative
 * pitch; a positive yaw turns towards the left ({@code +x}); a positive roll swings a hanging limb
 * towards the left. Models face {@code +z} with their feet on the origin.</p>
 *
 * <p>The viewer in {@code website/public/viewer} plays the same keyframes with the same maths.</p>
 */
public final class Rig {

    /** A bone: its name, its parent's index (-1 for the root) and the point it turns about. */
    public record Bone(String name, int parent, Vector3f pivot) {
    }

    /** How a key is reached from the one before it. */
    public enum Ease {
        /** Eased in and out. */
        SMOOTH,
        /** At an even pace: a spin. */
        LINEAR,
        /** Fast at first, settling at the end: a strike. */
        SNAP;

        float apply(float x) {
            return switch (this) {
                case SMOOTH -> x * x * (3 - 2 * x);
                case LINEAR -> x;
                case SNAP -> 1 - (1 - x) * (1 - x) * (1 - x);
            };
        }
    }

    /** A key: at {@code tick}, three values (pitch, yaw, roll in degrees for a bone; x, y, z in blocks for the root's travel). */
    public record Key(float tick, float a, float b, float c, Ease ease) {
    }

    /** Something a move asks of its boss at a tick: throw the keg, call the lightning. */
    public record Cue(int tick, String name) {
    }

    /**
     * A move: its keys per bone (by index) and for the root's travel; the ticks the weapon leaves a
     * trail (and cuts) between; the ticks the air round the body hurts between; the ticks a shockwave
     * rings out; its cues; and for how many ticks from its start the boss keeps turning to its target.
     */
    public record Move(String id, String label, int ticks, Map<Integer, List<Key>> bones, List<Key> travel,
                       List<int[]> trails, List<int[]> slashes, List<Integer> rings, List<Cue> cues, int aim) {
    }

    /** How a body moves at a tick, from the tick before ({@link #motion}). */
    public record Motion(float speed, float sideways, float forward, float fall, float spin, float turning, float height, float distance) {
    }

    /**
     * A part that changes hands: a weapon drawn from the back, a sword put away. As its dial goes from
     * 0 to 100 the bone {@code part} leaves its parent for the bone {@code onto}, where {@code held}
     * places it (a rigid move of the model at rest). The dial {@code by} is a bone no piece rides: a
     * move keys it like any other, and its first number is read as the dial instead of a pitch.
     */
    public record Carry(int part, int onto, int by, Matrix4f held) {
    }

    /**
     * A part that can be spent: while its dial {@code by} (a bone no piece rides, keyed like any other)
     * reads over 50 the bone {@code part} is drawn nowhere, shrunk to nothing where it is. An arrow loosed.
     */
    public record Spend(int part, int by) {
    }

    private final List<Bone> bones;
    private final List<Carry> carries;
    private final List<Spend> spends;
    private final Map<String, Integer> index = new HashMap<>();
    private final float legLength;
    private final int root, pelvis, chest, cape, hair, skirt, legR;

    private Rig(List<Bone> bones, float legLength, List<String[]> carried, List<Matrix4f> held, List<String[]> spent) {
        this.bones = List.copyOf(bones);
        this.legLength = legLength;
        for (int i = 0; i < bones.size(); i++) index.put(bones.get(i).name(), i);
        root = find("ROOT");
        pelvis = find("PELVIS");
        chest = find("CHEST");
        cape = find("CAPE");
        hair = find("HAIR");
        skirt = find("SKIRT");
        legR = find("LEG_R");
        if (root != 0) throw new IllegalArgumentException("the first bone must be ROOT");
        if (legR >= 0 && (legR != bones.size() - 4 || find("SHIN_R") != legR + 1 || find("LEG_L") != legR + 2 || find("SHIN_L") != legR + 3)) {
            throw new IllegalArgumentException("LEG_R, SHIN_R, LEG_L, SHIN_L must be the last four bones");
        }
        List<Carry> all = new ArrayList<>();
        for (int i = 0; i < carried.size(); i++) {
            String[] names = carried.get(i);
            all.add(new Carry(bone(names[0]), bone(names[1]), bone(names[2]), new Matrix4f(held.get(i))));
        }
        carries = List.copyOf(all);
        List<Spend> spends = new ArrayList<>();
        for (String[] names : spent) spends.add(new Spend(bone(names[0]), bone(names[1])));
        this.spends = List.copyOf(spends);
    }

    /** Starts a skeleton whose legs are {@code legLength} blocks from hip to sole. */
    public static Builder skeleton(float legLength) {
        return new Builder(legLength);
    }

    /** Lists a skeleton's bones, parents before children. */
    public static final class Builder {
        private final List<Bone> bones = new ArrayList<>();
        private final List<String[]> carried = new ArrayList<>();
        private final List<Matrix4f> held = new ArrayList<>();
        private final List<String[]> spent = new ArrayList<>();
        private final float legLength;

        private Builder(float legLength) {
            this.legLength = legLength;
        }

        /** The bone {@code part} changes hands to {@code onto}, placed there by {@code held}, on the dial {@code by} ({@link Carry}). */
        public Builder carry(String part, String onto, String by, Matrix4f held) {
            carried.add(new String[]{part, onto, by});
            this.held.add(held);
            return this;
        }

        /** The bone {@code part} is spent, drawn nowhere, while the dial {@code by} reads over 50 ({@link Spend}). */
        public Builder spend(String part, String by) {
            spent.add(new String[]{part, by});
            return this;
        }

        public Builder bone(String name, String parent, Vector3f pivot) {
            int at = -1;
            if (parent != null) {
                for (int i = 0; i < bones.size(); i++) if (bones.get(i).name().equals(parent)) at = i;
                if (at < 0) throw new IllegalArgumentException(name + ": its parent " + parent + " must come before it");
            }
            bones.add(new Bone(name, at, new Vector3f(pivot)));
            return this;
        }

        public Rig build() {
            return new Rig(bones, legLength, carried, held, spent);
        }
    }

    public List<Bone> bones() {
        return bones;
    }

    public List<Carry> carries() {
        return carries;
    }

    public List<Spend> spends() {
        return spends;
    }

    public float legLength() {
        return legLength;
    }

    /** The index of the bone called {@code name}; a model's part that is no bone is a mistake. */
    public int bone(String name) {
        Integer i = index.get(name);
        if (i == null) throw new IllegalArgumentException(name + " is not a bone");
        return i;
    }

    private int find(String name) {
        return index.getOrDefault(name, -1);
    }

    /** Whether bone {@code i} moves on its own when a move does not key it. */
    private boolean automatic(int i) {
        return i == cape || i == hair || i == skirt || legR >= 0 && i >= legR;
    }

    /** The bone every piece of a model rides on. */
    public int[] owners(List<Piece> pieces) {
        int[] owners = new int[pieces.size()];
        for (int i = 0; i < owners.length; i++) owners[i] = bone(pieces.get(i).part());
        return owners;
    }

    // =====================================================================================
    // Posing
    // =====================================================================================

    /** The three values of a track at {@code tick}: held before its first key and after its last, eased between. */
    public static float[] sample(List<Key> keys, float tick) {
        if (keys == null || keys.isEmpty()) return new float[3];
        Key first = keys.get(0);
        if (tick <= first.tick()) return new float[]{first.a(), first.b(), first.c()};
        for (int i = 0; i < keys.size() - 1; i++) {
            Key k0 = keys.get(i), k1 = keys.get(i + 1);
            if (tick <= k1.tick()) {
                float x = k1.ease().apply((tick - k0.tick()) / (k1.tick() - k0.tick()));
                return new float[]{k0.a() + (k1.a() - k0.a()) * x, k0.b() + (k1.b() - k0.b()) * x, k0.c() + (k1.c() - k0.c()) * x};
            }
        }
        Key last = keys.get(keys.size() - 1);
        return new float[]{last.a(), last.b(), last.c()};
    }

    private static float[] track(Move move, int bone, float tick) {
        return sample(bone < 0 ? null : move.bones().get(bone), tick);
    }

    /**
     * How the body moves at {@code tick}, from the tick before: its travel and its speed across the
     * ground, that velocity in its own frame, how fast it falls and spins, how fast its trunk turns,
     * and how high (or, crouching, how low) it is.
     */
    public Motion motion(Move move, float tick) {
        float before = Math.max(0, tick - 1);
        float[] now = sample(move.travel(), tick), then = sample(move.travel(), before);
        float dx = now[0] - then[0], dz = now[2] - then[2];
        float rootYaw = track(move, root, tick)[1];
        float spin = Math.abs(rootYaw - track(move, root, before)[1]);
        float pelvisYaw = track(move, pelvis, tick)[1];
        float turning = track(move, chest, tick)[1] - track(move, chest, before)[1] + pelvisYaw - track(move, pelvis, before)[1];
        double yaw = Math.toRadians(rootYaw + pelvisYaw);
        float sideways = (float) (dx * Math.cos(yaw) - dz * Math.sin(yaw)), forward = (float) (dx * Math.sin(yaw) + dz * Math.cos(yaw));
        return new Motion((float) Math.hypot(dx, dz), sideways, forward, Math.max(0, then[1] - now[1]), spin, turning, now[1],
                (float) Math.hypot(now[0], now[2]));
    }

    /**
     * How the cape ({@code hair} false) or the hair trails the body at {@code tick}, as a turn: it
     * streams back the faster the body travels, falls or spins, sways a little at rest, and lags
     * behind when the trunk turns.
     */
    public float[] follow(Move move, float tick, boolean hair) {
        Motion m = motion(move, tick);
        float lift = hair ? Math.min(60, m.speed() * 110 + m.fall() * 50 + m.spin() * .4f) + 3 * (float) Math.sin(tick * .17)
                : Math.min(60, m.speed() * 140 + m.fall() * 60 + m.spin() * .4f) + 4 * (float) Math.sin(tick * .15);
        float lag = Math.max(-30, Math.min(30, -m.turning() * (hair ? 1.2f : 1.5f)));
        return new float[]{lift, lag, 0};
    }

    /**
     * The legs at {@code tick} (thigh right, shin right, thigh left, shin left; each a pitch): on the
     * ground they stride as the body travels, stand upright however it leans, and bend at the knee
     * when it crouches so the feet stay on the floor; in the air they tuck up.
     */
    public float[] legs(Move move, float tick) {
        Motion m = motion(move, tick);
        float grounded = Math.max(0, Math.min(1, 1 - (m.height() - .15f) / .4f));
        float crouch = m.height() < 0 ? (float) Math.toDegrees(Math.acos(Math.max(-1, 1 + m.height() / legLength))) : 0;
        float stride = Math.min(30, m.speed() * 120) * grounded * (float) Math.sin(m.distance() / .5 * Math.PI);
        float upright = -(track(move, pelvis, tick)[0] + track(move, root, tick)[0]) * grounded;
        float air = 1 - grounded;
        return new float[]{
                -crouch + stride + upright - 45 * air, 2 * crouch + Math.max(0, -stride) * .6f + 85 * air,
                -crouch - stride + upright - 20 * air, 2 * crouch + Math.max(0, stride) * .6f + 50 * air};
    }

    /** Every bone's matrix at {@code tick} of {@code move}: parent, then its pivot, its turn (yaw, pitch, roll) and back. */
    public Matrix4f[] bones(Move move, float tick) {
        return bones(move, tick, new Matrix4f());
    }

    /**
     * {@link #bones(Move, float)} with the whole body first placed by {@code carry}: where the boss
     * stands and faces relative to its displays, so walking and turning never teleport them.
     */
    public Matrix4f[] bones(Move move, float tick, Matrix4f carry) {
        Matrix4f[] world = new Matrix4f[bones.size()];
        float[] travel = sample(move.travel(), tick);
        float[] legs = null;
        for (int i = 0; i < world.length; i++) {
            Bone bone = bones.get(i);
            List<Key> keys = move.bones().get(i);
            float[] turn;
            if (keys != null) {
                turn = sample(keys, tick);
            } else if (i == cape || i == hair) {
                turn = follow(move, tick, i == hair);
            } else if (legR >= 0 && i >= legR) {
                if (legs == null) legs = legs(move, tick);
                turn = new float[]{legs[i - legR], 0, 0};
            } else {
                turn = new float[3];
            }
            Matrix4f m = bone.parent() < 0 ? new Matrix4f(carry).translate(travel[0], travel[1], travel[2])
                    : new Matrix4f(world[bone.parent()]);
            Vector3f p = bone.pivot();
            m.translate(p).rotateY((float) Math.toRadians(turn[1])).rotateX((float) Math.toRadians(turn[0]))
                    .rotateZ((float) Math.toRadians(turn[2])).translate(-p.x, -p.y, -p.z);
            world[i] = m;
        }
        for (Carry c : carries) {
            float by = Math.max(0, Math.min(1, track(move, c.by(), tick)[0] / 100f));
            if (by > 0) world[c.part()] = blend(world[c.part()], new Matrix4f(world[c.onto()]).mul(c.held()), by, bones.get(c.part()).pivot());
        }
        return world;
    }

    /**
     * The rigid frame {@code by} of the way from {@code a} to {@code b}: {@code pivot} goes straight
     * from where the one puts it to where the other does, and the frame turns about it as it goes.
     */
    static Matrix4f blend(Matrix4f a, Matrix4f b, float by, Vector3f pivot) {
        if (by >= 1) return b;
        Vector3f at = a.transformPosition(new Vector3f(pivot)).lerp(b.transformPosition(new Vector3f(pivot)), by);
        Quaternionf turn = a.getNormalizedRotation(new Quaternionf()).slerp(b.getNormalizedRotation(new Quaternionf()), by);
        return new Matrix4f().translation(at).rotate(turn).translate(-pivot.x, -pivot.y, -pivot.z);
    }

    /** Where every piece is at {@code tick} of {@code move}, the body placed by {@code carry}. */
    public List<Transformation> pose(Move move, float tick, List<Piece> pieces, int[] owners, Matrix4f carry) {
        Matrix4f[] world = bones(move, tick, carry);
        boolean[] spent = new boolean[world.length];
        for (Spend s : spends) spent[s.part()] = track(move, s.by(), tick)[0] > 50;
        List<Transformation> out = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            Transformation t = posed(pieces.get(i), world[owners[i]]);
            out.add(spent[owners[i]] ? new Transformation(t.getTranslation(), t.getLeftRotation(), new Vector3f(), t.getRightRotation()) : t);
        }
        return out;
    }

    /** A piece carried by its bone's matrix: turned and moved, never stretched. */
    public static Transformation posed(Piece piece, Matrix4f bone) {
        Transformation rest = piece.transformation();
        Vector3f translation = bone.transformPosition(new Vector3f(rest.getTranslation()));
        Quaternionf rotation = bone.getNormalizedRotation(new Quaternionf()).mul(rest.getLeftRotation());
        return new Transformation(translation, rotation, rest.getScale(), new Quaternionf());
    }

    /** A point of the model in the world: turned by the display's yaw and moved to its feet. */
    public static Location toWorld(Location feet, float yaw, Vector3f p) {
        Vector3f turned = new Quaternionf().rotateY((float) Math.toRadians(-yaw)).transform(new Vector3f(p));
        return feet.clone().add(turned.x, turned.y, turned.z);
    }

    // =====================================================================================
    // Authoring
    // =====================================================================================

    public MoveBuilder move(String id, String label, int ticks) {
        return new MoveBuilder(id, label, ticks);
    }

    /**
     * Builds a move from compact keys: {@code "ARM_R -85 55 0; CHEST 0 40 0; move 0 0 .5"} sets those
     * bones' pitch, yaw and roll and where the root has travelled to at that tick; bones a key leaves
     * out keep easing between their own keys. An empty key holds every bone at rest, the automatic
     * ones excepted.
     */
    public final class MoveBuilder {
        private final String id, label;
        private final int ticks;
        private final Map<Integer, List<Key>> tracks = new LinkedHashMap<>();
        private final List<Key> travel = new ArrayList<>();
        private final List<int[]> trails = new ArrayList<>(), slashes = new ArrayList<>();
        private final List<Integer> rings = new ArrayList<>();
        private final List<Cue> cues = new ArrayList<>();
        private int aim = -1;

        private MoveBuilder(String id, String label, int ticks) {
            this.id = id;
            this.label = label;
            this.ticks = ticks;
        }

        public MoveBuilder key(int tick, Ease ease, String spec) {
            if (spec.isBlank()) {
                for (int i = 0; i < bones.size(); i++) {
                    if (!automatic(i)) tracks.computeIfAbsent(i, b -> new ArrayList<>()).add(new Key(tick, 0, 0, 0, ease));
                }
                travel.add(new Key(tick, 0, 0, 0, ease));
                return this;
            }
            for (String part : spec.split(";")) {
                String[] w = part.trim().split("\\s+");
                Key key = new Key(tick, Float.parseFloat(w[1]), Float.parseFloat(w[2]), Float.parseFloat(w[3]), ease);
                if (w[0].equals("move")) travel.add(key);
                else tracks.computeIfAbsent(bone(w[0]), b -> new ArrayList<>()).add(key);
            }
            return this;
        }

        /**
         * Legs that walk on the spot for the whole move, a step every half {@code period}: the thighs
         * swing {@code thigh} degrees either way and the trailing knee bends {@code knee}. For the
         * loops a boss plays while its stand walks.
         */
        public MoveBuilder stride(int period, float thigh, float knee) {
            for (int tick = 0, k = 0; tick <= ticks; tick += period / 4, k++) {
                float swing = k % 2 == 1 ? 0 : k % 4 == 0 ? -thigh : thigh;
                float right = k % 4 == 3 ? knee : k % 4 == 2 ? knee * .5f : k % 4 == 0 ? knee * .15f : 0;
                float left = k % 4 == 1 ? knee : k % 4 == 0 ? knee * .5f : k % 4 == 2 ? knee * .15f : 0;
                key(tick, Ease.SMOOTH, String.format(Locale.ROOT, "LEG_R %s 0 0; SHIN_R %s 0 0; LEG_L %s 0 0; SHIN_L %s 0 0",
                        swing, right, -swing, left));
            }
            return this;
        }

        public MoveBuilder trail(int from, int to) {
            trails.add(new int[]{from, to});
            return this;
        }

        public MoveBuilder slashes(int from, int to) {
            slashes.add(new int[]{from, to});
            return this;
        }

        public MoveBuilder ring(int tick) {
            rings.add(tick);
            return this;
        }

        public MoveBuilder cue(int tick, String name) {
            cues.add(new Cue(tick, name));
            return this;
        }

        /** For how many ticks from its start the boss keeps turning to its target (default: until its first blow). */
        public MoveBuilder aim(int ticks) {
            aim = ticks;
            return this;
        }

        public Move build() {
            Map<Integer, List<Key>> frozen = new LinkedHashMap<>();
            tracks.forEach((bone, keys) -> {
                List<Key> track = new ArrayList<>(keys);
                // An automatic bone keyed part way through starts from rest, not from its first key.
                if (automatic(bone) && track.get(0).tick() > 0) track.add(0, new Key(0, 0, 0, 0, Ease.SMOOTH));
                frozen.put(bone, List.copyOf(track));
            });
            int first = ticks;
            for (int[] r : trails) first = Math.min(first, r[0]);
            for (int[] r : slashes) first = Math.min(first, r[0]);
            for (int r : rings) first = Math.min(first, r);
            for (Cue c : cues) first = Math.min(first, c.tick());
            return new Move(id, label, ticks, frozen, List.copyOf(travel), List.copyOf(trails), List.copyOf(slashes),
                    List.copyOf(rings), List.copyOf(cues), aim >= 0 ? aim : first == ticks ? 0 : Math.max(0, first - 2));
        }
    }
}
