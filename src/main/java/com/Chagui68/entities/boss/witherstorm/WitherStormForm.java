package com.Chagui68.entities.boss.witherstorm;

/**
 * The five shapes the Wither Storm grows through, in the order the mod grows them: the Wither with a
 * lump on its back, the hunchback that swallowed the middle head, the swollen one with its first
 * tentacles, the three-headed Destroyer and the Devourer.
 *
 * <p>Each form carries the numbers its fight runs on. The ones a server owner is likely to want to
 * change (scale, health, when it evolves, beam reach, flight height) are read again from
 * {@code entities.wither-storm.forms.<key>}; these are their defaults. The rest follow the mod: the
 * hunchbacks crawl after one victim and only pull the one they are looking at, the Destroyer and the
 * Devourer fly high and their beams swallow anything that wanders into them.</p>
 */
public enum WitherStormForm {

    //          key          name                 hp   body  beam  end  fly  keep  speed  turn  bite clus  every skull  power tip   sick  hits  stars suck aim
    HUNCHBACK("hunchback", "The Hunchback", 1.0, 1.0, 20, 1.5, 5, 12, 0.24, 6.0, 1.6, 1, 24, 50, 2.5f, 0.0, 16, 1, 1, 3, 1.4),
    GROWING("growing", "The Growing Hunchback", 1.5, 0.9, 26, 2.0, 6, 14, 0.22, 5.0, 1.8, 1, 24, 60, 3.0f, 0.0, 20, 2, 1, 9, 1.3),
    PREGNANT("pregnant", "The Swollen Hunchback", 2.0, 0.8, 32, 2.5, 8, 16, 0.20, 4.0, 2.2, 2, 15, 60, 3.5f, 1.2, 28, 2, 2, 18, 1.2),
    DESTROYER("destroyer", "The Destroyer", 3.5, 0.6, 64, 5.0, 28, 30, 0.16, 1.6, 4.0, 2, 20, 0, 4.0f, 3.0, 64, 3, 3, 0, 0.9),
    DEVOURER("devourer", "The Devourer", 5.0, 0.5, 96, 7.0, 40, 44, 0.13, 1.0, 5.0, 3, 16, 0, 5.0f, 4.5, 96, 4, 5, 0, 0.8);

    private final String key;
    private final String displayName;
    /** Max health is the configured base health times this. */
    private final double healthMultiplier;
    /** Share of a hit on the mass that gets through; the heads always take it all. */
    private final double bodyDamageMultiplier;
    /** How far a tractor beam reaches, in blocks, and how wide it is at the far end. */
    private final double beamRange;
    private final double beamEndRadius;
    /** Height above the ground the storm keeps, and how far it likes to stay from its victim. */
    private final double flyHeight;
    private final double keepDistance;
    /** Blocks per tick it moves at most, and degrees per tick its body turns at most. */
    private final double moveSpeed;
    private final double turnRate;
    /** How close to a mouth something has to be dragged before the head bites it. */
    private final double biteRadius;
    /** Radius of the chunk of ground a beam tears out, and ticks between two chunks. */
    private final int clusterRadius;
    private final int clusterInterval;
    /** Ticks between two plain wither skulls per head; 0 for the forms that stopped spitting them. */
    private final int skullInterval;
    /** Explosion power of a flaming skull. */
    private final float flamingSkullPower;
    /** Reach of a tentacle tip; 0 for the forms without tentacles. */
    private final double tentacleReach;
    /** Radius of the wither sickness around the storm. */
    private final double sicknessRadius;
    /** Projectile hits that injure a head (a random number up to twice this). */
    private final int injuryHits;
    /** Nether stars it leaves when it dies in this form. */
    private final int netherStars;
    /**
     * Lumps of ground the hunchbacks suck into their mass at each pickup, like the mod's hunchback
     * cluster source (3, 9 and 18 in phases 1 to 3); 0 for the forms that feed through their beams.
     */
    private final int suctionClusters;
    /**
     * Degrees per tick a head turns while its beam is on. Slower than a sprint across the beam, so a
     * player who runs sideways out of it gets away, as in the mod.
     */
    private final double beamAimRate;

