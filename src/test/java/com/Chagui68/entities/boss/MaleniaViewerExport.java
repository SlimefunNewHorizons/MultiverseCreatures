package com.Chagui68.entities.boss;

import com.Chagui68.utils.DisplaySculpt;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Malenia for the viewer in {@code website/public/viewer}: every piece with the transformation the
 * plugin sends to the game and the bone it rides on, her skeleton and her attacks (the keyframes
 * {@link MaleniaRig} plays in the game, which the viewer plays with the same maths).
 * {@code StandViewerExport} writes them into the same file as the Stands and the Wither Storm.
 */
public final class MaleniaViewerExport {

    private MaleniaViewerExport() {
    }

    /** Malenia as a viewer entry, ready to go into the model list. */
    public static String entries() {
        List<DisplaySculpt.Piece> pieces = MaleniaModel.pieces();
        float[] b = MaleniaModelTest.bounds(pieces);
        return "{\"key\":\"malenia\",\"name\":\"Malenia, Blade of Miquella\",\"kind\":\"blocks\",\"group\":\"Bosses\""
                + ",\"size\":" + array(b)
                + ",\"boxes\":" + pieces.size()
                + ",\"poses\":{\"idle\":" + pieces(pieces) + "}"
                + ",\"rig\":" + rig(pieces)
                + ",\"moves\":" + moves(pieces) + "}";
    }

    /** The bones (name, parent, pivot), the bone each piece rides on, and the blade's collar and tip on the hand. */
    private static String rig(List<DisplaySculpt.Piece> pieces) {
        StringBuilder out = new StringBuilder("{\"bones\":[");
        for (MaleniaRig.Bone bone : MaleniaRig.Bone.values()) {
            if (bone.ordinal() > 0) out.append(',');
            out.append(String.format(Locale.ROOT, "[\"%s\",%d,%.4f,%.4f,%.4f]", bone.name(),
                    bone.parent == null ? -1 : bone.parent.ordinal(), bone.pivot.x, bone.pivot.y, bone.pivot.z));
        }
        out.append("],\"owners\":[");
        int[] owners = MaleniaRig.owners(pieces);
        for (int i = 0; i < owners.length; i++) out.append(i > 0 ? "," : "").append(owners[i]);
        Vector3f base = MaleniaModel.bladePoint(0f), tip = MaleniaModel.bladePoint(1f);
        out.append("],\"blade\":{\"bone\":").append(MaleniaRig.Bone.HAND_R.ordinal())
                .append(",\"base\":").append(array(new float[]{base.x, base.y, base.z}))
                .append(",\"tip\":").append(array(new float[]{tip.x, tip.y, tip.z})).append("}}");
        return out.toString();
    }

    private static String moves(List<DisplaySculpt.Piece> pieces) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (MaleniaRig.Move move : MaleniaRig.MOVES) {
            if (!first) out.append(',');
            first = false;
            out.append('"').append(move.id()).append("\":{\"label\":\"").append(move.label()).append("\",\"ticks\":").append(move.ticks());
            out.append(",\"bones\":{");
            boolean firstBone = true;
            for (Map.Entry<MaleniaRig.Bone, List<MaleniaRig.Key>> track : move.bones().entrySet()) {
                if (!firstBone) out.append(',');
                firstBone = false;
                out.append('"').append(track.getKey().ordinal()).append("\":").append(track(track.getValue()));
            }
            out.append("},\"travel\":").append(track(move.travel()));
            out.append(",\"trails\":").append(ranges(move.trails())).append(",\"slashes\":").append(ranges(move.slashes()))
                    .append(",\"rings\":").append(move.rings());
            out.append(",\"size\":").append(array(reach(pieces, move))).append('}');
        }
        return out.append('}').toString();
    }

    /** Everything an attack sweeps through, with room for the slashes round her: what the viewer's camera frames. */
    static float[] reach(List<DisplaySculpt.Piece> pieces, MaleniaRig.Move move) {
        int[] owners = MaleniaRig.owners(pieces);
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int tick = 0; tick <= move.ticks(); tick += 2) {
            Matrix4f[] world = MaleniaRig.bones(move, tick);
            List<Transformation> all = MaleniaRig.pose(move, tick, pieces, owners);
            List<DisplaySculpt.Piece> posed = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i += 3) {
                Transformation t = all.get(i);
                posed.add(new DisplaySculpt.Piece("ROOT", pieces.get(i).material(),
                        t.getTranslation().add(t.getLeftRotation().transform(new Vector3f(t.getScale()).mul(.5f))),
                        t.getLeftRotation(), t.getScale(), false));
            }
            grow(b, MaleniaModelTest.bounds(posed));
            for (int[] range : move.slashes()) {
                if (tick >= range[0] && tick <= range[1]) {
                    Vector3f c = world[0].transformPosition(new Vector3f(0, 1.3f, 0));
                    grow(b, new float[]{c.x - 2.4f, Math.max(0, c.y - 2f), c.z - 2.4f, c.x + 2.4f, c.y + 2f, c.z + 2.4f});
                }
            }
        }
        return b;
    }

    private static void grow(float[] b, float[] by) {
        for (int i = 0; i < 3; i++) {
            b[i] = Math.min(b[i], by[i]);
            b[i + 3] = Math.max(b[i + 3], by[i + 3]);
        }
    }

    private static String ranges(List<int[]> ranges) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < ranges.size(); i++) out.append(i > 0 ? "," : "").append('[').append(ranges.get(i)[0]).append(',').append(ranges.get(i)[1]).append(']');
        return out.append(']').toString();
    }

    /** A track's keys as [tick, a, b, c, ease], the ease as SMOOTH 0, LINEAR 1, SNAP 2. */
    private static String track(List<MaleniaRig.Key> keys) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < keys.size(); i++) {
            MaleniaRig.Key k = keys.get(i);
            if (i > 0) out.append(',');
            out.append(String.format(Locale.ROOT, "[%s,%s,%s,%s,%d]", num(k.tick()), num(k.a()), num(k.b()), num(k.c()), k.ease().ordinal()));
        }
        return out.append(']').toString();
    }

    private static String num(float f) {
        return f == Math.rint(f) ? String.valueOf((int) f) : String.format(Locale.ROOT, "%.3f", f);
    }

    private static String pieces(List<DisplaySculpt.Piece> pieces) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < pieces.size(); i++) {
            DisplaySculpt.Piece piece = pieces.get(i);
            Transformation t = piece.transformation();
            Vector3f p = t.getTranslation(), s = t.getScale();
            Quaternionf q = t.getLeftRotation();
            if (i > 0) out.append(',');
            out.append("{\"k\":\"b\",\"m\":\"").append(piece.material()).append("\",\"g\":").append(piece.glow() ? 1 : 0)
                    .append(String.format(Locale.ROOT, ",\"t\":[%.4f,%.4f,%.4f],\"q\":[%.4f,%.4f,%.4f,%.4f],\"s\":[%.4f,%.4f,%.4f]}",
                            p.x, p.y, p.z, q.x, q.y, q.z, q.w, s.x, s.y, s.z));
        }
        return out.append(']').toString();
    }

    private static String array(float[] v) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) out.append(i > 0 ? "," : "").append(String.format(Locale.ROOT, "%.3f", v[i]));
        return out.append(']').toString();
    }
}
