package com.Chagui68.monitor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorServerTest {

    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("A link serves the page and the data; without the token the answer is 404")
    void serves() throws Exception {
        MonitorServer server = new MonitorServer(null, "Test", "1.0", 30);
        int port = server.start(0);
        try {
            UUID admin = UUID.randomUUID();
            String token = server.issue(admin);
            String base = "http://localhost:" + port;

            assertEquals(404, get(base + "/").statusCode());
            assertEquals(404, get(base + "/nope/").statusCode());
            HttpResponse<String> page = get(base + "/" + token + "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("MSC Server Monitor"));
            assertEquals(200, get(base + "/" + token).statusCode());

            String second = server.issue(admin);
            assertEquals(404, get(base + "/" + token + "/").statusCode(), "a new link cancels the old one");
            assertEquals(200, get(base + "/" + second + "/").statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("A busy port moves the server to a free one")
    void busyPort() throws Exception {
        try (java.net.ServerSocket taken = new java.net.ServerSocket(0)) {
            MonitorServer server = new MonitorServer(null, "Test", "1.0", 30);
            int port = server.start(taken.getLocalPort());
            try {
                assertTrue(port != taken.getLocalPort() && port > 0);
                assertEquals(404, get("http://localhost:" + port + "/").statusCode());
            } finally {
                server.stop();
            }
        }
    }
}
