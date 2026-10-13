package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Map;

/**
 * The pieces of the Monster's movement that do not depend on the blocks round it: its stride, which
 * way it faces and where its body goes. {@link MonsterScript}
 * plans the visit with them.
 *
 * <p>Positions are world coordinates in doubles; angles about {@code y} are radians, the way
 * {@link MonsterRig} turns the model (its face looks along {@code +z} at 0).</p>
 */
public final class MonsterMotion {

    private MonsterMotion() {
    }

    /** A pose mid stride: legs and arms swinging, a slight lean and sway. */
    public static Pose stride(Pose pose, float stride) {
        float swing = (float) Math.sin(stride) * 28f;
        return pose.plus(Map.of(
                "THIGH_R", new Vector3f(swing, 0, 0), "THIGH_L", new Vector3f(-swing, 0, 0),
                "ARM_R", new Vector3f(-swing * 0.6f, 0, 0), "ARM_L", new Vector3f(swing * 0.6f, 0, 0),
                "TORSO", new Vector3f(8, 0, (float) Math.sin(stride * 0.5) * 4f)));
    }

    /** The angle that turns its face to {@code to} from {@code from}, flat; {@code fallback} when on top. */
    public static float toward(Vector3d from, Vector3d to, float fallback) {
        double dx = to.x - from.x, dz = to.z - from.z;
        return dx * dx + dz * dz < 1.0e-6 ? fallback : (float) Math.atan2(dx, dz);
    }

    /** The angle that faces someone looking along {@code look}: the way they look, turned round. */
    public static float facing(Vector3d look, float fallback) {
        return look.x * look.x + look.z * look.z < 1.0e-6 ? fallback : (float) Math.atan2(-look.x, -look.z);
    }

    /** The matrix that puts a pose's model at {@code anchor}, turned by {@code yaw}. */
    public static Matrix4f root(Vector3d anchor, float yaw, Pose pose) {
        return new Matrix4f().translation((float) anchor.x, (float) anchor.y, (float) anchor.z)
                .rotateY(yaw).mul(MonsterRig.body(pose));
    }
}
