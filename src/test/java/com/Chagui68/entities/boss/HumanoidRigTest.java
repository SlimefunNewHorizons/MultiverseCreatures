package com.Chagui68.entities.boss;

import com.Chagui68.testsupport.LimbGeometry;
import com.Chagui68.testsupport.ProjectPaths;
import com.Chagui68.utils.MscLimb;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What every dressed humanoid boss promises, checked once for all of them: NIX and Jack Star are two
 * different sets of display pieces on the same skeleton (two arms and two legs that each fold at one
 * joint, hanging from a head and a torso on the stand's axis), so the rules that keep that skeleton
 * together live here and each model's own test keeps only what is particular to it.
 */
class HumanoidRigTest {

    /** One model seen through the few calls the contract needs. */
    interface Rig<P, G> {
        List<P> parts();
        G group(P part);
        Vector3f base(P part);
        Vector3f scale(P part);
        Vector3f pivot(G group);
        Vector3f secondJoint(G group);
        boolean hangsFromSecondJoint(P part);
        Quaternionf lowerRotation(P part, float phase);
        float walkSwing(G group, float phase);
        Vector3f moved(P part, Quaternionf upper, Quaternionf lower);
        List<MscLimb.Limb> walkPose(float phase);
        double hitboxScale();
        List<G> arms();
        List<G> legs();
        List<G> rigid();
        /** Right/left pairs that must mirror each other. */
        List<List<P>> mirrored();
        /** Whether the arms are inside the hitbox too, or sit outside it by design. */
        boolean armsInHitbox();
        String bossSource();
        String walkRate();
    }

    static Stream<Rig<?, ?>> rigs() {
        return Stream.of(nix(), jack());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rigs")
    @DisplayName("The rest pose is one skeleton: mirrored, every piece in its place, joints where the export splits each limb")
    <P, G> void theRestPoseIsASoundSkeleton(Rig<P, G> rig) {
        for (List<P> pair : rig.mirrored()) {
            Vector3f r = rig.base(pair.get(0));
            Vector3f l = rig.base(pair.get(1));
            assertEquals(-r.x, l.x, 0.02f, pair + " are not mirrored");
            assertEquals(r.y, l.y, 0.02f, pair + " are at different heights");
            assertEquals(r.z, l.z, 0.02f, pair + " are at different depths");
        }

        List<P> parts = rig.parts();
        for (int i = 0; i < parts.size(); i++) {
            for (int j = i + 1; j < parts.size(); j++) {
                assertTrue(rig.base(parts.get(i)).distance(rig.base(parts.get(j))) > 0.02f,
                        parts.get(i) + " and " + parts.get(j) + " sit on top of each other");
            }
        }

        Quaternionf swing = new Quaternionf().rotateX(0.32f);
        for (P part : parts) {
            Vector3f base = rig.base(part);
            if (Math.abs(base.x) > 0.05f) { // head and torso hang from the spine, not a side
                float jointX = rig.pivot(rig.group(part)).x;
                assertEquals(Math.signum(base.x), Math.signum(jointX),
                        part + " swings around a joint on the wrong side (part x=" + base.x + ", joint x=" + jointX + ")");
                assertEquals(base.x, jointX, 0.01f, part + " hangs off its joint instead of from it");
            }
            Vector3f moved = rig.moved(part, new Quaternionf(swing), new Quaternionf());
            assertEquals(base.x, moved.x, 1.0e-4f, part + " slid sideways while swinging");
            assertTrue(base.distance(moved) < 0.35f, part + " flew away from its joint: " + base.distance(moved));
        }

        for (G group : limbs(rig)) {
            List<P> limb = parts.stream().filter(p -> rig.group(p) == group).toList();
            LimbGeometry.Split<P> split = LimbGeometry.largestGap(limb, p -> rig.base(p).y);
            assertEquals(1, split.upper().size(), group + " should have a single piece at the joint");
            for (P part : limb) {
                assertEquals(split.lower().contains(part), rig.hangsFromSecondJoint(part),
                        part + " is on the wrong side of its joint, so the walk would fold the wrong piece");
            }
            Vector3f joint = rig.secondJoint(group);
            assertNotNull(joint, group + " must expose the joint its lower segment folds about");
            assertEquals(split.joint(), joint.y, 0.01, group + "'s joint is not where the export splits the limb");
            assertEquals(rig.pivot(group).x, joint.x, 0.01f, "a joint must sit on its limb's own axis");
        }
        for (G group : rig.rigid()) {
            assertNull(rig.secondJoint(group), group + " has no second segment");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rigs")
    @DisplayName("The walk folds only the lower half of each limb, keeps the skeleton rigid and stays over the hitbox")
    <P, G> void theWalkKeepsTheSkeletonTogether(Rig<P, G> rig) {
        float halfWidth = 0.25f * (float) rig.hitboxScale();
        float height = 1.975f * (float) rig.hitboxScale();

        for (float phase = 0f; phase < (float) (2 * Math.PI); phase += 0.1f) {
            for (P part : rig.parts()) {
                G group = rig.group(part);
                Quaternionf folded = rig.lowerRotation(part, phase);
                if (!rig.hangsFromSecondJoint(part)) {
                    assertEquals(new Quaternionf(), folded, part + " is above the joint and must stay rigid");
                } else {
                    float swing = rig.walkSwing(group, phase);
                    Quaternionf expected = rig.arms().contains(group) ? MscLimb.elbow(swing) : MscLimb.knee(swing);
                    assertEquals(expected, folded, part + " does not follow its own limb's swing at phase " + phase);
                }
                if (!covered(rig, group)) continue;
                Vector3f moved = rig.moved(part, new Quaternionf().rotateX(rig.walkSwing(group, phase)), folded);
                float margin = rig.armsInHitbox() ? rig.scale(part).x * 0.25f : 0f;
                assertTrue(Math.abs(moved.x) + margin < halfWidth, part + " swung out of the hitbox sideways at phase " + phase);
                assertTrue(Math.abs(moved.z) + margin < halfWidth, part + " swung out of the hitbox front or back at phase " + phase);
            }
        }

        List<MscLimb.Limb> rest = rig.walkPose(0f);
        assertEquals(4, rest.size(), "the four limbs are the whole walk skeleton");
        for (G group : limbs(rig)) {
            MscLimb.Limb limb = limbAt(rest, rig.pivot(group));
            assertNotNull(limb, group + " is missing from the walk skeleton");
            assertEquals(0f, limb.joint().distance(rig.secondJoint(group)), 1.0e-5f,
                    group + " must fold exactly at the joint the display pieces fold at");
        }
        for (float phase = 0f; phase < (float) (2 * Math.PI); phase += 0.1f) {
            List<MscLimb.Limb> posed = rig.walkPose(phase);
            assertEquals(rest.size(), posed.size());
            for (int index = 0; index < posed.size(); index++) {
                MscLimb.Limb limb = posed.get(index);
                MscLimb.Limb idle = rest.get(index);
                assertEquals(0f, limb.pivot().distance(idle.pivot()), 1.0e-5f, "a limb's pivot moved at phase " + phase);
                assertEquals(idle.pivot().distance(idle.joint()), limb.pivot().distance(limb.joint()), 1.0e-4f,
                        "the upper segment changed length at phase " + phase);
                assertEquals(idle.joint().distance(idle.tip()), limb.joint().distance(limb.tip()), 1.0e-4f,
                        "the lower segment came away from its joint at phase " + phase);
                if (!rig.armsInHitbox() && !isLeg(rig, limb)) continue;
                for (Vector3f point : List.of(limb.pivot(), limb.joint(), limb.tip())) {
                    assertTrue(Math.abs(point.x) < halfWidth, "the walk left the hitbox sideways at phase " + phase);
                    assertTrue(Math.abs(point.z) < halfWidth, "the walk left the hitbox front or back at phase " + phase);
                    assertTrue(point.y > 0f && point.y < height, "the walk left the hitbox vertically at phase " + phase);
                }
            }
        }

        // The knee folds the foot behind on the back half of the step, the elbow the hand in front on
        // the forward half, each bringing the end closer to the joint it hangs from.
        G leg = rig.legs().get(0);
        float back = (float) (-Math.PI / 2);
        MscLimb.Limb bentLeg = limbAt(rig.walkPose(back), rig.pivot(leg));
        MscLimb.Limb straightLeg = limbAt(rest, rig.pivot(leg));
        Vector3f straightFoot = MscLimb.swing(straightLeg.tip(), straightLeg.pivot(),
                new Quaternionf().rotateX(rig.walkSwing(leg, back)));
        assertTrue(bentLeg.tip().z > straightFoot.z, "the knee must fold the foot behind the straight leg");
        assertTrue(bentLeg.tip().distance(bentLeg.pivot()) < straightFoot.distance(straightLeg.pivot()),
                "the fold must bring the foot closer to the hip, or the knee never bent");

        G arm = rig.arms().get(1);
        float forward = (float) (Math.PI / 2);
        MscLimb.Limb bentArm = limbAt(rig.walkPose(forward), rig.pivot(arm));
        MscLimb.Limb straightArm = limbAt(rest, rig.pivot(arm));
        Vector3f straightHand = MscLimb.swing(straightArm.tip(), straightArm.pivot(),
                new Quaternionf().rotateX(rig.walkSwing(arm, forward)));
        assertTrue(bentArm.tip().z < straightHand.z, "the elbow must fold the hand in front of the straight arm");
        assertTrue(bentArm.tip().distance(bentArm.pivot()) < straightHand.distance(straightArm.pivot()),
                "the fold must bring the hand closer to the shoulder, or the elbow never bent");

        String source = ProjectPaths.read(ProjectPaths.source("com", "Chagui68", "entities", "boss", rig.bossSource()));
        assertTrue(source.contains(rig.walkRate()), "the boss's own step and the walk replay must advance at the same rate");
    }

    // ------------------------------------------------------------------ the two rigs

    static Rig<NixBoss.NixPart, NixBoss.LimbGroup> nix() {
        return new Rig<>() {
            public List<NixBoss.NixPart> parts() { return List.of(NixBoss.NixPart.values()); }
            public NixBoss.LimbGroup group(NixBoss.NixPart p) { return p.group; }
            public Vector3f base(NixBoss.NixPart p) { return NixModel.baseTranslation(p); }
            public Vector3f scale(NixBoss.NixPart p) { return p.scale; }
            public Vector3f pivot(NixBoss.LimbGroup g) { return NixModel.pivot(g); }
            public Vector3f secondJoint(NixBoss.LimbGroup g) { return NixModel.secondJoint(g); }
            public boolean hangsFromSecondJoint(NixBoss.NixPart p) { return NixModel.hangsFromSecondJoint(p); }
            public Quaternionf lowerRotation(NixBoss.NixPart p, float phase) { return NixModel.lowerRotation(p, phase); }
            public float walkSwing(NixBoss.LimbGroup g, float phase) { return NixModel.walkSwing(g, phase); }
            public Vector3f moved(NixBoss.NixPart p, Quaternionf upper, Quaternionf lower) {
                return NixModel.compose(p, upper, lower).getTranslation();
            }
            public List<MscLimb.Limb> walkPose(float phase) { return NixModel.walkPose(phase); }
            public double hitboxScale() { return NixBoss.MODEL_HITBOX_SCALE; }
            public List<NixBoss.LimbGroup> arms() { return List.of(NixBoss.LimbGroup.ARM_RIGHT, NixBoss.LimbGroup.ARM_LEFT); }
            public List<NixBoss.LimbGroup> legs() { return List.of(NixBoss.LimbGroup.LEG_RIGHT, NixBoss.LimbGroup.LEG_LEFT); }
            public List<NixBoss.LimbGroup> rigid() {
                return List.of(NixBoss.LimbGroup.HEAD, NixBoss.LimbGroup.TORSO_UPPER, NixBoss.LimbGroup.TORSO_LOWER);
            }
            public List<List<NixBoss.NixPart>> mirrored() {
                List<List<NixBoss.NixPart>> pairs = new ArrayList<>();
                for (int segment = 1; segment <= 6; segment++) {
                    pairs.add(List.of(NixBoss.NixPart.valueOf("LEG_R_" + segment), NixBoss.NixPart.valueOf("LEG_L_" + segment)));
                    pairs.add(List.of(NixBoss.NixPart.valueOf("ARM_R_" + segment), NixBoss.NixPart.valueOf("ARM_L_" + segment)));
                }
                return pairs;
            }
            public boolean armsInHitbox() { return true; }
            public String bossSource() { return "NixBoss.java"; }
            public String walkRate() { return "NixModel.WALK_RATE"; }
            @Override public String toString() { return "NIX"; }
        };
    }

    static Rig<JackStarBoss.JackPart, JackStarBoss.LimbGroup> jack() {
        return new Rig<>() {
            public List<JackStarBoss.JackPart> parts() { return List.of(JackStarBoss.JackPart.values()); }
            public JackStarBoss.LimbGroup group(JackStarBoss.JackPart p) { return p.group; }
            public Vector3f base(JackStarBoss.JackPart p) { return JackModel.baseTranslation(p); }
            public Vector3f scale(JackStarBoss.JackPart p) { return p.scale; }
            public Vector3f pivot(JackStarBoss.LimbGroup g) { return JackModel.pivot(g); }
            public Vector3f secondJoint(JackStarBoss.LimbGroup g) { return JackModel.secondJoint(g); }
            public boolean hangsFromSecondJoint(JackStarBoss.JackPart p) { return JackModel.hangsFromSecondJoint(p); }
            public Quaternionf lowerRotation(JackStarBoss.JackPart p, float phase) { return JackModel.lowerRotation(p, phase); }
            public float walkSwing(JackStarBoss.LimbGroup g, float phase) { return JackModel.walkSwing(g, phase); }
            public Vector3f moved(JackStarBoss.JackPart p, Quaternionf upper, Quaternionf lower) {
                return JackModel.compose(p, upper, lower, 1.0f).getTranslation();
            }
            public List<MscLimb.Limb> walkPose(float phase) { return JackModel.walkPose(phase); }
            public double hitboxScale() { return JackStarBoss.MODEL_HITBOX_SCALE; }
            public List<JackStarBoss.LimbGroup> arms() {
                return List.of(JackStarBoss.LimbGroup.ARM_RIGHT, JackStarBoss.LimbGroup.ARM_LEFT);
            }
            public List<JackStarBoss.LimbGroup> legs() {
                return List.of(JackStarBoss.LimbGroup.LEG_RIGHT, JackStarBoss.LimbGroup.LEG_LEFT);
            }
            public List<JackStarBoss.LimbGroup> rigid() {
                return List.of(JackStarBoss.LimbGroup.HEAD, JackStarBoss.LimbGroup.TORSO_UPPER,
                        JackStarBoss.LimbGroup.TORSO_LOWER);
            }
            public List<List<JackStarBoss.JackPart>> mirrored() {
                return List.of(
                        List.of(JackStarBoss.JackPart.LEG_R_UPPER, JackStarBoss.JackPart.LEG_L_UPPER),
                        List.of(JackStarBoss.JackPart.LEG_R_LOWER, JackStarBoss.JackPart.LEG_L_LOWER),
                        List.of(JackStarBoss.JackPart.ARM_R_UPPER, JackStarBoss.JackPart.ARM_L_UPPER),
                        List.of(JackStarBoss.JackPart.ARM_R_LOWER, JackStarBoss.JackPart.ARM_L_LOWER));
            }
            // Jack's arms sit outside the stand's box by design (JackModelTest's hitbox check says why).
            public boolean armsInHitbox() { return false; }
            public String bossSource() { return "JackStarBoss.java"; }
            public String walkRate() { return "JackModel.WALK_RATE"; }
            @Override public String toString() { return "Jack Star"; }
        };
    }

    // ------------------------------------------------------------------ helpers

    private static <P, G> List<G> limbs(Rig<P, G> rig) {
        List<G> limbs = new ArrayList<>(rig.legs());
        limbs.addAll(rig.arms());
        return limbs;
    }

    private static <P, G> boolean covered(Rig<P, G> rig, G group) {
        return rig.armsInHitbox() || !rig.arms().contains(group);
    }

    private static <P, G> boolean isLeg(Rig<P, G> rig, MscLimb.Limb limb) {
        for (G leg : rig.legs()) {
            if (limb.pivot().distance(rig.pivot(leg)) < 1.0e-5f) return true;
        }
        return false;
    }

    private static MscLimb.Limb limbAt(List<MscLimb.Limb> limbs, Vector3f pivot) {
        for (MscLimb.Limb limb : limbs) {
            if (limb.pivot().distance(pivot) < 1.0e-5f) return limb;
        }
        return null;
    }
}
