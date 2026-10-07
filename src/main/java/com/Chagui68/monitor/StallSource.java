package com.Chagui68.monitor;

/**
 * Names who is to blame for a main-thread stack captured while the server was frozen.
 *
 * <p>Pure on class names, so a unit test can feed it frames without a server. The first frame from
 * code that is neither the JDK nor the server decides the plugin; when there is none, the stall is
 * the server's own (chunks, entities, the world save) or the thread was simply waiting.
 */
public final class StallSource {

    public static final String SERVER = "Server (vanilla / Paper)";
    public static final String WAITING = "Waiting (lock, sleep, disk or network)";

    private static final String[] SERVER_PREFIXES = {
            "net.minecraft.", "org.bukkit.craftbukkit.", "io.papermc.", "ca.spottedleaf.", "com.destroystokyo.",
            "org.purpurmc.", "org.spigotmc.", "org.bukkit.", "net.kyori.", "com.mojang.", "it.unimi.", "com.google.",
            "org.apache.", "org.joml.", "io.netty."};

    private static final String[] JDK_PREFIXES = {"java.", "javax.", "jdk.", "sun.", "com.sun."};

    private StallSource() {
    }

    /** A stack frame reduced to what the page shows: who, and where. */
    public record Blame(String source, String where) {
    }

    /** Picks the culprit of a stack, listed from the innermost frame outwards. */
    public static Blame of(String[] frames) {
        if (frames == null || frames.length == 0) return new Blame(SERVER, "unknown");
        String top = frames[0];
        if (startsWithAny(top, "java.lang.Thread.sleep", "jdk.internal.misc.Unsafe.park",
                "java.util.concurrent.locks.LockSupport", "java.lang.Object.wait", "java.net.", "sun.nio.ch.",
                "java.io.", "sun.nio.fs.", "java.nio.file.", "sun.security")) {
            // A plugin frame further down still names who asked for the wait.
            for (String frame : frames) {
                String plugin = pluginOf(frame);
                if (plugin != null) return new Blame(plugin + " (waiting)", shorten(frame));
            }
            return new Blame(WAITING, shorten(top));
        }
        for (String frame : frames) {
            String plugin = pluginOf(frame);
            if (plugin != null) return new Blame(plugin, shorten(frame));
        }
        for (String frame : frames) {
            if (startsWithAny(frame, "net.minecraft.", "org.bukkit.craftbukkit.")) {
                return new Blame(SERVER, shorten(frame));
            }
        }
        return new Blame(SERVER, shorten(top));
    }

    /** {@code frame} is {@code package.Class.method}; returns the plugin it belongs to, or null. */
    static String pluginOf(String frame) {
        if (frame.startsWith("com.Chagui68.")) return "MultiverseCreatures";
        if (startsWithAny(frame, SERVER_PREFIXES) || startsWithAny(frame, JDK_PREFIXES)) return null;
        if (frame.startsWith("io.github.thebusybiscuit.slimefun4.") || frame.startsWith("me.mrCookieSlime.")) {
            return "Slimefun";
        }
        String[] parts = frame.split("\\.");
        if (parts.length < 3) return null;
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    private static boolean startsWithAny(String text, String... prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) return true;
        }
        return false;
    }

    /** {@code com.Chagui68.entities.boss.fx.Fx.circle} becomes {@code Fx.circle}. */
    static String shorten(String frame) {
        String[] parts = frame.split("\\.");
        if (parts.length < 2) return frame;
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
