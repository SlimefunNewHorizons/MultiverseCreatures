package com.Chagui68.entities.boss;

import com.Chagui68.testsupport.LimbGeometry;
import com.Chagui68.testsupport.ProjectPaths;
import com.Chagui68.utils.MscLimb;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What is particular to Jack Star's model: eleven parts matching the in-game reference, the body
 * centred on the invisible armour stand that receives the hits, the slash, the shape shift and the
 * hitbox. The skeleton rules it shares with NIX (mirrored limbs, joints, walk) are in
 * {@link HumanoidRigTest}.
 *
 * <p>The reference numbers are the passenger transforms of the model as built in game
 * ({@code /summon block_display ... {Passengers:[item_display x11]}}). The plugin keeps the same
 * shape with one difference: the reference data carries a global X offset for the whole body, and
 * the code re-centres it so the visible body sits over the hitbox. These tests pin both halves of
 * that contract.
 */
class JackModelTest {

    /**
     * Reference model, straight from the in-game passengers: one entry per part, in the order the
     * game wrote them. Only the translation matters here; the full matrices live in JackPart.
     */
    private static final Map<JackStarBoss.JackPart, Vector3f> REFERENCE = new EnumMap<>(JackStarBoss.JackPart.class);

    static {
        REFERENCE.put(JackStarBoss.JackPart.HEAD, new Vector3f(-0.9301796875f, 1.872775625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.TORSO_UPPER, new Vector3f(-0.9301796875f, 1.404275625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.TORSO_LOWER, new Vector3f(-0.9301796875f, 1.170025625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.LEG_R_UPPER, new Vector3f(-1.0473046875f, 0.701525625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.LEG_R_LOWER, new Vector3f(-1.0473046875f, 0.467275625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.LEG_L_UPPER, new Vector3f(-0.8130546875f, 0.701525625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.LEG_L_LOWER, new Vector3f(-0.8130546875f, 0.467275625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.ARM_R_UPPER, new Vector3f(-0.5823184375f, 1.404275625f, -0.0165821875f));
        REFERENCE.put(JackStarBoss.JackPart.ARM_R_LOWER, new Vector3f(-0.586125f, 1.170025625f, -0.016875f));
        REFERENCE.put(JackStarBoss.JackPart.ARM_L_UPPER, new Vector3f(-1.2815546875f, 1.404275625f, -0.016851575f));
        REFERENCE.put(JackStarBoss.JackPart.ARM_L_LOWER, new Vector3f(-1.2815546875f, 1.170025625f, -0.016851575f));
    }

    @Test
    @DisplayName("Every part matches the in-game reference model up to one shared X offset, re-centred over the hitbox")
    void partsMatchTheReferenceModel() {
        assertEquals(JackStarBoss.JackPart.values().length, REFERENCE.size(),
                "the reference table must cover every part");

        Float sharedShift = null;
        for (Map.Entry<JackStarBoss.JackPart, Vector3f> entry : REFERENCE.entrySet()) {
            JackStarBoss.JackPart part = entry.getKey();
            Vector3f reference = entry.getValue();

            assertEquals(reference.y, part.offset.y, 0.001f, part + " drifted vertically from the model");
            assertEquals(reference.z, part.offset.z, 0.001f, part + " drifted in depth from the model");

            float shift = part.offset.x - reference.x;
            if (sharedShift == null) {
                sharedShift = shift;
                assertTrue(Math.abs(shift) > 0.9f, "the reference body is offset as a whole: " + shift);
            }
            assertEquals(sharedShift, shift, 0.005f,
                    part + " is not on the model's single shared X axis");
        }

        assertEquals(0.0f, JackStarBoss.JackPart.CENTER.x, 0.005f,
                "the body axis must sit where the invisible armour stand is");
        assertEquals(0.0f, JackModel.baseTranslation(JackStarBoss.JackPart.HEAD).x, 0.01f);
        assertEquals(0.0f, JackModel.baseTranslation(JackStarBoss.JackPart.TORSO_UPPER).x, 0.01f);
    }


    @Test
    @DisplayName("Head sits above the torso, the torso above the legs, and the body is about two blocks tall")
    void verticalLayoutIsAHumanoid() {
        assertTrue(baseY(JackStarBoss.JackPart.HEAD) > baseY(JackStarBoss.JackPart.TORSO_UPPER) + 0.3f);
        assertTrue(baseY(JackStarBoss.JackPart.TORSO_UPPER) > baseY(JackStarBoss.JackPart.TORSO_LOWER) + 0.2f);
        assertTrue(baseY(JackStarBoss.JackPart.TORSO_LOWER) > baseY(JackStarBoss.JackPart.LEG_R_UPPER) + 0.4f);
        assertTrue(baseY(JackStarBoss.JackPart.LEG_R_UPPER) > baseY(JackStarBoss.JackPart.LEG_R_LOWER) + 0.2f);

        float headTop = baseY(JackStarBoss.JackPart.HEAD) + JackStarBoss.JackPart.HEAD.scale.y * 0.25f;
        float feetBottom = baseY(JackStarBoss.JackPart.LEG_L_LOWER)
                - JackStarBoss.JackPart.LEG_L_LOWER.scale.y * 0.25f;
        assertTrue(headTop > 2.0f && headTop < 2.3f, "head top should be around two blocks: " + headTop);
        assertTrue(feetBottom > 0.1f && feetBottom < 0.4f, "feet should clear the ground: " + feetBottom);
    }






    @Test
    @DisplayName("During slash, elbows bend in the sword arc and knees flex in stance")
    void slashFoldsElbowsAndKnees() {
        for (float prog = 0f; prog <= 1f; prog += 0.05f) {
            for (JackStarBoss.JackPart part : JackStarBoss.JackPart.values()) {
                Quaternionf fold = JackModel.slashLowerRotation(part, prog);
                if (!JackModel.hangsFromSecondJoint(part)) {
                    assertEquals(new Quaternionf(), fold, part + " is above joint and must not fold");
                    continue;
                }
                assertNotNull(fold);
            }
        }
        // At mid slash (prog = 0.5), elbows bend dynamically
        for (JackStarBoss.JackPart armPart : List.of(JackStarBoss.JackPart.ARM_R_LOWER, JackStarBoss.JackPart.ARM_L_LOWER)) {
            Quaternionf elbowRot = JackModel.slashLowerRotation(armPart, 0.5f);
            assertNotEquals(new Quaternionf(), elbowRot, "elbows must fold during slash arc");
        }
        // At mid slash (prog = 0.5), knees flex dynamically
        for (JackStarBoss.JackPart legPart : List.of(JackStarBoss.JackPart.LEG_R_LOWER, JackStarBoss.JackPart.LEG_L_LOWER)) {
            Quaternionf kneeRot = JackModel.slashLowerRotation(legPart, 0.5f);
            assertNotEquals(new Quaternionf(), kneeRot, "knees must flex during slash stance");
        }
    }



    @Test
    @DisplayName("Shape shifting scales the whole body and the stand stays inside the hitbox")
    void shapeShiftScalesTheBody() {
        JackStarBoss.JackPart part = JackStarBoss.JackPart.TORSO_UPPER;
        Vector3f rest = JackModel.baseTranslation(part);

        Transformation big = JackModel.compose(part, new Quaternionf(), 2.2f);
        assertEquals(rest.x * 2.2f, big.getTranslation().x, 1.0e-3f);
        assertEquals(rest.y * 2.2f, big.getTranslation().y, 1.0e-3f);
        assertEquals(part.scale.y * 2.2f, big.getScale().y, 1.0e-3f);

        for (JackStarBoss.JackPart p : JackStarBoss.JackPart.values()) {
            Vector3f base = JackModel.baseTranslation(p);
            assertTrue(base.y > 0.2f && base.y < 2.2f, p + " sits outside the stand's height: " + base.y);
        }
    }

    @Test
    @DisplayName("The invisible stand's hitbox covers the visible body")
    void hitboxCoversTheBody() {
        // The stand is the only hitbox the model has: the displays must stay un-hittable, so the
        // swing has to land on the stand for the boss to take damage at all.
        float halfWidth = 0.25f * (float) JackStarBoss.MODEL_HITBOX_SCALE;
        float height = 1.975f * (float) JackStarBoss.MODEL_HITBOX_SCALE;

        List<JackStarBoss.JackPart> spine = List.of(
                JackStarBoss.JackPart.HEAD,
                JackStarBoss.JackPart.TORSO_UPPER,
                JackStarBoss.JackPart.TORSO_LOWER,
                JackStarBoss.JackPart.LEG_R_UPPER,
                JackStarBoss.JackPart.LEG_R_LOWER,
                JackStarBoss.JackPart.LEG_L_UPPER,
                JackStarBoss.JackPart.LEG_L_LOWER);
        for (JackStarBoss.JackPart part : spine) {
            Vector3f base = JackModel.baseTranslation(part);
            assertTrue(Math.abs(base.x) + part.scale.x * 0.25f < halfWidth,
                    part + " leans outside the hitbox, so a swing at it would miss the stand");
            assertTrue(base.y < height, part + " sits above the hitbox: " + base.y);
        }

        float headTop = baseY(JackStarBoss.JackPart.HEAD) + JackStarBoss.JackPart.HEAD.scale.y * 0.25f;
        assertTrue(headTop < height, "the head top must be inside the scaled hitbox: " + headTop);
        assertTrue(headTop > 1.975f,
                "the scaled stand is pointless unless a vanilla box would have missed the head: " + headTop);
    }




    private static float baseY(JackStarBoss.JackPart part) {
        return JackModel.baseTranslation(part).y;
    }


}
