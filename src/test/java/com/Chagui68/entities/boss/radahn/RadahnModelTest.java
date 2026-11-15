package com.Chagui68.entities.boss.radahn;

import com.Chagui68.stand.StandViewerExport;
import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.HeadSculpt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RadahnModelTest {

    @Test
    @DisplayName("Every head he is drawn with wears a skin that was uploaded, which Mojang hosts")
    void everySkinIsUploaded() {
        List<Piece> pieces = RadahnModel.FORM.pieces();
        long local = pieces.stream().filter(p -> p.skin() == null || p.skin().startsWith(HeadSculpt.LOCAL)).count();
        assertEquals(0, local, local + " of his " + pieces.size()
                + " heads wear a skin nobody uploaded: his art changed, so run website/models/stand-skins/heads.py");
        for (Piece piece : pieces) {
            assertTrue(StandViewerExport.skinUrl(piece.skin()).startsWith("https://textures.minecraft.net/texture/"),
                    "a head of " + piece.part() + " has a texture that is not Mojang's");
        }
    }

    @Test
    @DisplayName("He is drawn with half the displays the block model took, and no more skins than a client fetches in a moment")
    void staysWithinHisBudget() {
        assertTrue(RadahnModel.FORM.pieces().size() <= 460, "he is drawn with " + RadahnModel.FORM.pieces().size() + " heads");
        assertTrue(RadahnModel.skins().size() <= 170, "his heads wear " + RadahnModel.skins().size() + " different skins");
    }
}
