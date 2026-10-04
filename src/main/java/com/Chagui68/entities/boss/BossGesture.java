package com.Chagui68.entities.boss;

import org.joml.Quaternionf;

/**
 * The body language of the arsenal attacks JackStar and NIX cast on top of their signature moves:
 * a handful of held poses, each eased in from rest, held while the attack channels and eased back
 * out, so any attack of any length gets a body that moves without a snap.
 *
 * <p>Limbs are named by their group's {@code name()} so both bosses, whose limb enums differ only in
 * owner, share one table. Sign conventions follow their models: a positive X rotation swings an arm
 * forward ({@code PI} holds it straight up), a positive Z lifts the right arm out to the side and a
 * negative one the left. A positive elbow bend folds the forearm forward; a negative knee bend folds
 * the shin back.
 */
public enum BossGesture {
    /** Both arms thrown up at the sky. */
    CAST(new float[][]{{2.7f, 0, 0.25f}, {2.7f, 0, -0.25f}, {-0.35f, 0, 0}, {-0.1f, 0, 0}, {0, 0, 0}, {0, 0, 0}},
            0.3f, 0f),
    /** The right arm aimed straight at the target, the left held back. */
    POINT(new float[][]{{1.55f, 0, 0}, {-0.4f, 0, -0.1f}, {0, 0, 0}, {0, -0.2f, 0}, {0, 0, 0}, {0, 0, 0}},
            0.1f, 0f),
    /** Both arms driven forward from a braced stance. */
    THRUST(new float[][]{{1.45f, 0, -0.15f}, {1.45f, 0, 0.15f}, {0.05f, 0, 0}, {0.15f, 0, 0}, {-0.3f, 0, 0}, {0.3f, 0, 0}},
            0f, -0.3f),
    /** The right arm swept across the body, the torso turning with it. */
    SWEEP(new float[][]{{1.0f, -1.2f, 0.3f}, {-0.3f, 0, 0}, {0, 0.15f, 0}, {0, 0.35f, 0}, {0, 0, 0}, {0, 0, 0}},
            0.2f, 0f),
    /** A gathered spring: knees bent, arms swept back, the body leaning in. */
    CROUCH(new float[][]{{-0.8f, 0, 0}, {-0.8f, 0, 0}, {-0.2f, 0, 0}, {0.35f, 0, 0}, {0.6f, 0, 0}, {0.6f, 0, 0}},
            0.4f, -1.0f),
    /** Arms flung out to the sides. */
    SPREAD(new float[][]{{0.2f, 0, 1.45f}, {0.2f, 0, -1.45f}, {-0.2f, 0, 0}, {-0.1f, 0, 0}, {0, 0, 0}, {0, 0, 0}},
            0.1f, 0f);

    /** Longest ease in or out, in ticks; short attacks use a quarter of their length each way. */
    static final int MAX_RAMP = 8;

    /** Euler angles (X, Y, Z) per limb, in the order of {@link #slot(String)}. */
    private final float[][] angles;
    private final float armBend;
    private final float legBend;

    BossGesture(float[][] angles, float armBend, float legBend) {
        this.angles = angles;
        this.armBend = armBend;
        this.legBend = legBend;
    }

    /** The rotation of the limb group named {@code group} on tick {@code t} of a {@code duration}-tick channel. */
    public Quaternionf limb(String group, int t, int duration) {
        int slot = slot(group);
        Quaternionf q = new Quaternionf();
        if (slot < 0) return q;
        float e = envelope(t, duration);
        float[] a = angles[slot];
        return q.rotateX(a[0] * e).rotateY(a[1] * e).rotateZ(a[2] * e);
    }

    /** How far an elbow ({@code arm}) or a knee folds on tick {@code t}. */
    public float bend(boolean arm, int t, int duration) {
        return (arm ? armBend : legBend) * envelope(t, duration);
    }

    /** 0 at both ends of the channel, 1 through the middle, eased with a smoothstep each way. */
    static float envelope(int t, int duration) {
        if (duration <= 1 || t < 0 || t >= duration) return 0f;
        int ramp = Math.max(1, Math.min(MAX_RAMP, duration / 4));
        float up = smooth(t / (float) ramp);
        float down = smooth((duration - 1 - t) / (float) ramp);
        return Math.min(up, down);
    }

    private static float smooth(float x) {
        float c = Math.max(0f, Math.min(1f, x));
        return c * c * (3 - 2 * c);
    }

    private static int slot(String group) {
        return switch (group) {
            case "ARM_RIGHT" -> 0;
            case "ARM_LEFT" -> 1;
            case "HEAD" -> 2;
            case "TORSO_UPPER" -> 3;
            case "LEG_RIGHT" -> 4;
            case "LEG_LEFT" -> 5;
            default -> -1;
        };
    }
}