    private WitherStormModel model;
    private WitherStormModel detailedModel;

    WitherStormForm(String key, String displayName, double healthMultiplier, double bodyDamageMultiplier,
                    double beamRange, double beamEndRadius, double flyHeight, double keepDistance, double moveSpeed,
                    double turnRate, double biteRadius, int clusterRadius, int clusterInterval, int skullInterval,
                    float flamingSkullPower, double tentacleReach, double sicknessRadius, int injuryHits, int netherStars,
                    int suctionClusters, double beamAimRate) {
        this.key = key;
        this.displayName = displayName;
        this.healthMultiplier = healthMultiplier;
        this.bodyDamageMultiplier = bodyDamageMultiplier;
        this.beamRange = beamRange;
        this.beamEndRadius = beamEndRadius;
        this.flyHeight = flyHeight;
        this.keepDistance = keepDistance;
        this.moveSpeed = moveSpeed;
        this.turnRate = turnRate;
        this.biteRadius = biteRadius;
        this.clusterRadius = clusterRadius;
        this.clusterInterval = clusterInterval;
        this.skullInterval = skullInterval;
        this.flamingSkullPower = flamingSkullPower;
        this.tentacleReach = tentacleReach;
        this.sicknessRadius = sicknessRadius;
        this.injuryHits = injuryHits;
        this.netherStars = netherStars;
        this.suctionClusters = suctionClusters;
        this.beamAimRate = beamAimRate;
    }

    /** The form's model, read from the jar the first time it is asked for. */
    public synchronized WitherStormModel model() {
        if (model == null) {
            model = WitherStormModel.load(key);
        }
        return model;
    }

    /**
     * The model the storm wears: the Destroyer has the mod's full 401-voxel mass when {@code detailed}
     * and its low-detail one (33 boxes) otherwise; the other forms have one model.
     */
    public synchronized WitherStormModel model(boolean detailed) {
        if (!detailed || this != DESTROYER) return model();
        if (detailedModel == null) {
            detailedModel = WitherStormModel.load(key + "-detailed");
        }
        return detailedModel;
    }

    /**
     * Whether a head carries a tractor beam, as the mod's tractorBeamActive decides: none in the
     * first form, only the middle head in the hunchbacks after it, every head from the Destroyer on.
     */
    public boolean hasBeam(int head) {
        if (this == HUNCHBACK) return false;
        return isColossal() || head == 0;
    }

    /** The next form, or null for the Devourer. */
    public WitherStormForm next() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    /** Whether this form flies high and pulls in everything its beams touch, like the mod's phase 4 on. */
    public boolean isColossal() {
        return ordinal() >= DESTROYER.ordinal();
    }

    public static WitherStormForm byKey(String key) {
        for (WitherStormForm form : values()) {
            if (form.key.equalsIgnoreCase(key)) return form;
        }
        return null;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double bodyDamageMultiplier() {
        return bodyDamageMultiplier;
    }

    public double beamRange() {
        return beamRange;
    }

    public double beamEndRadius() {
        return beamEndRadius;
    }

    public double flyHeight() {
        return flyHeight;
    }

    public double keepDistance() {
        return keepDistance;
    }

    public double moveSpeed() {
        return moveSpeed;
    }

    public double turnRate() {
        return turnRate;
    }

    public double biteRadius() {
        return biteRadius;
    }

    public int clusterRadius() {
        return clusterRadius;
    }

    public int clusterInterval() {
        return clusterInterval;
    }

    public int skullInterval() {
        return skullInterval;
    }

    public float flamingSkullPower() {
        return flamingSkullPower;
    }

    public double tentacleReach() {
        return tentacleReach;
    }

    public double sicknessRadius() {
        return sicknessRadius;
    }

    public int injuryHits() {
        return injuryHits;
    }

    public int netherStars() {
        return netherStars;
    }

    public int suctionClusters() {
        return suctionClusters;
    }

    public double beamAimRate() {
        return beamAimRate;
    }
}
