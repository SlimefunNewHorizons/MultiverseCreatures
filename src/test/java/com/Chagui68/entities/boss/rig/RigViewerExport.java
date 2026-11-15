package com.Chagui68.entities.boss.rig;

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
 * Every rigged boss for the viewer in {@code website/public/viewer}: each piece with the
 * transformation the plugin sends to the game and the bone it rides on, the skeleton, and the moves
 * (the keyframes {@link Rig} plays in the game, which the viewer plays with the same maths).
 * {@code StandViewerExport} writes them into the same file as the Stands and the Wither Storm.
 */
public final class RigViewerExport {

    private RigViewerExport() {
    }

    /** Every form of {@link RigForms} as viewer entries, comma separated, ready to go into the model list. */
    public static String entries() {
        StringBuilder out = new StringBuilder();
        for (RigForm form : RigForms.ALL) out.append(out.length() > 0 ? "," : "").append(entry(form));
        return out.toString();
    }

    static String entry(RigForm form) {
        return "{\"key\":\"" + form.key() + "\",\"name\":\"" + form.name() + "\",\"kind\":\"blocks\",\"group\":\"Bosses\""
                + ",\"size\":" + array(bounds(form.pieces()))
                + ",\"boxes\":" + form.pieces().size()
                + ",\"poses\":{\"idle\":" + pieces(form.pieces()) + "}"
                + ",\"rig\":" + rig(form)
                + ",\"moves\":" + moves(form) + "}";
    }

    /** min x, y, z, max x, y, z of the drawn pieces, in blocks. */
    public static float[] bounds(List<DisplaySculpt.Piece> pieces) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (DisplaySculpt.Piece piece : pieces) {
            Transformation t = piece.transformation();
            Matrix4f m = new Matrix4f().translate(t.getTranslation()).rotate(t.getLeftRotation()).scale(t.getScale());
            for (int i = 0; i < 8; i++) {
                Vector3f c = m.transformPosition(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
                b[0] = Math.min(b[0], c.x); b[1] = Math.min(b[1], c.y); b[2] = Math.min(b[2], c.z);
                b[3] = Math.max(b[3], c.x); b[4] = Math.max(b[4], c.y); b[5] = Math.max(b[5], c.z);
            }
        }
        return b;
    }

    /** The bones (name, parent, pivot), the bone each piece rides on, the legs' length, and the weapon's base and tip on its bone. */
    private static String rig(RigForm form) {
        StringBuilder out = new StringBuilder("{\"bones\":[");
        List<Rig.Bone> bones = form.rig().bones();
        for (int i = 0; i < bones.size(); i++) {
            Rig.Bone bone = bones.get(i);
            if (i > 0) out.append(',');
            out.append(String.format(Locale.ROOT, "[\"%s\",%d,%.4f,%.4f,%.4f]", bone.name(), bone.parent(), bone.pivot().x, bone.pivot().y, bone.pivot().z));
        }
        out.append("],\"owners\":[");
        for (int i = 0; i < form.owners().length; i++) out.append(i > 0 ? "," : "").append(form.owners()[i]);
        // Each part that changes hands: its bone, the bone it goes to, its dial, and where it is held there (column by column).
        out.append("],\"carry\":[");
        List<Rig.Carry> carries = form.rig().carries();
        for (int i = 0; i < carries.size(); i++) {
            Rig.Carry c = carries.get(i);
            out.append(i > 0 ? "," : "").append('[').append(c.part()).append(',').append(c.onto()).append(',').append(c.by());
            for (float f : c.held().get(new float[16])) out.append(String.format(Locale.ROOT, ",%.5f", f));
            out.append(']');
        }
        // Each part that can be spent: its bone and its dial.
        out.append("],\"spend\":[");
        List<Rig.Spend> spends = form.rig().spends();
        for (int i = 0; i < spends.size(); i++) out.append(i > 0 ? "," : "").append('[').append(spends.get(i).part()).append(',').append(spends.get(i).by()).append(']');
        Vector3f base = form.weaponBase(), tip = form.weaponTip();
        out.append("],\"leg\":").append(String.format(Locale.ROOT, "%.3f", form.rig().legLength()))
                .append(",\"middle\":").append(String.format(Locale.ROOT, "%.3f", form.middle()))
                .append(",\"blade\":{\"bone\":").append(form.weaponBone())
                .append(",\"base\":").append(array(new float[]{base.x, base.y, base.z}))
                .append(",\"tip\":").append(array(new float[]{tip.x, tip.y, tip.z}))
                .append(",\"color\":").append(array(new float[]{form.trail().getRed() / 255f, form.trail().getGreen() / 255f, form.trail().getBlue() / 255f}))
                .append("}}");
        return out.toString();
    }

