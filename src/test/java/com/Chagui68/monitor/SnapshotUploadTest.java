package com.Chagui68.monitor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotUploadTest {

    @Test
    @DisplayName("A sealed snapshot opens with its key, hides its content and rejects another key")
    void roundTrip() throws Exception {
        byte[] key = SnapshotUpload.newKey();
        String json = "{\"server\":\"Purpur\",\"tps\":19.9}";
        byte[] sealed = SnapshotUpload.seal(json, key);
        assertFalse(new String(sealed, StandardCharsets.ISO_8859_1).contains("Purpur"));
        assertEquals(json, SnapshotUpload.open(sealed, key));
        assertThrows(Exception.class, () -> SnapshotUpload.open(sealed, SnapshotUpload.newKey()));
    }

    @Test
    @DisplayName("Two seals of the same text differ (a fresh IV each time)")
    void freshIv() throws Exception {
        byte[] key = SnapshotUpload.newKey();
        assertFalse(Arrays.equals(SnapshotUpload.seal("x", key), SnapshotUpload.seal("x", key)));
    }

    @Test
    @DisplayName("The link keeps the key in the fragment and names a custom storage only when it differs")
    void link() {
        byte[] key = new byte[32];
        String plain = SnapshotUpload.link("https://a.io/monitor", "https://b.io/", "https://b.io", "abc", key);
        assertTrue(plain.startsWith("https://a.io/monitor/#c=abc&k="));
        assertFalse(plain.contains("&u="));
        assertTrue(SnapshotUpload.link("https://a.io/monitor/", "https://other.io", "https://b.io", "abc", key)
                .contains("&u="));
    }

    @Test
    @DisplayName("The code comes from the JSON body or the Location header")
    void code() throws IOException {
        assertEquals("k1", SnapshotUpload.codeOf("{\"key\":\"k1\"}", null));
        assertEquals("k2", SnapshotUpload.codeOf("", "/k2"));
        assertThrows(IOException.class, () -> SnapshotUpload.codeOf("", null));
    }
}
