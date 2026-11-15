package com.Chagui68.entities.boss.rig;

import com.Chagui68.utils.DisplaySculpt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RigFormsTest {

    /** The most displays a new boss may be drawn with: what its pose costs the players' connections. */
    private static final int BUDGET = 200;

    static List<RigForm> forms() {
        return RigForms.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("forms")
    @DisplayName("A form stands on the ground, every piece a real tile, within its budget of displays")
    void modelIsSound(RigForm form) {
        // Malenia is sculpted in fine tiles and Radahn in painted heads on purpose: they are the two that may go over it.
        if (!form.key().startsWith("malenia") && !form.key().equals("radahn")) {
            assertTrue(form.pieces().size() <= BUDGET, form.key() + " is drawn with " + form.pieces().size() + " displays");
        }
        for (DisplaySculpt.Piece p : form.pieces()) {
            assertTrue(p.size().x > 0 && p.size().y > 0 && p.size().z > 0, "a piece of " + p.part() + " has no size");
            assertTrue(p.center().isFinite() && p.rotation().isFinite(), "a piece of " + p.part() + " is not finite");
        }
        float[] b = RigViewerExport.bounds(form.pieces());
        assertEquals(0f, b[1], .06f, form.key() + " stands on the ground");
        assertTrue(b[4] > 2f && b[4] < 4.6f, form.key() + " is " + b[4] + " blocks tall");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("forms")
    @DisplayName("A form's moves keep their keys in order, end in time, and never sink it through the floor")
    void movesAreSound(RigForm form) {
        assertNotNull(form.move("idle"), form.key() + " has no idle move");
        for (Rig.Move move : form.moves()) {
            move.bones().forEach((bone, keys) -> {
                for (int i = 1; i < keys.size(); i++) {
                    assertTrue(keys.get(i).tick() > keys.get(i - 1).tick(), form.key() + " " + move.id() + ": bone " + bone + "'s keys are out of order");
                }
                assertTrue(keys.get(keys.size() - 1).tick() <= move.ticks(), form.key() + " " + move.id() + ": bone " + bone + " keys past the end");
            });
            for (Rig.Cue cue : move.cues()) assertTrue(cue.tick() > 0 && cue.tick() <= move.ticks(), form.key() + " " + move.id() + ": cue " + cue.name());
            float[] reach = RigViewerExport.reach(form, move);
            assertTrue(reach[1] > -.6f, form.key() + " " + move.id() + " goes " + reach[1] + " below the floor");
        }
    }
}
