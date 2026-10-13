package com.Chagui68.stand;

import com.Chagui68.entities.monster.MonsterScript;
import com.Chagui68.entities.monster.MonsterRig;
import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * Writes the Monster for the model viewer: every pose as the matrices of its eighty heads, the same
 * ones {@link MonsterRig} hands the game's displays, shear and all.
 *
 * <p>Each pose comes with its scene: a player standing where the pose was designed round them (facing
 * away, or turned to face it), what the pose rests on (a ceiling, a chest) and a box round all of it
 * for the camera.</p>
 *
 * <p>Each waiting pose also comes with its animation: {@link MonsterMotion#rehearse} plays the visit
 * with the scene's player standing still, and every tick is written as the rig's parameters (where it
 * stands, how it faces, its lift and tilt and every joint's turn). The viewer gets the rig itself and
 * turns those into the heads' matrices, so the frames stay small.</p>
 */
final class ConceptViewerExport {

    /** A player's box round its feet: half its width (arms included), its height and half its depth. */
    private static final float[] PLAYER = {0.375f, 1.875f, 0.125f};
    /** The visit as the game plays it by default: entities.monster.approach-speed and scream-ticks. */
    private static final double SPEED = 0.35;
    private static final int SCREAM_TICKS = 20;

    private ConceptViewerExport() {
    }

    static String entry() {
        MonsterRig rig = MonsterRig.get();
        StringBuilder poses = new StringBuilder(), scenes = new StringBuilder();
        String idleSize = null;
        for (Pose pose : rig.poses().values()) {
            Matrix4f root = MonsterRig.viewer(pose).mul(MonsterRig.body(pose));
            Matrix4f[] heads = rig.heads(pose, root);
            float[] b = MonsterRig.bounds(java.util.List.of(heads));
            float[] p = pose.player();
            b[0] = Math.min(b[0], p[0] - PLAYER[0]);
            b[1] = Math.min(b[1], p[1]);
            b[2] = Math.min(b[2], p[2] - PLAYER[2]);
            b[3] = Math.max(b[3], p[0] + PLAYER[0]);
            b[4] = Math.max(b[4], p[1] + PLAYER[1]);
            b[5] = Math.max(b[5], p[2] + PLAYER[2]);

            StringBuilder list = new StringBuilder();
            for (int i = 0; i < heads.length; i++) {
                list.append(i > 0 ? "," : "").append("{\"skin\":\"")
                        .append(StandViewerExport.skinUrl(rig.textures().get(i))).append("\",\"m\":[");
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 4; col++) {
                        list.append(row + col > 0 ? "," : "").append(String.format(Locale.ROOT, "%.5f", heads[i].get(col, row)));
                    }
                }
                list.append("]}");
            }
            String size = String.format(Locale.ROOT, "[%.3f,%.3f,%.3f,%.3f,%.3f,%.3f]", b[0], b[1], b[2], b[3], b[4], b[5]);
            if (idleSize == null) {
                idleSize = size;
            }
            String name = "\"" + pose.name() + "\":";
            poses.append(poses.length() > 0 ? "," : "").append(name).append('[').append(list).append(']');
            scenes.append(scenes.length() > 0 ? "," : "").append(name).append(String.format(Locale.ROOT,
                    "{\"size\":%s,\"player\":[%.3f,%.3f,%.3f,%.1f],\"place\":\"%s\",\"anchor\":[%.3f,%.3f,%.3f],\"yaw\":%.1f%s}",
                    size, p[0], p[1], p[2], p[3], pose.place().name().toLowerCase(Locale.ROOT),
                    pose.at().x, pose.at().y, pose.at().z, pose.yaw(), (pose.space() == null ? ""
                            : String.format(Locale.ROOT, ",\"space\":[%.1f,%.1f,%.1f]", pose.space()[0], pose.space()[1], pose.space()[2]))
                            + (pose.base() == null ? "" : String.format(Locale.ROOT, ",\"base\":[%.0f,%.0f]", pose.base()[0], pose.base()[1]))
                            + world(pose.world())));
        }
        return String.format(Locale.ROOT, "{\"key\":\"%s\",\"name\":\"%s\",\"kind\":\"heads\",\"group\":\"%s\","
                        + "\"size\":%s,\"scenes\":{%s},\"poses\":{%s},\"rig\":%s,\"anims\":\"monster-anims.json\"}",
                rig.key(), rig.name(), rig.group(), idleSize, scenes, poses, rig(rig));
    }

    /** The blocks of a pose's scene, for the viewer to draw. */
    private static String world(MonsterRig.World world) {
        if (world == null) return "";
        StringBuilder blocks = new StringBuilder(), holes = new StringBuilder();
        for (int i = 0; i < world.blocks().size(); i++) {
            int[] b = world.blocks().get(i);
            blocks.append(i > 0 ? "," : "").append(String.format(Locale.ROOT, "[%d,%d,%d,%d,%d,%d,%d]",
                    b[0], b[1], b[2], b[3], b[4], b[5], world.chests().get(i) ? 1 : 0));
        }
        for (int i = 0; i < world.holes().size(); i++) {
            int[] h = world.holes().get(i);
            holes.append(i > 0 ? "," : "").append(String.format(Locale.ROOT, "[%d,%d,%d]", h[0], h[1], h[2]));
        }
        return ",\"world\":{\"blocks\":[" + blocks + "],\"holes\":[" + holes + "]}";
    }

    /** The rig for the viewer: each part's name, parent and pivot, each head's part and rest matrix. */
    private static String rig(MonsterRig rig) {
        StringBuilder parts = new StringBuilder(), owners = new StringBuilder(), rest = new StringBuilder();
        for (int p = 0; p < rig.partNames().size(); p++) {
            if (rig.parentOf(p) >= p) {
                throw new IllegalStateException(rig.partNames().get(p) + " is listed before its parent");
            }
            Vector3f v = rig.pivot(p);
            parts.append(p > 0 ? "," : "").append(String.format(Locale.ROOT, "[\"%s\",%d,%.3f,%.3f,%.3f]",
                    rig.partNames().get(p), rig.parentOf(p), v.x, v.y, v.z));
        }
        for (int i = 0; i < rig.size(); i++) {
            owners.append(i > 0 ? "," : "").append(rig.ownerOf(i));
            Matrix4f m = rig.rest(i);
            rest.append(i > 0 ? ",[" : "[");
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                    rest.append(row + col > 0 ? "," : "").append(String.format(Locale.ROOT, "%.5f", m.get(col, row)));
                }
            }
            rest.append(']');
        }
        // Where it looks from, on its head as the model was built (idle turns nothing).
        Vector3f eyes = rig.eyes(rig.pose("idle"), new Matrix4f());
        return "{\"parts\":[" + parts + "],\"owners\":[" + owners + "],\"rest\":[" + rest + "],\"eyes\":["
                + String.format(Locale.ROOT, "%.3f,%.3f,%.3f", eyes.x, eyes.y, eyes.z) + "]}";
    }

    /**
     * Every waiting pose's visits, one per behaviour it can really play in its own blocks, tick by tick:
     * phase, shown (1) or not (0), anchor, yaw (degrees), lift, tilt, the player's yaw (degrees), then
     * [pitch, yaw, roll] for each part in the rig's order, then its face's pitch (degrees) and size,
     * size 0 unless only its face is drawn, in the player's face. {@code anims} in the entry names this file.
     */
    static String anims() {
        MonsterRig rig = MonsterRig.get();
        MonsterScript.Settings settings = new MonsterScript.Settings(SPEED, SCREAM_TICKS, 100_000, 30);
        StringBuilder out = new StringBuilder("{");
        boolean firstPose = true;
        for (Pose pose : rig.poses().values()) {
            if (pose.place() == Place.ACTION) continue;
            StringBuilder plays = new StringBuilder();
            for (MonsterScript.Behavior behavior : MonsterScript.Behavior.values()) {
                MonsterScript.Take take = MonsterScript.rehearse(rig, pose, behavior, settings, 7L);
                if (take.played() != behavior) continue;
                StringBuilder list = new StringBuilder();
                for (int i = 0; i < take.frames().size(); i++) {
                    MonsterScript.Frame f = take.frames().get(i);
                    Pose q = f.pose();
                    list.append(i > 0 ? ",[" : "[").append(f.phase().ordinal()).append(',').append(f.visible() ? 1 : 0);
                    append(list, f.anchor().x, f.anchor().y, f.anchor().z);
                    list.append(',').append(degrees(Math.toDegrees(f.yaw())));
                    append(list, q.lift().x, q.lift().y, q.lift().z);
                    list.append(',').append(degrees(q.tilt().x)).append(',').append(degrees(q.tilt().y))
                            .append(',').append(degrees(q.tilt().z));
                    list.append(',').append(degrees(take.looks().get(i)));
                    for (String part : rig.partNames()) {
                        Vector3f t = q.turn(part);
                        list.append(',').append(degrees(t.x)).append(',').append(degrees(t.y)).append(',').append(degrees(t.z));
                    }
                    list.append(',').append(degrees(Math.toDegrees(f.facePitch()))).append(',')
                            .append(String.format(Locale.ROOT, "%.3f", f.faceScale()));
                    list.append(']');
                }
                plays.append(plays.length() > 0 ? "," : "").append('"').append(behavior.id()).append("\":[").append(list).append(']');
            }
            out.append(firstPose ? "" : ",").append('"').append(pose.name()).append("\":{").append(plays).append('}');
            firstPose = false;
        }
        return out.append('}').toString();
    }

    private static void append(StringBuilder out, double x, double y, double z) {
        out.append(String.format(Locale.ROOT, ",%.3f,%.3f,%.3f", x, y, z));
    }

    /** An angle to a tenth of a degree, without a trailing ".0". */
    private static String degrees(double value) {
        long tenths = Math.round(value * 10);
        return tenths % 10 == 0 ? Long.toString(tenths / 10) : String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }
}
