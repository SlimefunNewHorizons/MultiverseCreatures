package com.Chagui68.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the monitor's numbers into plain sentences: what is wrong and what usually causes it.
 *
 * <p>Pure on a {@link Metrics} value, so every rule has a unit test. The thresholds are the ones
 * server owners already know: 20 TPS is healthy, a tick over 50 ms is a late tick, a heap above 90%
 * leaves the garbage collector no room.
 */
public final class MonitorDiagnosis {

    public enum Level { OK, INFO, WARN, CRITICAL }

    public record Finding(Level level, String title, String detail) {
    }

    /** What the sampler measured over the window the page shows. */
    public record Metrics(
            double tps, double msptAvg, double msptMax, double heapPercent, double gcShare,
            int freezes, Map<String, Integer> freezeSources, int entities, int displayEntities,
            int armorStands, int mscEntities, int particlePeakDemand, int particleBudget,
            long particleDropped, int players, double cpuProcess) {
    }

    private MonitorDiagnosis() {
    }

    public static List<Finding> analyze(Metrics m) {
        List<Finding> out = new ArrayList<>();

        if (m.tps() < 12) {
            out.add(new Finding(Level.CRITICAL, "TPS " + one(m.tps()),
                    "The server runs at a fraction of its speed: everything lags, mobs and bosses stutter."));
        } else if (m.tps() < 18) {
            out.add(new Finding(Level.WARN, "TPS " + one(m.tps()), "Ticks are arriving late."));
        }

        if (m.msptAvg() > 50) {
            out.add(new Finding(Level.CRITICAL, "Ticks take " + one(m.msptAvg()) + " ms",
                    "A tick has 50 ms. Past that the server cannot keep 20 TPS: the work per tick is too much."));
        } else if (m.msptAvg() > 35) {
            out.add(new Finding(Level.WARN, "Ticks take " + one(m.msptAvg()) + " ms",
                    "Little headroom left: a boss attack or a chunk load will tip it over 50 ms."));
        }

        if (m.heapPercent() > 90) {
            out.add(new Finding(Level.CRITICAL, "Memory at " + (int) m.heapPercent() + "%",
                    "The heap is nearly full. The garbage collector runs back to back and freezes the server. "
                            + "Raise -Xmx or find what is holding memory (loaded chunks, entities, plugins)."));
        } else if (m.heapPercent() > 80) {
            out.add(new Finding(Level.WARN, "Memory at " + (int) m.heapPercent() + "%",
                    "Close to the limit; freezes from garbage collection are likely."));
        }

        if (m.gcShare() > 0.10) {
            out.add(new Finding(Level.CRITICAL, "Garbage collection takes " + (int) (m.gcShare() * 100) + "% of the time",
                    "Every collection pauses the server. That reads as freezes with no plugin to blame."));
        } else if (m.gcShare() > 0.04) {
            out.add(new Finding(Level.WARN, "Garbage collection takes " + (int) (m.gcShare() * 100) + "% of the time",
                    "Noticeable; watch whether the freezes line up with the memory curve."));
        }

        if (m.freezes() > 0) {
            String top = null;
            int topCount = 0;
            for (Map.Entry<String, Integer> e : m.freezeSources().entrySet()) {
                if (e.getValue() > topCount) {
                    top = e.getKey();
                    topCount = e.getValue();
                }
            }
            out.add(new Finding(m.freezes() >= 5 ? Level.CRITICAL : Level.WARN,
                    m.freezes() + " freeze" + (m.freezes() == 1 ? "" : "s") + " captured",
                    top == null ? "See the stacks below."
                            : "Most of them were inside " + top + " (" + topCount + " of " + m.freezes()
                            + "). The stacks below show the exact method."));
        }

        if (m.mscEntities() > 0 && m.particleBudget() > 0 && m.particlePeakDemand() > m.particleBudget() * 2) {
            out.add(new Finding(Level.WARN, "Boss particles over budget",
                    "A fight asked for " + m.particlePeakDemand() + " particle calls in one tick (budget "
                            + m.particleBudget() + "); " + m.particleDropped() + " were dropped. "
                            + "Lower performance.particle-budget-per-tick or the view distance if players ping-spike."));
        }

        if (m.displayEntities() > 4000) {
            out.add(new Finding(Level.WARN, m.displayEntities() + " display entities",
                    "Block, item and text displays are cheap one by one, but thousands sync to every viewer. "
                            + "Large Wither Storm forms and bosses with many pieces add up."));
        }
        if (m.armorStands() > 3000) {
            out.add(new Finding(Level.WARN, m.armorStands() + " armor stands",
                    "Armor stands tick and sync every frame. /msc cleanstands removes leftovers."));
        }
        if (m.entities() > 15000) {
            out.add(new Finding(Level.WARN, m.entities() + " entities loaded",
                    "More entities than a server comfortably ticks. Mob caps, item merging and /msc kill help."));
        }

        if (m.cpuProcess() > 0.9) {
            out.add(new Finding(Level.WARN, "CPU at " + (int) (m.cpuProcess() * 100) + "%",
                    "The host is saturated, so ticks slow down whatever the plugins do."));
        }

        if (out.isEmpty()) {
            out.add(new Finding(Level.OK, "No problems detected",
                    "TPS, tick time, memory and garbage collection are inside healthy ranges."));
        }
        return out;
    }

    private static String one(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
