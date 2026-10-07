package com.Chagui68.monitor;

import com.Chagui68.utils.MscLog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/**
 * The hosting-friendly way to open the monitor: the server uploads one snapshot and hands the admin a
 * link, so no port has to be open (the way Spark's viewer works).
 *
 * <p>The snapshot is gzipped and encrypted with AES-256-GCM <em>before</em> it leaves the server. The
 * key travels only in the link's fragment ({@code #...}), which browsers never send anywhere, so the
 * storage service holds bytes it cannot read and the page that shows them is an empty shell without
 * the link. The page decrypts in the browser.
 */
public final class SnapshotUpload {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_BYTES = 12;

    private SnapshotUpload() {
    }

    /** A fresh random AES-256 key. */
    public static byte[] newKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return key;
    }

    /** gzip, then AES-GCM: a 12-byte IV followed by the ciphertext, which is what the page expects. */
    public static byte[] seal(String json, byte[] key) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(zipped)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        }
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] sealed = cipher.doFinal(zipped.toByteArray());
        byte[] out = new byte[IV_BYTES + sealed.length];
        System.arraycopy(iv, 0, out, 0, IV_BYTES);
        System.arraycopy(sealed, 0, out, IV_BYTES, sealed.length);
        return out;
    }

    /** The inverse of {@link #seal}, used by the tests to prove the page's format. */
    static String open(byte[] sealed, byte[] key) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, sealed, 0, IV_BYTES));
        byte[] zipped = cipher.doFinal(sealed, IV_BYTES, sealed.length - IV_BYTES);
        try (java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(
                new java.io.ByteArrayInputStream(zipped))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * The link the admin opens. {@code storage} is added only when it is not the default one, so the
     * usual link stays short.
     */
    public static String link(String viewer, String storage, String defaultStorage, String code, byte[] key) {
        StringBuilder out = new StringBuilder(viewer.endsWith("/") ? viewer : viewer + "/");
        out.append("#c=").append(code).append("&k=").append(Base64.getUrlEncoder().withoutPadding().encodeToString(key));
        if (!trim(storage).equals(trim(defaultStorage))) {
            out.append("&u=").append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(trim(storage).getBytes(StandardCharsets.UTF_8)));
        }
        return out.toString();
    }

    /** Uploads the bytes to a bytebin-compatible service ({@code POST <storage>/post}) and returns its code. */
    public static String upload(String storage, byte[] body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trim(storage) + "/post"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/octet-stream")
                .header("User-Agent", "MultiverseCreatures-monitor")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("the storage service answered " + response.statusCode());
        }
        return codeOf(response.body(), response.headers().firstValue("Location").orElse(null));
    }

    /** The code is in the JSON body ({@code {"key":"..."}}) or, failing that, the last part of Location. */
    static String codeOf(String body, String location) throws IOException {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("key")) return json.get("key").getAsString();
        } catch (RuntimeException e) {
            MscLog.debug("the storage service did not answer with JSON; reading its Location header", e);
        }
        if (location != null && !location.isBlank()) {
            String[] parts = location.split("/");
            return parts[parts.length - 1];
        }
        throw new IOException("the storage service gave no code");
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
