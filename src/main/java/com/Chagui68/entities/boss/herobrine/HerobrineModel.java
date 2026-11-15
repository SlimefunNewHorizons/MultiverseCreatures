package com.Chagui68.entities.boss.herobrine;

import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.Rig.Ease;
import com.Chagui68.entities.boss.rig.Rig.Move;
import com.Chagui68.entities.boss.rig.RigForm;
import com.Chagui68.stand.HeadModel;
import com.Chagui68.utils.DisplaySculpt;
import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.Color;
import org.bukkit.Material;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.Chagui68.utils.DisplaySculpt.v;

/**
 * Herobrine, drawn with textured player heads: the eleven-head humanoid of the BDEngine export in
 * {@code herobrine/model.txt}, kept exactly as exported (every head's own turn and tilt), centred, its
 * feet put on the origin and scaled up to a boss's height. He carries a rusted
 * pickaxe low in his right hand.
 *
 * <p>He faces {@code +z} with his feet on the origin, his right side on {@code -x}.</p>
 */
public final class HerobrineModel {

    /** The export is a player's height; the boss stands two and a half blocks tall. */
    private static final float SCALE = 1.35f;

    private static final Material WOOD = Material.SPRUCE_PLANKS;
    private static final Material RUST = Material.TERRACOTTA;
    private static final Material IRON = Material.IRON_BLOCK;

    // The export's joints (neck 1.40, shoulder 1.29, elbow 1.17, hip .70, knee .47; arms .35 and legs .12 off centre) at SCALE.
    private static final Vector3f SHOULDER_R = v(-.47, 1.74, 0), ELBOW_R = v(-.47, 1.58, 0), WRIST_R = v(-.47, .97, 0);
    private static final Vector3f SHOULDER_L = v(.47, 1.74, 0), ELBOW_L = v(.47, 1.58, 0), WRIST_L = v(.47, .97, 0);
    private static final Vector3f HIP_R = v(-.16, .95, 0), KNEE_R = v(-.16, .63, 0);
    private static final Vector3f HIP_L = v(.16, .95, 0), KNEE_L = v(.16, .63, 0);
    private static final Vector3f FIST_R = v(-.47, 1.0, .06);
    /** The pickaxe's haft points ahead and low from his fist. */
    private static final Vector3f HAFT = v(0, -.30, .95).normalize();

    public static final Rig RIG = Rig.skeleton(.95f)
            .bone("ROOT", null, v(0, 1.25, 0))
            .bone("PELVIS", "ROOT", v(0, .95, 0))
            .bone("CHEST", "PELVIS", v(0, .95, 0))
            .bone("HEAD", "CHEST", v(0, 1.90, 0))
            .bone("ARM_R", "CHEST", SHOULDER_R).bone("FOREARM_R", "ARM_R", ELBOW_R).bone("HAND_R", "FOREARM_R", WRIST_R)
            .bone("ARM_L", "CHEST", SHOULDER_L).bone("FOREARM_L", "ARM_L", ELBOW_L).bone("HAND_L", "FOREARM_L", WRIST_L)
            .bone("LEG_R", "PELVIS", HIP_R).bone("SHIN_R", "LEG_R", KNEE_R)
            .bone("LEG_L", "PELVIS", HIP_L).bone("SHIN_L", "LEG_L", KNEE_L)
            .build();

    private static final String REST = "ROOT 0 0 0; PELVIS 0 0 0; CHEST 0 0 0; HEAD 0 0 0; ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; FOREARM_L 0 0 0";

