package com.Chagui68.entities.boss;

/**
 * What the Obsidian Sentinel gains each time it changes phase. Every phase keeps the ones before it,
 * so the last phase fights with all four.
 *
 * <ol>
 *     <li><b>Fury</b> (phase 1): attacks come 15% sooner.</li>
 *     <li><b>Obsidian Hide</b> (phase 2): 15% less damage taken.</li>
 *     <li><b>Tempest</b> (phase 3): attacks 15% sooner again, and a telegraphed lightning bolt on a
 *     player every ten seconds.</li>
 *     <li><b>Undying Will</b> (phase 4): regenerates 0.25% of its health a second, deals 20% more
 *     damage, and its destructive attacks come twice as often.</li>
 * </ol>
 *
 * @param attackInterval     multiplier on the pause between attacks
 * @param damageTaken        multiplier on every hit it takes, after its defences
 * @param damageDealt        multiplier on every hit it lands
 * @param regenPerSecond     share of max health restored every second
 * @param lightningEvery     ticks between Tempest's lightning bolts, 0 when it has none
 * @param destructiveFactor  how many times as likely its destructive attacks are
 */
public record SentinelPassives(double attackInterval, double damageTaken, double damageDealt, double regenPerSecond,
                               int lightningEvery, int destructiveFactor) {

    public static final SentinelPassives NONE = new SentinelPassives(1, 1, 1, 0, 0, 1);

    /** The passives a phase grants, its own and every earlier phase's. */
    public static SentinelPassives forPhase(int phase) {
        double interval = 1;
        double taken = 1;
        double dealt = 1;
        double regen = 0;
        int lightning = 0;
        int destructive = 1;
        if (phase >= 1) interval *= 0.85;
        if (phase >= 2) taken *= 0.85;
        if (phase >= 3) {
            interval *= 0.85;
            lightning = 200;
        }
        if (phase >= 4) {
            regen = 0.0025;
            dealt *= 1.2;
            destructive = 2;
        }
        return new SentinelPassives(interval, taken, dealt, regen, lightning, destructive);
    }

    /** The name and the line a phase's new passive is announced with, or null for the first phase. */
    public static String[] announcement(int phase) {
        return switch (phase) {
            case 1 -> new String[]{"FURY", "Its attacks come 15% sooner"};
            case 2 -> new String[]{"OBSIDIAN HIDE", "It takes 15% less damage"};
            case 3 -> new String[]{"TEMPEST", "Faster still, and the storm hunts you"};
            case 4 -> new String[]{"UNDYING WILL", "It regenerates, hits harder and unleashes cataclysms"};
            default -> null;
        };
    }
}
