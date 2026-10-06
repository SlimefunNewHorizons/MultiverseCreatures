package com.Chagui68.stand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the model viewer's data in step with the models: every build writes it again. */
class StandViewerExportTest {

    @Test
    @DisplayName("Every Stand with a body is written for the model viewer")
    void writesTheViewerData() throws Exception {
        Path out = StandViewerExport.write(Path.of("target", "stand-viewer", "models.json"));
        String json = Files.readString(out);
        for (StandType type : StandType.values()) {
            if (type.hasBody()) {
                assertTrue(json.contains("\"key\":\"" + type.key() + "\""), type.key());
            }
        }
        assertTrue(json.contains("\"kind\":\"heads\""), "The World is drawn with heads");
        for (com.Chagui68.entities.boss.witherstorm.WitherStormForm form
                : com.Chagui68.entities.boss.witherstorm.WitherStormForm.values()) {
            assertTrue(json.contains("\"key\":\"wither-storm-" + form.key() + "\""), "Wither Storm " + form.key());
        }
        assertTrue(json.contains("\"kind\":\"blocks\""), "the Wither Storm is drawn with block displays");
    }
}