    public static final List<Move> MOVES = List.of(
            // He hardly moves at all: only his head, slowly, as if listening.
            RIG.move("idle", "Stare", 120)
                    .key(0, Ease.SMOOTH, "")
                    .key(40, Ease.SMOOTH, "HEAD 0 0 12; CHEST 1 0 0; move 0 .01 0")
                    .key(80, Ease.SMOOTH, "HEAD 0 0 -12; CHEST 0 0 0; move 0 0 0")
                    .key(120, Ease.SMOOTH, "HEAD 0 0 0; CHEST 0 0 0; move 0 0 0")
                    .build(),
            RIG.move("walk", "Walk", 16)
                    .key(0, Ease.SMOOTH, "PELVIS 0 4 0; CHEST 2 -5 0; ARM_L -8 0 2; move 0 0 0")
                    .key(4, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 2 0 0; ARM_L 0 0 2; move 0 .02 0")
                    .key(8, Ease.SMOOTH, "PELVIS 0 -4 0; CHEST 2 5 0; ARM_L 8 0 2; move 0 0 0")
                    .key(12, Ease.SMOOTH, "PELVIS 0 0 0; CHEST 2 0 0; ARM_L 0 0 2; move 0 .02 0")
                    .key(16, Ease.SMOOTH, "PELVIS 0 4 0; CHEST 2 -5 0; ARM_L -8 0 2; move 0 0 0")
                    .stride(16, 20, 30)
                    .build(),
            RIG.move("run", "Rush", 8)
                    .key(0, Ease.SMOOTH, "ROOT 16 0 0; PELVIS 4 8 0; CHEST 8 -12 0; HEAD -18 0 0; ARM_R -15 -20 0; ARM_L -34 0 5; move 0 -.04 0")
                    .key(2, Ease.SMOOTH, "PELVIS 4 0 0; CHEST 8 0 0; ARM_L 0 0 5; move 0 .05 0")
                    .key(4, Ease.SMOOTH, "PELVIS 4 -8 0; CHEST 8 12 0; ARM_L 34 0 5; move 0 -.04 0")
                    .key(6, Ease.SMOOTH, "PELVIS 4 0 0; CHEST 8 0 0; ARM_L 0 0 5; move 0 .05 0")
                    .key(8, Ease.SMOOTH, "PELVIS 4 8 0; CHEST 8 -12 0; ARM_L -34 0 5; move 0 -.04 0")
                    .stride(8, 36, 58)
                    .build(),
            RIG.move("dodge", "Slip back", 12)
                    .key(0, Ease.SMOOTH, "")
                    .key(3, Ease.SMOOTH, "PELVIS 8 0 0; CHEST 10 0 0; move 0 -.15 0")
                    .key(8, Ease.SNAP, "CHEST -6 0 0; move 0 .2 -3.2")
                    .key(12, Ease.SMOOTH, REST + "; move 0 0 -3.4")
                    .aim(0)
                    .build(),
            RIG.move("strike", "Pick strike", 36)
                    .key(0, Ease.SMOOTH, "")
                    .key(10, Ease.SMOOTH, "PELVIS -4 0 0; CHEST -14 -15 0; HEAD 8 10 0; ARM_R -160 0 -8; FOREARM_R -30 0 0; ARM_L -15 0 20; move 0 .05 -.1")
                    .key(15, Ease.SNAP, "PELVIS 14 0 0; CHEST 26 10 0; HEAD -18 0 0; ARM_R -25 0 4; FOREARM_R 0 0 0; ARM_L 10 0 22; move 0 -.25 1.2")
                    .key(24, Ease.SMOOTH, "CHEST 22 8 0; move 0 -.2 1.2")
                    .key(36, Ease.SMOOTH, REST + "; move 0 0 1.2")
                    .trail(10, 16).ring(15)
                    .build(),
            RIG.move("cleave", "Wide cleave", 40)
                    .key(0, Ease.SMOOTH, "")
                    .key(10, Ease.SMOOTH, "PELVIS 4 -20 0; CHEST 6 -50 0; HEAD 0 38 0; ARM_R -60 -75 0; FOREARM_R -15 0 0; ARM_L -8 0 22; move 0 -.1 0")
                    .key(16, Ease.SNAP, "PELVIS 0 20 0; CHEST 6 55 0; HEAD 0 -28 0; ARM_R -66 62 0; FOREARM_R 0 0 0; ARM_L -4 0 32; move 0 0 1")
                    .key(24, Ease.SMOOTH, "CHEST 4 60 0; ARM_R -70 74 0; move 0 0 1.1")
                    .key(40, Ease.SMOOTH, REST + "; move 0 0 1.1")
                    .trail(10, 18)
                    .build(),
            // Gone, then behind his target with the pick already falling.
            RIG.move("blink", "Behind you", 34)
                    .key(0, Ease.SMOOTH, "")
                    .key(7, Ease.SMOOTH, "CHEST -12 -12 0; HEAD 6 8 0; ARM_R -165 0 -8; FOREARM_R -30 0 0; ARM_L -10 0 18; move 0 .05 0")
                    .key(13, Ease.SMOOTH, "CHEST -14 -12 0; move 0 .05 0")
                    .key(18, Ease.SNAP, "PELVIS 14 0 0; CHEST 26 10 0; HEAD -18 0 0; ARM_R -25 0 4; FOREARM_R 0 0 0; ARM_L 10 0 22; move 0 -.25 1")
                    .key(34, Ease.SMOOTH, REST + "; move 0 0 1")
                    .cue(8, "blink").trail(13, 19).aim(0)
                    .build(),
            RIG.move("storm", "Dry lightning", 60)
                    .key(0, Ease.SMOOTH, "")
                    .key(12, Ease.SMOOTH, "CHEST -10 0 0; HEAD 16 0 0; ARM_L -160 0 22; FOREARM_L -10 0 0; move 0 .3 0")
                    .key(44, Ease.SMOOTH, "CHEST -12 0 0; HEAD 20 0 0; ARM_L -170 0 16; move 0 .4 0")
                    .key(60, Ease.SMOOTH, REST + "; move 0 0 0")
                    .cue(16, "bolt").cue(28, "bolt").cue(40, "bolt").aim(42)
                    .build(),
            RIG.move("gloom", "White eyes", 50)
                    .key(0, Ease.SMOOTH, "")
                    .key(14, Ease.SMOOTH, "PELVIS 6 0 0; CHEST 12 0 0; HEAD -14 0 0; ARM_R -10 0 -30; ARM_L -10 0 30; move 0 -.1 0")
                    .key(36, Ease.SMOOTH, "CHEST 14 0 0; HEAD -16 0 0; move 0 -.1 0")
                    .key(50, Ease.SMOOTH, REST + "; move 0 0 0")
                    .cue(14, "gloom").aim(12)
                    .build());

    private static final List<Piece> PIECES = build();

    public static final RigForm FORM = RigForm.of("herobrine", "Herobrine", RIG, PIECES, MOVES, "HAND_R",
            haft(.45f), haft(1.12f), Color.fromRGB(235, 245, 255));

    private HerobrineModel() {
    }

    private static Vector3f haft(float along) {
        return new Vector3f(FIST_R).add(new Vector3f(HAFT).mul(along));
    }

    private static List<Piece> build() {
        List<Piece> pieces = new ArrayList<>(heads());
        DisplaySculpt s = new DisplaySculpt();
        pickaxe(s);
        pieces.addAll(s.pieces());
        return List.copyOf(pieces);
    }

    /**
     * The exported heads, each as the box it fills. A head item is a cube half its scale wide hanging
     * below its anchor, so its box is centred a quarter of its scale under the anchor. The parts are
     * told apart by where they are: the biggest is the head, the ones on the middle the chest, the
     * outer pairs the arms and the inner pairs the legs, the upper of each pair above the joint.
     */
    private static List<Piece> heads() {
        List<HeadModel.Head> heads = HeadModel.read(export());
        List<Matrix4f> matrices = new ArrayList<>();
        float cx = 0, cz = 0, feet = Float.MAX_VALUE;
        for (HeadModel.Head head : heads) {
            Matrix4f m = new Matrix4f().set(head.matrix()).transpose();
            matrices.add(m);
            cx += m.m30() / heads.size();
            cz += m.m32() / heads.size();
            feet = Math.min(feet, m.transformPosition(new Vector3f(0, -.5f, 0)).y);
        }
        int biggest = 0;
        float volume = -1;
        for (int i = 0; i < matrices.size(); i++) {
            Vector3f s = matrices.get(i).getScale(new Vector3f());
            if (s.x * s.y * s.z > volume) {
                volume = s.x * s.y * s.z;
                biggest = i;
            }
        }
        // The export already faces +z: only centre it, put its feet on the origin and scale it.
        Matrix4f place = new Matrix4f().scale(SCALE).translate(-cx, -feet, -cz);
        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < matrices.size(); i++) {
            Matrix4f m = new Matrix4f(place).mul(matrices.get(i));
            Vector3f center = m.transformPosition(new Vector3f(0, -.25f, 0));
            Vector3f size = m.getScale(new Vector3f()).mul(.5f);
            // Unnormalized: the heads are scaled unevenly, and the normalized form would read the scale as turn.
            Quaternionf rotation = m.getUnnormalizedRotation(new Quaternionf());
            pieces.add(new Piece(part(center, i == biggest), Material.PLAYER_HEAD, center, rotation, size, false, heads.get(i).texture()));
        }
        return pieces;
    }

