package com.Chagui68.entities.boss;

import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Malenia's skeleton and her attacks.
 *
 * <p>Every piece of {@link MaleniaModel} belongs to a {@link Bone} (its part is the bone's name). A
 * bone turns about its pivot, carrying its children: the root carries the whole body and moves it
 * through the air, the pelvis carries the robe, the chest the arms, head, cape and hair. An attack
 * ({@link Move}) is a set of keyframes per bone — pitch, yaw and roll in degrees, eased from one key to
 * the next — plus where the root travels, and the effects it plays: sword trails, slashes filling the
 * air round her, shockwaves.</p>
 *
 * <p>The cape, the hair, the legs and the skirt follow her on their own (unless a move keys them):
 * the cape and hair stream back as she dashes, falls or spins and lag when her body turns
 * ({@link #follow}); her legs stride, crouch and tuck ({@link #legs}); every tile of the robe swings
 * out from the waist as she spins, falls, crouches, moves and strides, stretching so the robe stays
 * one cloth ({@link #skirt}).</p>
 *
 * <p>Turns are applied yaw, then pitch, then roll. A hanging limb raised forward has a negative
 * pitch; a positive yaw turns towards her left ({@code +x}); a positive roll swings a hanging limb
 * towards her left. Her right hand's pitch of about 53 lines the blade up with the forearm.</p>
 *
 * <p>The viewer in {@code website/public/viewer} plays the same keyframes with the same maths.</p>
 */
public final class MaleniaRig {

    /** Her bones, parents before children. */
    public enum Bone {
        ROOT(null, new Vector3f(0, 1.30f, 0)),
        PELVIS(ROOT, new Vector3f(0, 1.42f, 0)),
        CHEST(PELVIS, new Vector3f(0, 1.52f, 0)),
        HEAD(CHEST, new Vector3f(0, 2.17f, 0)),
        CAPE(CHEST, new Vector3f(0, 2.02f, -.10f)),
        HAIR(CHEST, new Vector3f(0, 2.25f, -.12f)),
        ARM_R(CHEST, MaleniaModel.SHOULDER_R),
        FOREARM_R(ARM_R, MaleniaModel.ELBOW_R),
        HAND_R(FOREARM_R, MaleniaModel.WRIST_R),
        ARM_L(CHEST, MaleniaModel.SHOULDER_L),
        FOREARM_L(ARM_L, MaleniaModel.ELBOW_L),
        /** The robe and tunic: each tile swings out from the waist on its own ({@link #skirt}). */
        SKIRT(PELVIS, new Vector3f(0, 1.49f, 0)),
        LEG_R(PELVIS, MaleniaModel.HIP_R),
        SHIN_R(LEG_R, MaleniaModel.KNEE_R),
        LEG_L(PELVIS, MaleniaModel.HIP_L),
        SHIN_L(LEG_L, MaleniaModel.KNEE_L);

        public final Bone parent;
        public final Vector3f pivot;

        Bone(Bone parent, Vector3f pivot) {
            this.parent = parent;
            this.pivot = pivot;
        }
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

    /**
     * An attack: its keys per bone and for the root's travel, and its effects: the ticks the sword
     * leaves a trail between, the ticks slashes fill the air round her between, and the ticks a
     * shockwave rings out.
     */
    public record Move(String id, String label, int ticks, Map<Bone, List<Key>> bones, List<Key> travel,
                       List<int[]> trails, List<int[]> slashes, List<Integer> rings) {
    }

    /** Every attack, in the order the viewer lists them. */
    public static final List<Move> MOVES = List.of(
            move("idle", "Stance", 80)
                    .key(0, Ease.SMOOTH, "")
                    .key(20, Ease.SMOOTH, "PELVIS 0 4 -2; CHEST -3 -6 2; HEAD 4 14 0; ARM_R -8 -6 0; HAND_R 10 0 0; ARM_L 4 0 3; move .02 .03 0")
                    .key(40, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 0")
                    .key(60, Ease.SMOOTH, "PELVIS 0 -4 2; CHEST -2 6 -2; HEAD 2 -16 0; ARM_R -12 8 0; HAND_R -6 0 0; ARM_L -3 0 2; move -.02 .03 0")
                    .key(80, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 0")
                    .build(),
            move("combo", "Three-slash combo", 60)
                    .key(0, Ease.SMOOTH, "")
                    .key(8, Ease.SMOOTH, "PELVIS 5 -20 0; CHEST 8 -45 0; HEAD 0 30 0; ARM_R -75 -95 0; FOREARM_R -25 0 0; HAND_R 50 0 0; ARM_L -20 0 25; move 0 -.12 -.1")
                    .key(12, Ease.SNAP, "PELVIS 0 20 0; CHEST 10 50 0; HEAD 0 -20 0; ARM_R -85 60 0; FOREARM_R -5 0 0; ARM_L -10 0 40; move 0 0 .7")
                    .key(16, Ease.SMOOTH, "CHEST 8 58 0; ARM_R -95 78 0; move 0 .02 .85")
                    .key(21, Ease.SMOOTH, "PELVIS 0 10 0; CHEST 0 30 0; HEAD 0 -10 0; ARM_R -90 75 0; move 0 0 .9")
                    .key(25, Ease.SNAP, "PELVIS 0 -15 0; CHEST 12 -50 0; HEAD 0 25 0; ARM_R -80 -80 0; HAND_R 60 0 0; ARM_L -25 0 20; move 0 0 1.5")
                    .key(29, Ease.SMOOTH, "CHEST 10 -58 0; ARM_R -82 -92 0; move 0 0 1.6")
                    .key(34, Ease.SMOOTH, "PELVIS 12 5 0; CHEST 22 25 0; HEAD -10 -10 0; ARM_R -25 45 0; FOREARM_R -35 0 0; HAND_R 75 0 0; move 0 -.18 1.7")
                    .key(39, Ease.SNAP, "PELVIS -6 -8 0; CHEST -15 -30 0; HEAD 10 10 0; ARM_R -170 -30 0; FOREARM_R -10 0 0; HAND_R 40 0 0; ARM_L -40 0 50; move 0 .35 2.3")
                    .key(44, Ease.SMOOTH, "CHEST -10 -25 0; ARM_R -160 -28 0; move 0 .1 2.4")
                    .key(50, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 -10 0; HEAD 0 0 0; ARM_R -40 -20 0; FOREARM_R -10 0 0; HAND_R 30 0 0; ARM_L 0 0 0; move 0 0 2.4")
                    .key(60, Ease.SMOOTH, "CHEST 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0")
                    .trail(8, 17).trail(21, 30).trail(34, 41)
                    .build(),
            move("thrust", "Lunging thrust", 50)
                    .key(0, Ease.SMOOTH, "")
                    .key(14, Ease.SMOOTH, "ROOT -6 0 0; PELVIS 6 -15 0; CHEST 10 -35 0; HEAD 0 30 0; ARM_R -55 -35 0; FOREARM_R -95 0 0; HAND_R 75 0 0; ARM_L -50 0 30; move 0 -.2 -.45")
                    .key(19, Ease.SNAP, "ROOT 14 0 0; PELVIS 14 8 0; CHEST 20 10 0; HEAD -10 -5 0; ARM_R -96 4 0; FOREARM_R 0 0 0; HAND_R 53 0 0; ARM_L 20 0 35; move 0 -.12 3.8")
                    .key(26, Ease.SMOOTH, "ROOT 10 0 0; move 0 -.1 4.3")
                    .key(36, Ease.SMOOTH, "ROOT 0 0 0; PELVIS 4 0 0; CHEST 6 0 0; ARM_R -60 0 0; HAND_R 40 0 0; move 0 0 4.4")
                    .key(50, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0")
                    .trail(14, 22)
                    .build(),
            waterfowl(),
            whirlwind(),
            move("leap", "Leaping slam", 70)
                    .key(0, Ease.SMOOTH, "")
                    .key(12, Ease.SMOOTH, "PELVIS 15 0 0; CHEST 25 0 0; HEAD -15 0 0; ARM_R -25 -45 0; HAND_R 55 0 0; ARM_L 0 0 30; move 0 -.35 0")
                    .key(19, Ease.SNAP, "ROOT -30 0 0; PELVIS -5 0 0; CHEST -20 0 0; HEAD 15 0 0; ARM_R -178 0 0; FOREARM_R -35 0 0; HAND_R 40 0 0; ARM_L 0 0 70; move 0 2.8 1.6")
                    .key(29, Ease.SMOOTH, "ROOT 200 0 0; move 0 3.6 3")
                    .key(35, Ease.SNAP, "ROOT 375 0 0; CHEST 35 0 0; ARM_R -40 0 0; FOREARM_R 0 0 0; HAND_R 62 0 0; move 0 1 4.3")
                    .key(39, Ease.LINEAR, "ROOT 372 0 0; PELVIS 18 0 0; HEAD -20 0 0; move 0 -.15 4.5")
                    .key(54, Ease.SMOOTH, "ROOT 362 0 0; move 0 -.05 4.5")
                    .key(70, Ease.SMOOTH, "ROOT 360 0 0; PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 4.5")
                    .trail(19, 37).ring(39)
                    .build(),
            move("rising", "Rising cut", 44)
                    .key(0, Ease.SMOOTH, "")
                    .key(7, Ease.SMOOTH, "PELVIS 12 25 0; CHEST 18 55 0; HEAD -5 -30 0; ARM_R -10 80 0; FOREARM_R -20 0 0; HAND_R 60 0 0; ARM_L -20 0 30; move 0 -.2 .2")
                    .key(11, Ease.SNAP, "PELVIS -8 -20 0; CHEST -20 -50 0; HEAD 10 25 0; ARM_R -175 -40 0; FOREARM_R 0 0 0; HAND_R 40 0 0; ARM_L 10 0 60; move 0 .45 1.2")
                    .key(17, Ease.SMOOTH, "CHEST -18 -45 0; ARM_R -168 -35 0; move 0 .5 1.4")
                    .key(22, Ease.SNAP, "PELVIS 15 0 0; CHEST 30 0 0; HEAD -15 0 0; ARM_R -30 0 0; FOREARM_R -20 0 0; HAND_R 70 0 0; ARM_L -30 0 30; move 0 -.2 2")
                    .key(30, Ease.SMOOTH, "CHEST 26 0 0; ARM_R -25 0 0; move 0 -.15 2.1")
                    .key(44, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 2.1")
                    .trail(7, 13).trail(18, 25).ring(22)
                    .build(),
            flurry(),
            move("dash", "Blade dash", 40)
                    .key(0, Ease.SMOOTH, "")
                    .key(8, Ease.SMOOTH, "PELVIS 20 -10 0; CHEST 25 -30 0; HEAD -20 20 0; ARM_R -20 -60 0; FOREARM_R -20 0 0; HAND_R 60 0 0; ARM_L 20 0 30; move 0 -.25 -.3")
                    .key(16, Ease.SNAP, "ROOT 18 0 0; ARM_R -10 -70 0; move 0 -.15 6.5")
                    .key(21, Ease.SNAP, "ROOT 0 0 0; PELVIS -5 15 0; CHEST -15 40 0; HEAD 10 -15 0; ARM_R -165 40 0; FOREARM_R -10 0 0; HAND_R 40 0 0; ARM_L -30 0 50; move 0 .25 7.2")
                    .key(28, Ease.SMOOTH, "CHEST -10 35 0; ARM_R -150 35 0; move 0 0 7.4")
                    .key(40, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 7.4")
                    .trail(10, 24)
                    .build());

    /** Thrust flurry: five quick thrusts, left and right of centre, each a step further in. */
    private static Move flurry() {
        String coiled = "PELVIS 6 -15 0; CHEST 10 -35 0; ARM_R -55 -35 0; FOREARM_R -95 0 0; HAND_R 75 0 0";
        MoveBuilder m = move("flurry", "Thrust flurry", 56)
                .key(0, Ease.SMOOTH, "")
                .key(8, Ease.SMOOTH, coiled + "; HEAD 0 30 0; ARM_L -50 0 30; move 0 -.15 -.2");
        for (int k = 0; k < 5; k++) {
            int at = 12 + k * 7, side = k % 2 == 0 ? 6 : -6;
            float z = .7f * (k + 1);
            m.key(at, Ease.SNAP, String.format(Locale.ROOT,
                            "ROOT 8 0 0; PELVIS 10 %d 0; CHEST 16 %d 0; HEAD -8 -5 0; ARM_R -94 %d 0; FOREARM_R 0 0 0; HAND_R 53 0 0; move 0 -.1 %.2f",
                            side, side * 2, side, z))
                    .key(at + 4, Ease.SMOOTH, String.format(Locale.ROOT, "ROOT 0 0 0; %s; move 0 -.15 %.2f", coiled, z + .2f))
                    .trail(at - 2, at + 1);
        }
        return m.key(56, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 3.7")
                .build();
    }

    /**
     * The Waterfowl Dance: she coils and leaps, then three flurries in the air, each a whirl of a
     * quarter turn a tick with her blade held out, so it sweeps whole circles, while she circles the
     * area: the first wide and high, the second tilted the other way, the third spiralling down to
     * the ground, the disc of her blade turned on edge, a cascade of slashes. The air round her fills
     * with slashes the whole time. She lands in a crouch that shakes the ground.
     */
    private static Move waterfowl() {
        MoveBuilder m = move("waterfowl", "Waterfowl Dance", 140)
                .key(0, Ease.SMOOTH, "")
                .key(12, Ease.SMOOTH, "PELVIS 18 0 0; CHEST 30 -60 0; HEAD -10 30 0; ARM_R -30 -110 0; FOREARM_R -25 0 0; HAND_R 55 0 0; ARM_L -30 0 30; move 0 -.4 0")
                .key(20, Ease.SNAP, "ROOT 0 120 0; PELVIS -5 0 0; CHEST -10 0 0; HEAD 0 0 0; ARM_R -80 -40 -50; FOREARM_R 0 0 0; HAND_R 53 0 0; ARM_L 0 0 60; move 0 2.8 .6");
        // centre x, y from, y to, centre z, radius from, radius to, pitch, roll of her tilt
        float[][] flurries = {{0f, 2.8f, 2.2f, 1.4f, 1.3f, 1.3f, 12f, 25f}, {.6f, 2.6f, 1.8f, 2.6f, 1.5f, 1.3f, 22f, -30f},
                {0f, 2.0f, .4f, 3.4f, 1.6f, .3f, 48f, 0f}};
        int tick = 20, turn = 120;
        for (int f = 0; f < flurries.length; f++) {
            float[] fl = flurries[f];
            for (int k = 1; k <= 13; k++) {
                tick += 2;
                turn += 180;
                float u = k / 13f;
                double around = u * Math.PI * 2 * (f == 1 ? -1 : 1);
                float r = fl[4] + (fl[5] - fl[4]) * u;
                float x = fl[0] + r * (float) Math.sin(around), z = fl[3] - r * (float) Math.cos(around);
                float y = fl[1] + (fl[2] - fl[1]) * u;
                m.key(tick, Ease.LINEAR, String.format(Locale.ROOT,
                        "ROOT %s %d %s; ARM_R %d 0 -88; CHEST 0 %d 0; HAND_R 53 0 0; move %.2f %.2f %.2f",
                        fl[6], turn, fl[7], k % 2 == 0 ? -6 : 6, k % 2 == 0 ? 12 : -12, x, y, z));
            }
            if (f < flurries.length - 1) {
                tick += 6;
                float[] next = flurries[f + 1];
                turn += 180;
                m.key(tick, Ease.SMOOTH, String.format(Locale.ROOT, "ROOT 0 %d 0; ARM_R -70 -60 -30; CHEST 0 0 0; move %.2f %.2f %.2f",
                        turn, next[0], next[1] + .3f, next[3] - next[4]));
            }
        }
        int facing = (int) Math.ceil(turn / 360.0) * 360;
        return m.key(tick + 6, Ease.SNAP, String.format(Locale.ROOT,
                        "ROOT 0 %d 0; PELVIS 18 0 0; CHEST 30 0 0; HEAD -15 0 0; ARM_R -45 -20 0; FOREARM_R -15 0 0; HAND_R 55 0 0; ARM_L -20 0 40; move 0 -.3 3.4", facing))
                .key(140, Ease.SMOOTH, String.format(Locale.ROOT,
                        "ROOT 0 %d 0; PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 3.4", facing))
                .trail(20, tick).slashes(22, 46).slashes(52, 76).slashes(82, tick).ring(tick + 6)
                .build();
    }

    /** Whirlwind: she coils and spins across the ground, four turns with her blade held out, and turns out of it. */
    private static Move whirlwind() {
        MoveBuilder m = move("whirlwind", "Whirlwind", 64)
                .key(0, Ease.SMOOTH, "")
                .key(10, Ease.SMOOTH, "PELVIS 10 -20 0; CHEST 12 -70 0; HEAD 0 40 0; ARM_R -70 -100 0; FOREARM_R -20 0 0; HAND_R 55 0 0; ARM_L 0 0 40; move 0 -.2 0");
        int turn = 0;
        for (int k = 1; k <= 12; k++) {
            turn += 120;
            m.key(10 + k * 2, Ease.LINEAR, String.format(Locale.ROOT, "ROOT 0 %d %d; ARM_R %d 0 -88; CHEST 0 0 0; HEAD 0 0 0; HAND_R 53 0 0; move 0 %.2f %.2f",
                    turn, k % 2 == 0 ? 6 : -6, k % 2 == 0 ? -4 : 4, -.1f + .08f * (float) Math.sin(k), k * .26f));
        }
        return m.key(42, Ease.SMOOTH, String.format(Locale.ROOT, "ROOT 0 %d 0; CHEST 5 35 0; HEAD 0 -20 0; ARM_R -80 65 0; move 0 0 3.3", turn))
                .key(64, Ease.SMOOTH, String.format(Locale.ROOT,
                        "ROOT 0 %d 0; PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; move 0 0 3.3", turn))
                .trail(10, 40).slashes(12, 36)
                .build();
    }

    private MaleniaRig() {
    }

    public static Move move(String id) {
        return MOVES.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
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

    /**
     * How she moves at {@code tick}, from the tick before: her travel and its speed across the ground,
     * that velocity in her own frame ({@code sideways}, {@code forward}), how fast she falls and spins,
     * how fast her body turns, and how high (or, crouching, how low) she is.
     */
    public record Motion(float speed, float sideways, float forward, float fall, float spin, float turning, float height, float distance) {
    }

    public static Motion motion(Move move, float tick) {
        float before = Math.max(0, tick - 1);
        float[] now = sample(move.travel(), tick), then = sample(move.travel(), before);
        float dx = now[0] - then[0], dz = now[2] - then[2];
        float rootYaw = sample(move.bones().get(Bone.ROOT), tick)[1];
        float spin = Math.abs(rootYaw - sample(move.bones().get(Bone.ROOT), before)[1]);
        float pelvisYaw = sample(move.bones().get(Bone.PELVIS), tick)[1];
        float turning = sample(move.bones().get(Bone.CHEST), tick)[1] - sample(move.bones().get(Bone.CHEST), before)[1]
                + pelvisYaw - sample(move.bones().get(Bone.PELVIS), before)[1];
        double yaw = Math.toRadians(rootYaw + pelvisYaw);
        float sideways = (float) (dx * Math.cos(yaw) - dz * Math.sin(yaw)), forward = (float) (dx * Math.sin(yaw) + dz * Math.cos(yaw));
        return new Motion((float) Math.hypot(dx, dz), sideways, forward, Math.max(0, then[1] - now[1]), spin, turning, now[1],
                (float) Math.hypot(now[0], now[2]));
    }

    /**
     * How the cape ({@code hair} false) or the hair trails her at {@code tick}, as a turn: it streams
     * back the faster she travels, falls or spins, sways a little at rest, and lags behind when her body
     * turns. The viewer computes the same.
     */
    public static float[] follow(Move move, float tick, boolean hair) {
        Motion m = motion(move, tick);
        float lift = hair ? Math.min(60, m.speed() * 110 + m.fall() * 50 + m.spin() * .4f) + 3 * (float) Math.sin(tick * .17)
                : Math.min(60, m.speed() * 140 + m.fall() * 60 + m.spin() * .4f) + 4 * (float) Math.sin(tick * .15);
        float lag = Math.max(-30, Math.min(30, -m.turning() * (hair ? 1.2f : 1.5f)));
        return new float[]{lift, lag, 0};
    }

    /**
     * Her legs at {@code tick} (thigh right, shin right, thigh left, shin left; each a pitch), unless
     * the move keys them: on the ground they stride as she travels, stand upright however she leans,
     * and bend at the knee when she crouches so her feet stay on the floor; in the air they tuck up.
     */
    public static float[] legs(Move move, float tick) {
        Motion m = motion(move, tick);
        float grounded = Math.max(0, Math.min(1, 1 - (m.height() - .15f) / .4f));
        float crouch = m.height() < 0 ? (float) Math.toDegrees(Math.acos(Math.max(-1, 1 + m.height() / 1.05f))) : 0;
        float stride = Math.min(30, m.speed() * 120) * grounded * (float) Math.sin(m.distance() / .5 * Math.PI);
        float upright = -(sample(move.bones().get(Bone.PELVIS), tick)[0] + sample(move.bones().get(Bone.ROOT), tick)[0]) * grounded;
        float air = 1 - grounded;
        return new float[]{
                -crouch + stride + upright - 45 * air, 2 * crouch + Math.max(0, -stride) * .6f + 85 * air,
                -crouch - stride + upright - 20 * air, 2 * crouch + Math.max(0, stride) * .6f + 50 * air};
    }

    /** Every bone's matrix at {@code tick} of {@code move}: parent, then its pivot, its turn (yaw, pitch, roll) and back. */
    public static Matrix4f[] bones(Move move, float tick) {
        return bones(move, tick, new Matrix4f());
    }

    /**
     * {@link #bones(Move, float)} with the whole body first placed by {@code carry}: where the boss
     * stands and faces relative to its displays, so walking and turning never teleport them.
     */
    public static Matrix4f[] bones(Move move, float tick, Matrix4f carry) {
        Matrix4f[] world = new Matrix4f[Bone.values().length];
        float[] travel = sample(move.travel(), tick);
        float[] legs = null;
        for (Bone bone : Bone.values()) {
            List<Key> keys = move.bones().get(bone);
            float[] turn;
            if (keys != null) {
                turn = sample(keys, tick);
            } else if (bone == Bone.CAPE || bone == Bone.HAIR) {
                turn = follow(move, tick, bone == Bone.HAIR);
            } else if (bone.ordinal() >= Bone.LEG_R.ordinal()) {
                if (legs == null) legs = legs(move, tick);
                turn = new float[]{legs[bone.ordinal() - Bone.LEG_R.ordinal()], 0, 0};
            } else {
                turn = sample(null, tick);
            }
            Matrix4f m = bone.parent == null ? new Matrix4f(carry).translate(travel[0], travel[1], travel[2])
                    : new Matrix4f(world[bone.parent.ordinal()]);
            Vector3f p = bone.pivot;
            m.translate(p).rotateY((float) Math.toRadians(turn[1])).rotateX((float) Math.toRadians(turn[0]))
                    .rotateZ((float) Math.toRadians(turn[2])).translate(-p.x, -p.y, -p.z);
            world[bone.ordinal()] = m;
        }
        return world;
    }

    /**
     * The skirt at {@code tick}: how far every tile of the robe swings out from the waist is worked
     * out per tile from these. It opens as she spins, billows as she falls, pools as she crouches,
     * trails behind the way she moves, is pushed forward by her striding legs and sways a little.
     */
    public record Flow(float open, float dragSideways, float dragForward, float legRight, float legLeft, float tick) {
    }

    public static Flow flow(Move move, float tick, Matrix4f[] world) {
        Motion m = motion(move, tick);
        float open = Math.min(35, m.spin() * .35f) + Math.min(40, m.fall() * 70) + (m.height() < 0 ? Math.min(55, -m.height() * 120) : 0);
        float[] legs = legs(move, tick);
        List<Key> right = move.bones().get(Bone.LEG_R), left = move.bones().get(Bone.LEG_L);
        // Wrapped, since a somersault leaves her legs a whole turn round: -390 is -30 to the robe.
        return new Flow(open, m.sideways() * 60, m.forward() * 60,
                (float) Math.IEEEremainder(right != null ? sample(right, tick)[0] : legs[0], 360),
                (float) Math.IEEEremainder(left != null ? sample(left, tick)[0] : legs[2], 360), tick);
    }

    /**
     * Where a point of the robe at rest goes: a tilt out from the waist about the tangent at its angle
     * round her, by as much as {@code flow} swings that side. The swing changes smoothly round her, so
     * the cloth bends as one piece instead of tearing into columns.
     */
    public static Vector3f drape(Vector3f p, Flow flow) {
        double a = Math.atan2(p.x, p.z);
        float sin = (float) Math.sin(a), cos = (float) Math.cos(a);
        float trail = -(sin * flow.dragSideways() + cos * flow.dragForward());
        // Blended across the front, so the two legs never split the panels down the middle.
        float leg = flow.legRight() + (flow.legLeft() - flow.legRight()) * (1 + sin) / 2;
        float swing = flow.open() + Math.max(-20, Math.min(45, trail)) + Math.max(0, -leg) * .5f * Math.max(0, cos)
                + 2 * (float) Math.sin(flow.tick() * .1 + a * 2);
        swing = Math.max(-15, Math.min(70, swing));
        Vector3f pivot = new Vector3f(.16f * sin, 1.49f, .124f * cos);
        return new Vector3f(p).sub(pivot).rotateAxis((float) Math.toRadians(-swing), cos, 0, -sin).add(pivot);
    }

    /**
     * The matrix that carries one tile of the skirt: the pelvis, then {@link #drape} near the tile,
     * as an affine map (where its centre goes and how the cloth round it bends and stretches).
     */
    public static Matrix4f skirt(Matrix4f pelvis, Piece piece, Flow flow) {
        Vector3f c = piece.center();
        float h = .01f;
        Vector3f[] d = new Vector3f[3];
        for (int k = 0; k < 3; k++) {
            Vector3f step = new Vector3f().setComponent(k, h);
            d[k] = drape(new Vector3f(c).add(step), flow).sub(drape(new Vector3f(c).sub(step), flow)).div(2 * h);
        }
        Matrix4f near = new Matrix4f(d[0].x, d[0].y, d[0].z, 0, d[1].x, d[1].y, d[1].z, 0, d[2].x, d[2].y, d[2].z, 0, 0, 0, 0, 1);
        Vector3f to = drape(c, flow);
        return new Matrix4f(pelvis).translate(to).mul(near).translate(-c.x, -c.y, -c.z);
    }

    /** Where every piece is at {@code tick} of {@code move}. */
    public static List<Transformation> pose(Move move, float tick, List<Piece> pieces, int[] owners) {
        return pose(move, tick, pieces, owners, new Matrix4f());
    }

    /** {@link #pose(Move, float, List, int[])} with the body placed by {@code carry} ({@link #bones(Move, float, Matrix4f)}). */
    public static List<Transformation> pose(Move move, float tick, List<Piece> pieces, int[] owners, Matrix4f carry) {
        Matrix4f[] world = bones(move, tick, carry);
        Flow flow = flow(move, tick, world);
        List<Transformation> out = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            out.add(owners[i] == Bone.SKIRT.ordinal() ? stretched(pieces.get(i), skirt(world[Bone.PELVIS.ordinal()], pieces.get(i), flow))
                    : posed(pieces.get(i), world[owners[i]]));
        }
        return out;
    }

    /**
     * A piece carried by a matrix that may stretch it, as a skirt tile is to close the gaps when the
     * robe flares: turned by the nearest rotation, each edge stretched as much as the cloth along it,
     * centred where the matrix puts its centre. The little shear left out keeps the game's
     * interpolation steady.
     */
    public static Transformation stretched(Piece piece, Matrix4f carrier) {
        Transformation rest = piece.transformation();
        Matrix3f m = carrier.get3x3(new Matrix3f());
        Matrix3f q = new Matrix3f(m);
        for (int i = 0; i < 10; i++) q.add(new Matrix3f(q).invert().transpose()).scale(.5f);
        Matrix3f stretch = new Matrix3f(q).transpose().mul(m);
        Quaternionf rotation = q.getNormalizedRotation(new Quaternionf()).mul(rest.getLeftRotation());
        Vector3f size = new Vector3f(rest.getScale());
        for (int k = 0; k < 3; k++) {
            Vector3f edge = rest.getLeftRotation().transform(new Vector3f().setComponent(k, 1));
            size.setComponent(k, size.get(k) * stretch.transform(edge).length());
        }
        Vector3f centre = carrier.transformPosition(new Vector3f(piece.center()));
        Vector3f translation = centre.sub(rotation.transform(new Vector3f(size).mul(.5f)));
        return new Transformation(translation, rotation, size, new Quaternionf());
    }

    /** The bone every piece of a model rides on. */
    public static int[] owners(List<Piece> pieces) {
        int[] owners = new int[pieces.size()];
        for (int i = 0; i < owners.length; i++) owners[i] = Bone.valueOf(pieces.get(i).part()).ordinal();
        return owners;
    }

    /** A piece carried by its bone's matrix: turned and moved, never stretched. */
    public static Transformation posed(Piece piece, Matrix4f bone) {
        Transformation rest = piece.transformation();
        Vector3f translation = bone.transformPosition(new Vector3f(rest.getTranslation()));
        Quaternionf rotation = bone.getNormalizedRotation(new Quaternionf()).mul(rest.getLeftRotation());
        return new Transformation(translation, rotation, rest.getScale(), new Quaternionf());
    }

    // =====================================================================================
    // In the world
    // =====================================================================================

    /**
     * Plays an attack in front of a viewer, side on so its travel crosses their view: the body is
     * spawned, posed every two ticks with the game's own interpolation between, its slashes and impacts
     * heard and seen, and removed a couple of seconds after the attack ends.
     */
    public static void preview(Plugin plugin, Player viewer, Move move) {
        List<Piece> pieces = MaleniaModel.pieces();
        int[] owners = owners(pieces);
        Location eye = viewer.getLocation();
        double look = Math.toRadians(eye.getYaw());
        float yaw = eye.getYaw() + 90f;
        double facing = Math.toRadians(yaw);
        // Eight blocks ahead, started back along her heading so the attack plays out in front.
        Location feet = eye.clone().add(-Math.sin(look) * 8 + Math.sin(facing) * 3, 0, Math.cos(look) * 8 - Math.cos(facing) * 3);
        List<BlockDisplay> body = MaleniaModel.spawn(pieces, feet, yaw);
        int step = 2;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick += step;
                if (tick > move.ticks() + 40) {
                    body.forEach(BlockDisplay::remove);
                    cancel();
                    return;
                }
                float t = Math.min(tick, move.ticks());
                List<Transformation> posed = pose(move, t, pieces, owners);
                for (int i = 0; i < body.size(); i++) MaleniaModel.animate(body.get(i), posed.get(i), step);
                effects(feet, yaw, move, t, bones(move, t), step);
            }
        }.runTaskTimer(plugin, 1L, step);
    }

    private static final Particle.DustOptions GOLD = new Particle.DustOptions(Color.fromRGB(255, 214, 120), 1.1f);

    /**
     * What {@code move} sounds and looks like at {@code t}, played every {@code step} ticks for displays
     * at {@code feet} facing {@code yaw} whose bones are {@code world}: sword trails, the slashes filling
     * the air round her, the shockwaves.
     */
    static void effects(Location feet, float yaw, Move move, float t, Matrix4f[] world, int step) {
        Location root = toWorld(feet, yaw, world[Bone.ROOT.ordinal()].transformPosition(new Vector3f(0, .05f, 0)));
        for (int[] trail : move.trails()) {
            if (t == trail[0] || t == trail[0] + 1) feet.getWorld().playSound(root, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.3f);
            if (t >= trail[0] && t <= trail[1]) {
                Vector3f tip = world[Bone.HAND_R.ordinal()].transformPosition(MaleniaModel.bladePoint(1f));
                feet.getWorld().spawnParticle(Particle.DUST, toWorld(feet, yaw, tip), 4, .05, .05, .05, 0, GOLD);
            }
        }
        for (int[] range : move.slashes()) {
            if (t >= range[0] && t <= range[1]) {
                Location centre = toWorld(feet, yaw, world[Bone.ROOT.ordinal()].transformPosition(new Vector3f(0, 1.3f, 0)));
                feet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, centre, 5, 1.4, 1.0, 1.4, 0);
                if (t % 4 == 0) feet.getWorld().playSound(centre, Sound.ENTITY_PLAYER_ATTACK_SWEEP, .7f, 1.6f);
            }
        }
        for (int at : move.rings()) {
            if (at > t - step && at <= t) {
                feet.getWorld().playSound(root, Sound.ENTITY_GENERIC_EXPLODE, .8f, 1.4f);
                feet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, root, 14, 1.6, .1, 1.6, 0);
            }
        }
    }

    /** A point of the model in the world: turned by the display's yaw and moved to its feet. */
    static Location toWorld(Location feet, float yaw, Vector3f p) {
        Vector3f turned = new Quaternionf().rotateY((float) Math.toRadians(-yaw)).transform(new Vector3f(p));
        return feet.clone().add(turned.x, turned.y, turned.z);
    }

    // =====================================================================================
    // Authoring
    // =====================================================================================

    private static MoveBuilder move(String id, String label, int ticks) {
        return new MoveBuilder(id, label, ticks);
    }

    /**
     * Builds a move from compact keys: {@code "ARM_R -85 55 0; CHEST 0 40 0; move 0 0 .5"} sets those
     * bones' pitch, yaw and roll and where the root has travelled to at that tick; bones a key leaves
     * out keep easing between their own keys. An empty key holds every bone at rest (the cape, hair,
     * skirt and legs excepted: they follow her on their own).
     */
    private static final class MoveBuilder {
        private final String id, label;
        private final int ticks;
        private final Map<Bone, List<Key>> bones = new EnumMap<>(Bone.class);
        private final List<Key> travel = new ArrayList<>();
        private final List<int[]> trails = new ArrayList<>(), slashes = new ArrayList<>();
        private final List<Integer> rings = new ArrayList<>();

        MoveBuilder(String id, String label, int ticks) {
            this.id = id;
            this.label = label;
            this.ticks = ticks;
        }

        MoveBuilder key(int tick, Ease ease, String spec) {
            if (spec.isBlank()) {
                for (Bone bone : Bone.values()) {
                    if (bone.ordinal() < Bone.CAPE.ordinal() || bone.ordinal() > Bone.HAIR.ordinal() && bone.ordinal() < Bone.SKIRT.ordinal()) {
                        bones.computeIfAbsent(bone, b -> new ArrayList<>()).add(new Key(tick, 0, 0, 0, ease));
                    }
                }
                travel.add(new Key(tick, 0, 0, 0, ease));
                return this;
            }
            for (String part : spec.split(";")) {
                String[] w = part.trim().split("\\s+");
                Key key = new Key(tick, Float.parseFloat(w[1]), Float.parseFloat(w[2]), Float.parseFloat(w[3]), ease);
                if (w[0].equals("move")) travel.add(key);
                else bones.computeIfAbsent(Bone.valueOf(w[0]), b -> new ArrayList<>()).add(key);
            }
            return this;
        }

        MoveBuilder trail(int from, int to) {
            trails.add(new int[]{from, to});
            return this;
        }

        MoveBuilder slashes(int from, int to) {
            slashes.add(new int[]{from, to});
            return this;
        }

        MoveBuilder ring(int tick) {
            rings.add(tick);
            return this;
        }

        Move build() {
            Map<Bone, List<Key>> frozen = new LinkedHashMap<>();
            bones.forEach((bone, keys) -> frozen.put(bone, List.copyOf(keys)));
            return new Move(id, label, ticks, frozen, List.copyOf(travel), List.copyOf(trails), List.copyOf(slashes), List.copyOf(rings));
        }
    }
}
