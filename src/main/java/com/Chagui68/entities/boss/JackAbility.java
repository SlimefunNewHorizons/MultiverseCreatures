package com.Chagui68.entities.boss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * JackStar's arsenal: twenty attacks, four unlocked by each of his five phases, so every phase
 * fights with its own set instead of repeating the same few, plus three destructive attacks he
 * casts on their own cooldown from phase 2. {@link JackArsenal} plays them.
 *
 * <p>{@code channel} is how long he stands rooted and posed while casting; what the attack leaves
 * behind (puddles, falling bolts, a closing ring) keeps going after that.
 */
public enum JackAbility {
    // Phase 1: probing the player.
    PING_FLOOD(1, 44, BossGesture.POINT, 3, 28, "Ping Flood", "ping -f %s"),
    NULL_POINTER(1, 30, BossGesture.POINT, 0, 24, "Null Pointer", "*%s = NULL;"),
    SYNTAX_ERROR(1, 24, BossGesture.THRUST, 0, 8, "Syntax Error", "eval(\"%s;;;\")"),
    PACKET_SNIFFER(1, 48, BossGesture.SWEEP, 0, 10, "Packet Sniffer", "tcpdump -i arena"),
    // Phase 2: wearing the arena down.
    MEMORY_LEAK(2, 24, BossGesture.CAST, 0, 24, "Memory Leak", "malloc(∞)"),
    INFINITE_LOOP(2, 30, BossGesture.SPREAD, 0, 20, "Infinite Loop", "while(%s) {}"),
    ROLLBACK(2, 20, BossGesture.POINT, 0, 20, "Ctrl+Z", "git reset --hard HEAD~1"),
    FIREWALL_RING(2, 20, BossGesture.CROUCH, 0, 12, "Firewall Ring", "iptables -A INPUT -j DROP"),
    // Phase 3: the Warden protocol.
    THREAD_SPIKES(3, 30, BossGesture.THRUST, 3, 20, "Thread Spikes", "new Thread[3].start()"),
    DDOS(3, 24, BossGesture.CAST, 0, 26, "DDoS", "flood --target %s"),
    ENCRYPTION_LOCK(3, 24, BossGesture.POINT, 0, 18, "Encryption Lock", "gpg --encrypt %s"),
    PORT_SCAN(3, 56, BossGesture.SPREAD, 0, 14, "Port Scan", "nmap -p- arena"),
    // Phase 4: hitbox alchemy.
    SEGFAULT(4, 30, BossGesture.CAST, 3, 20, "Segmentation Fault", "*(int*)0 = 0;"),
    OVERCLOCK(4, 16, BossGesture.CROUCH, 0, 10, "Overclock", "cpufreq --max"),
    RECURSIVE_CLONE(4, 24, BossGesture.SPREAD, 0, 20, "Recursive Clone", "fork() && fork()"),
    BLUE_SCREEN(4, 34, BossGesture.CAST, 0, 14, "Blue Screen", "KeBugCheck(0xDEAD)"),
    // Phase 5: kernel panic.
    RM_RF(5, 46, BossGesture.CAST, 0, 9, "rm -rf /", "sudo rm -rf / --no-preserve-root"),
    ROOT_ACCESS(5, 44, BossGesture.SPREAD, 0, 20, "Root Access", "su root"),
    ZERO_DAY(5, 24, BossGesture.SWEEP, 4, 24, "Zero Day", "exploit %s"),
    SYSTEM_CRASH(5, 30, BossGesture.CAST, 0, 26, "System Crash", "kill -9 1"),
    // Destructive (phase 0: outside the phase pools, cast on their own cooldown from phase 2).
    KERNEL_NUKE(0, 128, BossGesture.CAST, 0, 30, "Kernel Nuke", "launch --nuke %s"),
    DISK_FORMAT(0, 118, BossGesture.SPREAD, 0, 20, "Disk Format", "format C: /y"),
    SUDO_LASER(0, 104, BossGesture.THRUST, 0, 30, "sudo laser", "sudo laser --sweep 360");

    /** How many recent picks a new pick avoids, so the same attack is never cast twice running. */
    public static final int MEMORY = 3;
    /** Weight of the current phase's own attacks against the previous phase's. */
    static final int OWN_WEIGHT = 3;
    static final int PREVIOUS_WEIGHT = 1;

    public final int phase;
    public final int channel;
    public final BossGesture gesture;
    public final double minRange;
    public final double maxRange;
    public final String label;
    /** The line typed into the console when it is cast; {@code %s} is the target's name. */
    public final String command;

    JackAbility(int phase, int channel, BossGesture gesture, double minRange, double maxRange,
                String label, String command) {
        this.phase = phase;
        this.channel = channel;
        this.gesture = gesture;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.label = label;
        this.command = command;
    }

    public boolean fits(double distance) {
        return distance >= minRange && distance <= maxRange;
    }

    /** The attacks a phase can cast: its own four, and its predecessor's at a lower weight. */
    public static List<JackAbility> pool(int phase) {
        List<JackAbility> out = new ArrayList<>();
        for (JackAbility a : values()) {
            if (a.destructive()) continue;
            if (a.phase == phase || a.phase == phase - 1) out.add(a);
        }
        return out;
    }

    /** A destructive attack: a long charge and a blow across the arena, on its own cooldown. */
    public boolean destructive() {
        return phase == 0;
    }

    /** The destructive attacks. */
    public static List<JackAbility> cataclysms() {
        List<JackAbility> out = new ArrayList<>();
        for (JackAbility a : values()) {
            if (a.destructive()) out.add(a);
        }
        return out;
    }

    static int weight(JackAbility ability, int phase) {
        if (ability.phase == phase) return OWN_WEIGHT;
        return ability.phase == phase - 1 ? PREVIOUS_WEIGHT : 0;
    }

    /**
     * The next attack for {@code phase} at {@code distance}, skipping the {@code recent} ones; when
     * every attack in range was cast recently the recent ones are allowed again rather than none.
     *
     * @return the pick, or {@code null} when nothing in the pool reaches that far
     */
    public static JackAbility pick(int phase, double distance, Collection<JackAbility> recent, Random random) {
        JackAbility pick = pick(phase, distance, recent, random, true);
        return pick != null ? pick : pick(phase, distance, recent, random, false);
    }

    private static JackAbility pick(int phase, double distance, Collection<JackAbility> recent, Random random,
                                    boolean avoidRecent) {
        int total = 0;
        for (JackAbility a : pool(phase)) {
            if (!a.fits(distance) || (avoidRecent && recent.contains(a))) continue;
            total += weight(a, phase);
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (JackAbility a : pool(phase)) {
            if (!a.fits(distance) || (avoidRecent && recent.contains(a))) continue;
            roll -= weight(a, phase);
            if (roll < 0) return a;
        }
        return null;
    }
}
