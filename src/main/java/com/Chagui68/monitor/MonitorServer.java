package com.Chagui68.monitor;

import com.Chagui68.utils.MscLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * The private web page behind {@code /msc tps}.
 *
 * <p>The page lives in the plugin's jar and is only ever served by this server, to a link that
 * {@code /msc tps} hands to an admin: every link carries a random token that expires and is revoked
 * by the next run of the command. The page is not in the repository's {@code docs/} folder, so it is
 * never part of the GitHub Pages site. Without a valid token the server answers 404 and nothing else.
 */
public final class MonitorServer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private record Link(UUID owner, long expires) {
    }

    private final ServerMonitor monitor;
    private final String serverName;
    private final String version;
    private final long linkMillis;
    private final Map<String, Link> links = new HashMap<>();

    private HttpServer http;
    private String page;

    public MonitorServer(ServerMonitor monitor, String serverName, String version, int linkMinutes) {
        this.monitor = monitor;
        this.serverName = serverName;
        this.version = version;
        this.linkMillis = Math.max(1, linkMinutes) * 60_000L;
    }

    /** How many ports above the wanted one are tried before asking the system for any free one. */
    static final int PORT_TRIES = 20;

    /**
     * Opens the port on first use and returns the one actually bound. A port that is taken (a hosting
     * panel gives the plugin one port and often something else already holds it) moves on to the next
     * ones, and finally to any free port the system picks.
     */
    public synchronized int start(int port) throws IOException {
        if (http != null) return http.getAddress().getPort();
        try (InputStream in = MonitorServer.class.getResourceAsStream("/monitor/index.html")) {
            if (in == null) throw new IOException("monitor/index.html is missing from the jar");
            page = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        http = bind(port);
        http.createContext("/", this::handle);
        http.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "MSC-Monitor-Http");
            t.setDaemon(true);
            return t;
        }));
        http.start();
        return http.getAddress().getPort();
    }

    private static HttpServer bind(int port) throws IOException {
        IOException last = null;
        for (int i = 0; i <= PORT_TRIES && port > 0 && port + i <= 65535; i++) {
            try {
                return HttpServer.create(new InetSocketAddress(port + i), 0);
            } catch (java.net.BindException e) {
                last = e;
            }
        }
        try {
            return HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            if (last != null) e.addSuppressed(last);
            throw e;
        }
    }

    public synchronized void stop() {
        if (http != null) http.stop(0);
        http = null;
        links.clear();
    }

    public synchronized boolean isRunning() {
        return http != null;
    }

    /** A fresh token for {@code owner}; the previous one of that admin stops working. */
    public synchronized String issue(UUID owner) {
        links.values().removeIf(l -> l.owner().equals(owner) || l.expires() < System.currentTimeMillis());
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        links.put(token, new Link(owner, System.currentTimeMillis() + linkMillis));
        return token;
    }

    private synchronized boolean valid(String token) {
        Link link = links.get(token);
        if (link == null) return false;
        if (link.expires() < System.currentTimeMillis()) {
            links.remove(token);
            return false;
        }
        return true;
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");
            // "/<token>" or "/<token>/" -> page, "/<token>/data" -> json.
            String token = parts.length > 1 ? parts[1] : "";
            if (!"GET".equals(ex.getRequestMethod()) || !valid(token)) {
                send(ex, 404, "text/plain; charset=utf-8", "Not found");
                return;
            }
            if (parts.length == 3 && parts[2].equals("data")) {
                send(ex, 200, "application/json; charset=utf-8", json(monitor.snapshot()));
            } else if (parts.length <= 2) {
                send(ex, 200, "text/html; charset=utf-8", page);
            } else {
                send(ex, 404, "text/plain; charset=utf-8", "Not found");
            }
        } catch (Exception e) {
            MscLog.debug("monitor request failed", e);
            send(ex, 500, "text/plain; charset=utf-8", "Error");
        } finally {
            ex.close();
        }
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Robots-Tag", "noindex, nofollow");
        ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    // ------------------------------------------------------------------ json

    public String json(ServerMonitor.Snapshot snap) {
        JsonObject root = new JsonObject();
        root.addProperty("server", serverName);
        root.addProperty("version", version);
        root.addProperty("now", System.currentTimeMillis());
        root.addProperty("boot", snap.bootTime());

        JsonArray history = new JsonArray();
        for (ServerMonitor.Sample s : snap.samples()) {
            JsonArray row = new JsonArray();
            row.add(s.time());
            row.add(round(s.tps()));
            row.add(round(s.msptAvg()));
            row.add(round(s.gapMax()));
            row.add(round(s.heapMb()));
            row.add(round(s.heapMaxMb()));
            row.add(round(s.gcMs()));
            row.add(round(s.cpu() * 100));
            row.add(s.entities());
            row.add(s.display());
            row.add(s.armorStands());
            row.add(s.msc());
            row.add(s.players());
            row.add(s.particlePeak());
            row.add(s.particleDropped());
            history.add(row);
        }
        // Column order of each history row, so the page does not guess.
        root.add("columns", array("t", "tps", "mspt", "gap", "heap", "heapMax", "gc", "cpu", "entities", "display",
                "stands", "msc", "players", "particlePeak", "particleDropped"));
        root.add("history", history);

        JsonArray worlds = new JsonArray();
        for (ServerMonitor.WorldStat w : snap.worlds()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", w.name());
            o.addProperty("entities", w.entities());
            o.addProperty("display", w.display());
            o.addProperty("stands", w.armorStands());
            o.addProperty("items", w.items());
            o.addProperty("chunks", w.chunks());
            o.addProperty("players", w.players());
            worlds.add(o);
        }
        root.add("worlds", worlds);

        JsonObject kinds = new JsonObject();
        for (Map.Entry<String, Integer> e : snap.kinds().entrySet()) kinds.addProperty(e.getKey(), e.getValue());
        root.add("kinds", kinds);

        JsonArray freezes = new JsonArray();
        Map<String, Integer> sources = new LinkedHashMap<>();
        for (ServerMonitor.Freeze f : snap.freezes()) {
            JsonObject o = new JsonObject();
            o.addProperty("time", f.time());
            o.addProperty("ms", f.durationMs());
            o.addProperty("source", f.source());
            o.addProperty("where", f.where());
            o.addProperty("gc", f.gcMs());
            o.addProperty("players", f.players());
            o.addProperty("msc", f.msc());
            JsonArray stack = new JsonArray();
            for (String line : f.stack()) stack.add(line);
            o.add("stack", stack);
            freezes.add(o);
            sources.merge(f.source(), 1, Integer::sum);
        }
        root.add("freezes", freezes);

        JsonArray findings = new JsonArray();
        for (MonitorDiagnosis.Finding f : MonitorDiagnosis.analyze(metrics(snap, sources))) {
            JsonObject o = new JsonObject();
            o.addProperty("level", f.level().name().toLowerCase());
            o.addProperty("title", f.title());
            o.addProperty("detail", f.detail());
            findings.add(o);
        }
        root.add("findings", findings);
        return root.toString();
    }

    /** Summarises the last minute of samples (the freezes cover the whole history). */
    static MonitorDiagnosis.Metrics metrics(ServerMonitor.Snapshot snap, Map<String, Integer> freezeSources) {
        List<ServerMonitor.Sample> all = snap.samples();
        int from = Math.max(0, all.size() - 60);
        double tps = 0, mspt = 0, msptMax = 0, heap = 0, gc = 0, cpu = 0;
        int n = 0, msptN = 0;
        long peak = 0, dropped = 0;
        ServerMonitor.Sample last = null;
        for (int i = from; i < all.size(); i++) {
            ServerMonitor.Sample s = all.get(i);
            tps += s.tps();
            if (!Double.isNaN(s.msptAvg())) {
                mspt += s.msptAvg();
                msptN++;
            }
            msptMax = Math.max(msptMax, s.gapMax());
            heap += s.heapMb() / Math.max(1, s.heapMaxMb()) * 100;
            gc += s.gcMs();
            cpu += s.cpu();
            peak = Math.max(peak, s.particlePeak());
            dropped += s.particleDropped();
            n++;
            last = s;
        }
        if (n == 0) n = 1;
        int freezes = 0;
        for (int c : freezeSources.values()) freezes += c;
        return new MonitorDiagnosis.Metrics(
                last == null ? 20 : tps / n, msptN == 0 ? 0 : mspt / msptN, msptMax, heap / n,
                gc / (n * 1000.0), freezes, freezeSources,
                last == null ? 0 : last.entities(), last == null ? 0 : last.display(),
                last == null ? 0 : last.armorStands(), last == null ? 0 : last.msc(),
                (int) peak, snap.particleBudget(), dropped, last == null ? 0 : last.players(), cpu / n);
    }

    private static JsonArray array(String... names) {
        JsonArray a = new JsonArray();
        for (String name : names) a.add(name);
        return a;
    }

    private static double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return Math.round(v * 10.0) / 10.0;
    }
}
