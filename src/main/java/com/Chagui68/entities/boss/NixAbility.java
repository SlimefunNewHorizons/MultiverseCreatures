package com.Chagui68.entities.boss;

/**
 * NIX's arsenal: ten executions he draws from alongside his three signature moves, and three
 * destructive attacks on a cooldown of their own.
 * {@link NixArsenal} plays them.
 *
 * <p>{@code channel} is how long he stands rooted and posed while casting; what the attack leaves
 * behind keeps going after that.
 */
public enum NixAbility {
    /** His axe thrown at the target, spinning out and back. */
    HEADSMANS_TOSS(20, BossGesture.SWEEP, 4, 22, "Headsman's Toss"),
    /** A spreading pool of blood that slows and withers, and feeds him. */
    BLOOD_POOL(24, BossGesture.CROUCH, 0, 8, "Blood Pool"),
    /** A ring of spikes closing on the target. */
    IRON_MAIDEN(20, BossGesture.POINT, 0, 20, "Iron Maiden"),
    /** A noose that hunts the target, hoists it and lets it drop. */
    NOOSE(20, BossGesture.CAST, 0, 20, "Gallows Noose"),
    /** Three marching stomps towards the target, each a shockwave. */
    MARCH(54, BossGesture.THRUST, 3, 16, "Executioner's March"),
    /** Wooden stocks lock up to three players in place. */
    PILLORY(20, BossGesture.POINT, 0, 22, "Pillory"),
    /** Blood falling in telegraphed drops around the players. */
    CRIMSON_RAIN(24, BossGesture.CAST, 0, 20, "Crimson Rain"),
    /** Two blades crossing in an X over the target. */
    SEVERING_CROSS(30, BossGesture.SWEEP, 0, 18, "Severing Cross"),
    /** Three tolls of a bell, then the wounded are executed. */
    LAST_RITES(46, BossGesture.CAST, 0, 24, "Last Rites"),
    /** Two chains swung around him twice, dragging in whoever they catch. */
    CHAIN_WHIRL(50, BossGesture.SPREAD, 0, 8, "Chain Whirl"),
    /** Destructive: a guillotine as tall as a house rises over the target and its blade falls. */
    GRAND_GUILLOTINE(108, BossGesture.CAST, 0, 24, "Grand Guillotine", true),
    /** Destructive: a blood moon rises, then three waves of blood roll out across the arena. */
    BLOOD_MOON(124, BossGesture.CAST, 0, 22, "Blood Moon", true),
    /** Destructive: eight giant axes rise around the arena and sweep in to the centre, twice. */
    EXECUTION_DAY(128, BossGesture.SPREAD, 0, 18, "Execution Day", true);

    public final int channel;
    public final BossGesture gesture;
    public final double minRange;
    public final double maxRange;
    public final String label;
    /** A destructive attack: a long charge, a blow across the arena, on its own cooldown. */
    public final boolean destructive;

    NixAbility(int channel, BossGesture gesture, double minRange, double maxRange, String label) {
        this(channel, gesture, minRange, maxRange, label, false);
    }

    NixAbility(int channel, BossGesture gesture, double minRange, double maxRange, String label, boolean destructive) {
        this.channel = channel;
        this.gesture = gesture;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.label = label;
        this.destructive = destructive;
    }

    public boolean fits(double distance) {
        return distance >= minRange && distance <= maxRange;
    }
}