    private static String part(Vector3f c, boolean head) {
        if (head) return "HEAD";
        float x = Math.abs(c.x);
        if (x < .08f) return "CHEST";
        String side = c.x < 0 ? "_R" : "_L";
        if (x > .3f) return (c.y > ELBOW_R.y ? "ARM" : "FOREARM") + side;
        return (c.y > KNEE_R.y ? "LEG" : "SHIN") + side;
    }

    private static String export() {
        try (InputStream in = HerobrineModel.class.getResourceAsStream("/herobrine/model.txt")) {
            if (in == null) throw new IllegalStateException("herobrine/model.txt is missing from the jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A rusted pickaxe: a long haft, an iron head, both points worn brown. */
    private static void pickaxe(DisplaySculpt s) {
        s.part("HAND_R");
        s.bar(WOOD, haft(-.30f), haft(1.05f), .035f, .035f, v(1, 0, 0));
        Vector3f head = haft(1.05f), up = v(0, .95, .30);
        s.box(IRON, head, v(.07, .12, .09), new Quaternionf().rotateX(.3f));
        s.bar(IRON, head, new Vector3f(head).add(new Vector3f(up).mul(.20f)).add(0, 0, .04f), .05f, .05f, v(1, 0, 0));
        s.bar(RUST, new Vector3f(head).add(new Vector3f(up).mul(.20f)).add(0, 0, .04f), new Vector3f(head).add(new Vector3f(up).mul(.36f)).add(0, 0, .12f), .035f, .04f, v(1, 0, 0));
        s.bar(IRON, head, new Vector3f(head).sub(new Vector3f(up).mul(.20f)).add(0, 0, .04f), .05f, .05f, v(1, 0, 0));
        s.bar(RUST, new Vector3f(head).sub(new Vector3f(up).mul(.20f)).add(0, 0, .04f), new Vector3f(head).sub(new Vector3f(up).mul(.36f)).add(0, 0, .12f), .035f, .04f, v(1, 0, 0));
    }
}
