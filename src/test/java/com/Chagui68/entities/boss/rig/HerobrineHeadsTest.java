package com.Chagui68.entities.boss.rig;

import com.Chagui68.entities.boss.herobrine.HerobrineModel;
import com.Chagui68.utils.DisplaySculpt.Piece;
import org.bukkit.util.Transformation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HerobrineHeadsTest {

    private static List<Piece> heads() {
        return HerobrineModel.FORM.pieces().stream().filter(p -> p.skin() != null).toList();
    }

    @Test
    @DisplayName("Herobrine's eleven heads land on a head, a chest of two and two pieces per limb")
    void everyHeadHasItsPart() {
        Map<String, Long> parts = heads().stream().collect(Collectors.groupingBy(Piece::part, Collectors.counting()));
        assertEquals(11, heads().size());
        assertEquals(Map.of("HEAD", 1L, "CHEST", 2L, "ARM_R", 1L, "FOREARM_R", 1L, "ARM_L", 1L, "FOREARM_L", 1L,
                "LEG_R", 1L, "SHIN_R", 1L, "LEG_L", 1L, "SHIN_L", 1L), parts);
    }

    @Test
    @DisplayName("A head is drawn from the top middle of its box at twice the box's size")
    void headFillsItsBox() {
        for (Piece piece : heads()) {
            assertEquals(1f, piece.rotation().lengthSquared(), 1e-4f, piece.part() + "'s turn is not a pure rotation");
            Transformation item = RigBody.head(piece.transformation());
            assertTrue(item.getScale().equals(new Vector3f(piece.size()).mul(2f), 1e-5f));
            Vector3f top = piece.rotation().transform(new Vector3f(0, piece.size().y * .5f, 0)).add(piece.center());
            assertTrue(item.getTranslation().equals(top, 1e-5f), piece.part() + " is anchored off its box");
        }
        Piece head = heads().stream().filter(p -> p.part().equals("HEAD")).findFirst().orElseThrow();
        for (Piece piece : heads()) assertTrue(piece.center().y <= head.center().y, piece.part() + " is above the head");
    }
}
