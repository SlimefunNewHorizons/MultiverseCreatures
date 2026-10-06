package com.Chagui68.entities.boss.witherstorm;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Wither Storm's five forms for the model viewer in {@code tools/stand-viewer}: every box with the
 * transformation the plugin sends to the game, in a handful of poses and two animations, plus where
 * the tractor beams and the hitboxes go. {@code StandViewerExport} writes them into the same file as
 * the Stands.
 */
public final class WitherStormViewerExport {

    /** Ticks between two exported animation frames; the viewer plays them at the same rate. */
    static final int FRAME_TICKS = 3;
    static final int FRAMES = 40;

    private WitherStormViewerExport() {
    }

    /** One JSON object per form, comma separated, ready to go into the viewer's model list. */
    public static String entries() {
        StringBuilder out = new StringBuilder();
        for (WitherStormForm form : WitherStormForm.values()) {
            if (out.length() > 0) out.append(',');
            out.append(entry(form, form.model(), "", ""));
        }
        // high-detail: true draws the Destroyer with the mod's full mass.
        out.append(',').append(entry(WitherStormForm.DESTROYER, WitherStormForm.DESTROYER.model(true),
                "-detailed", " (high detail)"));
        return out.toString();
    }

    static String entry(WitherStormForm form, WitherStormModel model, String keySuffix, String nameSuffix) {
        Map<String, WitherStormModel.Pose> poses = poses(model);
        StringBuilder json = new StringBuilder();
        json.append("{\"key\":\"wither-storm-").append(form.key()).append(keySuffix).append("\",\"name\":\"Wither Storm: ")
                .append(form.displayName()).append(nameSuffix).append("\",\"kind\":\"blocks\",\"group\":\"Wither Storm\"");
        float[] b = model.bounds(new WitherStormModel.Pose(), 1f);
        json.append(",\"size\":").append(array(b));
        json.append(",\"boxes\":").append(model.boxes().size());

        json.append(",\"poses\":{");
        boolean first = true;
        for (Map.Entry<String, WitherStormModel.Pose> pose : poses.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(pose.getKey()).append("\":").append(pieces(model, pose.getValue()));
        }
        json.append('}');

        json.append(",\"frames\":{");
        json.append("\"idle\":").append(animation(model, (pose, tick) -> { }));
        json.append(",\"roar\":").append(animation(model, (pose, tick) -> {
            float t = tick / (float) (FRAMES * FRAME_TICKS);
            float open = (float) Math.sin(Math.min(1, t * 1.6) * Math.PI);
            for (int i = 0; i < 3; i++) {
                pose.mouth[i] = 2f * open;
                pose.headPitch[i] = -12f * open;
            }
        }));
        json.append(",\"turning\":").append(animation(model, (pose, tick) -> {
            float a = (float) Math.sin(tick / 20.0);
            for (int i = 0; i < 3; i++) {
                pose.headYaw[i] = 55f * a * (i == 1 ? -1 : 1);
                pose.headPitch[i] = 20f + 15f * (float) Math.cos(tick / 15.0);
            }
        }));
        json.append('}');

        json.append(",\"beams\":{");
        first = true;
        for (Map.Entry<String, WitherStormModel.Pose> pose : poses.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append(pose.getKey()).append("\":").append(beams(form, model, pose.getValue()));
        }
        json.append('}');
        json.append(",\"hitboxes\":").append(hitboxes(model));
        json.append('}');
        return json.toString();
    }

    private static Map<String, WitherStormModel.Pose> poses(WitherStormModel model) {
        Map<String, WitherStormModel.Pose> poses = new LinkedHashMap<>();
        poses.put("idle", new WitherStormModel.Pose());
        WitherStormModel.Pose roar = new WitherStormModel.Pose();
        WitherStormModel.Pose down = new WitherStormModel.Pose();
        WitherStormModel.Pose left = new WitherStormModel.Pose();
        WitherStormModel.Pose dead = new WitherStormModel.Pose();
        for (int i = 0; i < 3; i++) {
            roar.mouth[i] = 2f;
            roar.headPitch[i] = -10f;
            down.headPitch[i] = 45f;
            down.mouth[i] = 0.6f;
            left.headYaw[i] = 50f;
            dead.headPitch[i] = 55f;
            dead.mouth[i] = 1.2f;
            dead.headYaw[i] = i == 1 ? 25f : i == 2 ? -25f : 0f;
        }
        dead.brokenJaw = 1.5f;
        dead.bodyPitch = -12f;
        poses.put("roar", roar);
        poses.put("look down", down);
        poses.put("look left", left);
        poses.put("playing dead", dead);
        return poses;
    }

    private interface Animator {
        void at(WitherStormModel.Pose pose, int tick);
    }

