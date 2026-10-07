package com.Chagui68.entities.boss.fx;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps the bosses' particles from flooding the network.
 *
 * <p>Every particle call is one packet to every player who receives it. The boss effects drew each
 * point of a shape as its own call, forced to every player within 512 blocks, so a single blast
 * dome was a few hundred packets per player per tick and a fight with several players saturated
 * the connection: ping spikes and timeouts. Two limits fix that:
 * <ul>
 *     <li><b>Budget</b> — at most {@code performance.particle-budget-per-tick} calls go out per
 *     tick. When the previous tick asked for more, every call is kept with the same probability, so
 *     all effects thin out evenly instead of the last ones vanishing; a hard ceiling at twice the
 *     budget catches a sudden burst.</li>
 *     <li><b>Reach</b> — particles go only to players within
 *     {@code performance.particle-view-distance} blocks, not 512.</li>
 * </ul>
 */
public final class ParticleBudget {

    public static final int DEFAULT_BUDGET = 150;
    public static final int DEFAULT_VIEW_DISTANCE = 48;

    private static int budget = DEFAULT_BUDGET;
    private static int viewDistance = DEFAULT_VIEW_DISTANCE;

    private static int tick = Integer.MIN_VALUE;
    private static int demand;
    private static int sent;
    private static double keep = 1.0;
    private static int peakDemand;
    private static long dropped;

    private ParticleBudget() {
    }

    /** Reads the limits; called on enable and on {@code /msc reload}. */
    public static void load(ConfigurationSection config) {
        budget = Math.max(10, config == null ? DEFAULT_BUDGET : config.getInt("performance.particle-budget-per-tick", DEFAULT_BUDGET));
        viewDistance = Math.max(16, config == null ? DEFAULT_VIEW_DISTANCE
                : config.getInt("performance.particle-view-distance", DEFAULT_VIEW_DISTANCE));
    }

    /** The share of calls kept when the last tick asked for {@code demand} against {@code budget}. */
    static double keepShare(int demand, int budget) {
        return demand <= budget ? 1.0 : budget / (double) demand;
    }

    /** Whether one more particle call may go out this tick. */
    static synchronized boolean allow(int now, double roll) {
        if (now != tick) {
            peakDemand = Math.max(peakDemand, demand);
            keep = keepShare(demand, budget);
            tick = now;
            demand = 0;
            sent = 0;
        }
        demand++;
        if (sent >= budget * 2 || (keep < 1.0 && roll >= keep)) {
            dropped++;
            return false;
        }
        sent++;
        return true;
    }

    /**
     * The busiest tick and the calls dropped since the last read, then zeroed: what the server
     * monitor charts. Index 0 is the peak demand, 1 the dropped calls, 2 the budget.
     */
    public static synchronized long[] drainStats() {
        long[] stats = {Math.max(peakDemand, demand), dropped, budget};
        peakDemand = 0;
        dropped = 0;
        return stats;
    }

    /** Spawns a particle within the budget, to the players in reach. */
    public static void spawn(World world, Particle type, double x, double y, double z, int count,
                             double spreadX, double spreadY, double spreadZ, double speed, Object data) {
        if (!allow(Bukkit.getCurrentTick(), ThreadLocalRandom.current().nextDouble())) return;
        type.builder()
                .location(world, x, y, z)
                .count(count)
                .offset(spreadX, spreadY, spreadZ)
                .extra(speed)
                .data(data)
                .receivers(viewDistance, true)
                .force(false)
                .spawn();
    }
}
