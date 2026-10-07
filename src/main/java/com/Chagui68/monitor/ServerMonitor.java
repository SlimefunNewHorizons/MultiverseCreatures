package com.Chagui68.monitor;

import com.Chagui68.entities.boss.fx.ParticleBudget;
import com.Chagui68.utils.MscLog;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Watches the server so {@code /msc tps} has something to show: a sample every second, and a
 * watchdog that catches the main thread in the act when a tick stalls.
 *
 * <p>The samples (TPS, tick time, heap, garbage collection, entity counts, boss particles) are taken
 * on the main thread, where the Bukkit API is safe, and read from the web server's thread through
 * {@link #snapshot()}. The watchdog is a daemon thread that never touches the API: it only reads a
 * heartbeat the main thread writes each tick and, when the beat goes stale, the main thread's stack.
 */
public final class ServerMonitor {

    /** One second of the server. */
    public record Sample(long time, double tps, double msptAvg, double gapMax, double heapMb, double heapMaxMb,
                         double gcMs, double cpu, int entities, int display, int armorStands, int msc,
                         int players, long particlePeak, long particleDropped) {
    }

    /** A main-thread stall, with who was running while it lasted. */
    public record Freeze(long time, long durationMs, String source, String where, List<String> stack,
                         long gcMs, int players, int msc) {
    }

    /** A copy of everything the page needs, safe to read from any thread. */
    public record Snapshot(List<Sample> samples, List<Freeze> freezes, List<WorldStat> worlds,
                           Map<String, Integer> kinds, long bootTime, int particleBudget) {
    }

    public record WorldStat(String name, int entities, int display, int armorStands, int items, int chunks,
                            int players) {
    }

    private static final String TAG_PREFIX = "MSC_";
    private static final String JACK_TAG_PREFIX = "msc_jackstar_";
    private static final int SCAN_EVERY_SAMPLES = 5;
    private static final long NANOS_PER_MS = 1_000_000L;

    private final JavaPlugin plugin;
    private final int historySize;
    private final long freezeThresholdMs;

    private final Deque<Sample> samples = new ArrayDeque<>();
    private final Deque<Freeze> freezes = new ArrayDeque<>();
    private List<WorldStat> worlds = List.of();
    private Map<String, Integer> kinds = Map.of();

    private volatile long beat = System.nanoTime();
    private volatile boolean running;
    private Thread mainThread;
    private Thread watchdog;
    private BukkitTask beatTask;
    private BukkitTask sampleTask;

    // Main-thread only.
    private long lastSampleNanos = System.nanoTime();
    private long lastBeatNanos = System.nanoTime();
    private int ticksSinceSample;
    private double gapMaxNanos;
    private long lastGcMs;
    private int sampleCount;
    private volatile int lastMsc;
    private volatile int lastPlayers;
    private volatile int lastBudget;

    public ServerMonitor(JavaPlugin plugin, int historySeconds, long freezeThresholdMs) {
        this.plugin = plugin;
        this.historySize = Math.max(60, historySeconds);
        this.freezeThresholdMs = Math.max(50, freezeThresholdMs);
    }

    /** Must be called on the main thread: it is the thread the watchdog will inspect. */
    public void start() {
        if (running) return;
        running = true;
        mainThread = Thread.currentThread();
        lastGcMs = gcMillis();
        beatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L);
        sampleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sample, 20L, 20L);
        watchdog = new Thread(this::watch, "MSC-Monitor-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    public void stop() {
        running = false;
        if (beatTask != null) beatTask.cancel();
        if (sampleTask != null) sampleTask.cancel();
        if (watchdog != null) watchdog.interrupt();
        beatTask = null;
        sampleTask = null;
        watchdog = null;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(new ArrayList<>(samples), new ArrayList<>(freezes), worlds, kinds,
                ManagementFactory.getRuntimeMXBean().getStartTime(), lastBudget);
    }

    /** The most recent sample, or null before the first second has passed. */
    public synchronized Sample latest() {
        return samples.peekLast();
    }

    // ------------------------------------------------------------------ main thread

    private void onTick() {
        long now = System.nanoTime();
        gapMaxNanos = Math.max(gapMaxNanos, now - lastBeatNanos);
        lastBeatNanos = now;
        ticksSinceSample++;
        beat = now;
    }

    private void sample() {
        long now = System.nanoTime();
        double seconds = Math.max(0.001, (now - lastSampleNanos) / 1e9);
        double tps = Math.min(20.0, ticksSinceSample / seconds);
        lastSampleNanos = now;
        ticksSinceSample = 0;
        double gap = gapMaxNanos / NANOS_PER_MS;
        gapMaxNanos = 0;

        double mspt = averageTickTime();
        Runtime rt = Runtime.getRuntime();
        double used = (rt.totalMemory() - rt.freeMemory()) / 1048576.0;
        double max = rt.maxMemory() / 1048576.0;
        long gc = gcMillis();
        double gcDelta = Math.max(0, gc - lastGcMs);
        lastGcMs = gc;

        if (sampleCount++ % SCAN_EVERY_SAMPLES == 0) scanEntities();

        lastPlayers = Bukkit.getOnlinePlayers().size();
        long[] particles = ParticleBudget.drainStats();
        lastBudget = (int) particles[2];
        int entities = 0, display = 0, stands = 0;
        for (WorldStat w : worlds) {
            entities += w.entities();
            display += w.display();
            stands += w.armorStands();
        }
        Sample s = new Sample(System.currentTimeMillis(), tps, mspt, gap, used, max, gcDelta, cpuLoad(), entities,
                display, stands, lastMsc, lastPlayers, particles[0], particles[1]);
        synchronized (this) {
            samples.addLast(s);
            while (samples.size() > historySize) samples.removeFirst();
        }
    }

    private void scanEntities() {
        List<WorldStat> stats = new ArrayList<>();
        Map<String, Integer> byKind = new HashMap<>();
        int msc = 0;
        for (World world : Bukkit.getWorlds()) {
            int total = 0, display = 0, stands = 0, items = 0;
            for (Entity e : world.getEntities()) {
                total++;
                if (e instanceof Display) display++;
                else if (e instanceof ArmorStand) stands++;
                else if (e instanceof org.bukkit.entity.Item) items++;
                boolean ours = false;
                for (String tag : e.getScoreboardTags()) {
                    if (tag.startsWith(TAG_PREFIX) || tag.startsWith(JACK_TAG_PREFIX)) {
                        byKind.merge(tag, 1, Integer::sum);
                        ours = true;
                        break;
                    }
                }
                if (ours) msc++;
            }
            stats.add(new WorldStat(world.getName(), total, display, stands, items, world.getChunkCount(),
                    world.getPlayers().size()));
        }
        Map<String, Integer> top = new LinkedHashMap<>();
        byKind.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(12)
                .forEach(e -> top.put(e.getKey(), e.getValue()));
        synchronized (this) {
            worlds = stats;
            kinds = top;
        }
        lastMsc = msc;
    }

    private static double averageTickTime() {
        try {
            return Bukkit.getAverageTickTime();
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static long gcMillis() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = bean.getCollectionTime();
            if (t > 0) total += t;
        }
        return total;
    }

    private static double cpuLoad() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
                double load = os.getProcessCpuLoad();
                return load < 0 ? 0 : load;
            }
        } catch (Throwable t) {
            MscLog.debug("the process CPU load is not available", t);
        }
        return 0;
    }

    // ------------------------------------------------------------------ watchdog

    /** Polls the heartbeat; while it is stale, samples the main thread's stack and keeps the commonest culprit. */
    private void watch() {
        long thresholdNanos = freezeThresholdMs * NANOS_PER_MS;
        while (running) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                return;
            }
            long seen = beat;
            if (System.nanoTime() - seen < thresholdNanos) continue;

            Map<String, int[]> votes = new HashMap<>();
            Map<String, StallSource.Blame> blames = new HashMap<>();
            Map<String, List<String>> stacks = new HashMap<>();
            long gcBefore = gcMillis();
            while (running && beat == seen) {
                StackTraceElement[] trace = mainThread.getStackTrace();
                String[] frames = new String[Math.min(trace.length, 40)];
                for (int i = 0; i < frames.length; i++) {
                    frames[i] = trace[i].getClassName() + "." + trace[i].getMethodName();
                }
                StallSource.Blame blame = StallSource.of(frames);
                String key = blame.source() + "|" + blame.where();
                votes.computeIfAbsent(key, k -> new int[1])[0]++;
                blames.putIfAbsent(key, blame);
                List<String> shown = new ArrayList<>();
                for (int i = 0; i < Math.min(trace.length, 14); i++) shown.add(trace[i].toString());
                stacks.putIfAbsent(key, shown);
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    return;
                }
            }
            long durationMs = (System.nanoTime() - seen) / NANOS_PER_MS;
            if (votes.isEmpty() || !running) continue;
            String best = null;
            for (Map.Entry<String, int[]> e : votes.entrySet()) {
                if (best == null || e.getValue()[0] > votes.get(best)[0]) best = e.getKey();
            }
            StallSource.Blame blame = blames.get(best);
            Freeze freeze = new Freeze(System.currentTimeMillis(), durationMs, blame.source(), blame.where(),
                    stacks.get(best), Math.max(0, gcMillis() - gcBefore), lastPlayers, lastMsc);
            synchronized (this) {
                freezes.addLast(freeze);
                while (freezes.size() > 40) freezes.removeFirst();
            }
        }
    }
}