    private static String moves(RigForm form) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Rig.Move move : form.moves()) {
            if (!first) out.append(',');
            first = false;
            out.append('"').append(move.id()).append("\":{\"label\":\"").append(move.label()).append("\",\"ticks\":").append(move.ticks());
            out.append(",\"bones\":{");
            boolean firstBone = true;
            for (Map.Entry<Integer, List<Rig.Key>> track : move.bones().entrySet()) {
                if (!firstBone) out.append(',');
                firstBone = false;
                out.append('"').append(track.getKey()).append("\":").append(track(track.getValue()));
            }
            out.append("},\"travel\":").append(track(move.travel()));
            out.append(",\"trails\":").append(ranges(move.trails())).append(",\"slashes\":").append(ranges(move.slashes()))
                    .append(",\"rings\":").append(move.rings());
            out.append(",\"size\":").append(array(reach(form, move))).append('}');
        }
        return out.append('}').toString();
    }

    /** Everything a move sweeps through, with room for the slashes round the body: what the viewer's camera frames. */
    public static float[] reach(RigForm form, Rig.Move move) {
        List<DisplaySculpt.Piece> pieces = form.pieces();
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int tick = 0; tick <= move.ticks(); tick += 2) {
            Matrix4f[] world = form.rig().bones(move, tick);
            List<Transformation> all = form.pose(move, tick, new Matrix4f());
            List<DisplaySculpt.Piece> posed = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i += 3) {
                Transformation t = all.get(i);
                posed.add(new DisplaySculpt.Piece("ROOT", pieces.get(i).material(),
                        t.getTranslation().add(t.getLeftRotation().transform(new Vector3f(t.getScale()).mul(.5f))),
                        t.getLeftRotation(), t.getScale(), false));
            }
            grow(b, bounds(posed));
            for (int[] range : move.slashes()) {
                if (tick >= range[0] && tick <= range[1]) {
                    Vector3f c = world[0].transformPosition(new Vector3f(0, form.middle(), 0));
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
    private static String track(List<Rig.Key> keys) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < keys.size(); i++) {
            Rig.Key k = keys.get(i);
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
            // A skinned piece goes out as the player head the game draws: its item transform and its skin.
            Transformation t = piece.skin() != null ? RigBody.head(piece.transformation()) : piece.transformation();
            Vector3f p = t.getTranslation(), s = t.getScale();
            Quaternionf q = t.getLeftRotation();
            if (i > 0) out.append(',');
            if (piece.skin() != null) out.append("{\"k\":\"h\",\"skin\":\"").append(com.Chagui68.stand.StandViewerExport.skinUrl(piece.skin())).append('"');
            else out.append("{\"k\":\"b\",\"m\":\"").append(piece.material()).append("\",\"g\":").append(piece.glow() ? 1 : 0);
            out.append(String.format(Locale.ROOT, ",\"t\":[%.4f,%.4f,%.4f],\"q\":[%.4f,%.4f,%.4f,%.4f],\"s\":[%.4f,%.4f,%.4f]}",
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