    private static String animation(WitherStormModel model, Animator animator) {
        StringBuilder out = new StringBuilder("[");
        for (int f = 0; f < FRAMES; f++) {
            int tick = f * FRAME_TICKS;
            WitherStormModel.Pose pose = new WitherStormModel.Pose();
            pose.ticks = tick;
            pose.tentacleTicks = tick;
            animator.at(pose, tick);
            if (f > 0) out.append(',');
            out.append(pieces(model, pose));
        }
        return out.append(']').toString();
    }

    private static String pieces(WitherStormModel model, WitherStormModel.Pose pose) {
        WitherStormModel.Placement[] placements = model.place(pose, 1f);
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < placements.length; i++) {
            WitherStormModel.Placement p = placements[i];
            WitherStormModel.Box box = model.boxes().get(i);
            if (i > 0) out.append(',');
            if (p == null) {
                out.append("null");
                continue;
            }
            out.append("{\"k\":\"").append(box.skull() ? "s" : "b").append("\",\"m\":\"").append(box.material())
                    .append("\",\"g\":").append(box.glow() ? 1 : 0)
                    .append(String.format(Locale.ROOT, ",\"t\":[%.3f,%.3f,%.3f],\"q\":[%.4f,%.4f,%.4f,%.4f],\"s\":[%.3f,%.3f,%.3f]}",
                            p.translation().x, p.translation().y, p.translation().z,
                            p.rotation().x, p.rotation().y, p.rotation().z, p.rotation().w,
                            p.scale().x, p.scale().y, p.scale().z));
        }
        return out.append(']').toString();
    }

    /** Each beam head's mouth, its direction and how far and wide its beam goes, at the form's defaults. */
    private static String beams(WitherStormForm form, WitherStormModel model, WitherStormModel.Pose pose) {
        Matrix4f[] m = model.partMatrices(pose, 1f);
        StringBuilder out = new StringBuilder("[");
        boolean first = true;
        for (WitherStormModel.Gaze g : model.gazes()) {
            if (!g.jawed() && g.index() != 0) continue;
            Matrix4f part = m[g.part()];
            Vector3f centre = WitherStormModel.point(part, g.centre().x, g.centre().y, g.centre().z);
            Vector3f mouth = WitherStormModel.point(part, g.mouth().x, g.mouth().y, g.mouth().z);
            Vector3f dir = new Vector3f(mouth).sub(centre).normalize();
            if (!first) out.append(',');
            first = false;
            out.append(String.format(Locale.ROOT, "{\"o\":[%.3f,%.3f,%.3f],\"d\":[%.4f,%.4f,%.4f],\"l\":%.2f,\"r\":%.2f}",
                    mouth.x, mouth.y, mouth.z, dir.x, dir.y, dir.z, form.beamRange(), form.beamEndRadius()));
        }
        return out.append(']').toString();
    }

    /** The mass's hitbox and one cube per head, as {min, max} in display space at rest. */
    private static String hitboxes(WitherStormModel model) {
        WitherStormModel.Pose rest = new WitherStormModel.Pose();
        float[] mass = model.bounds(rest, 1f, WitherStormModel.Kind.MASS);
        float[] base = model.bounds(rest, 1f, WitherStormModel.Kind.BASE);
        float[] b = mass[0] > mass[3] ? base : base[0] > base[3] ? mass : new float[]{
                Math.min(mass[0], base[0]), Math.min(mass[1], base[1]), Math.min(mass[2], base[2]),
                Math.max(mass[3], base[3]), Math.max(mass[4], base[4]), Math.max(mass[5], base[5])};
        float cx = (b[0] + b[3]) / 2, cz = (b[2] + b[5]) / 2;
        float w = Math.max(1f, Math.min(b[3] - b[0], b[5] - b[2]) * 0.85f) / 2;
        float h = Math.max(1f, (b[4] - b[1]) * 0.9f);
        StringBuilder out = new StringBuilder("[");
        out.append(array(new float[]{cx - w, b[1], cz - w, cx + w, b[1] + h, cz + w}));
        Matrix4f[] m = model.partMatrices(rest, 1f);
        for (WitherStormModel.Gaze g : model.gazes()) {
            Vector3f c = WitherStormModel.point(m[g.part()], g.centre().x, g.centre().y, g.centre().z);
            float half = Math.max(0.8f, WitherStormModel.width(m[g.part()], g)) / 2;
            out.append(',').append(array(new float[]{c.x - half, c.y - half, c.z - half, c.x + half, c.y + half, c.z + half}));
        }
        return out.append(']').toString();
    }

    private static String array(float[] v) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) out.append(',');
            out.append(String.format(Locale.ROOT, "%.3f", v[i]));
        }
        return out.append(']').toString();
    }

}
