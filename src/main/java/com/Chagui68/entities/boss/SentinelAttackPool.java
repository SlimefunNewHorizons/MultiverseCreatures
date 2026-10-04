package com.Chagui68.entities.boss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Which attack the Obsidian Sentinel throws next, as pure functions so the rotation can be tested
 * without a server. Every attack shares one clock: when it comes round, {@link #next} rolls the
 * kind of attack and then the attack itself.
 *
 * <p>Two problems lived in the old inline arrays. At the distance players actually fight a
 * fourteen-block boss from (five to fifteen blocks), the medium list held ten distinct attacks and
 * was used 85% of the time, so a fight looked like the same ten moves on a loop; and {@code
 * doombeam} and {@code rainoflances} were registered but listed nowhere, so they never fired.
 * Every registered attack now appears in at least one pool (a test pins that down), and
 * {@link #pick} skips the last {@link #HISTORY} attacks thrown so the rotation keeps moving.
 */
public final class SentinelAttackPool {

    /**
     * How many of the most recent attacks are kept out of the next pick: an attack waits for three
     * others before it can come back. Every attack counts, the big ones and the defences included.
     */
    public static final int HISTORY = 3;

    /** Aerial attacks thrown before a flight may end early; the pools below hold five or more each. */
    public static final int AERIAL_ATTACKS_PER_FLIGHT = 5;

    /** Ground attacks for a player within melee reach of the boss. */
    public static final List<String> GROUND_CLOSE = List.of(
            "shieldbash", "warstomp", "chaingrapple", "armorspikes", "mirrorimage", "vortexpull",
            "groundshatter", "lanceflurry", "whirlwindslash", "executionsweep", "earthmaw",
            "shadowstep", "runeward", "spearcyclone", "cataclysm", "tremorlance", "gravecleaver");

    /** Ground attacks for the usual fighting distance. */
    public static final List<String> GROUND_MEDIUM = List.of(
            "lancestorm", "earthpillar", "groundshatter", "armorspikes", "vortexpull", "lanceflurry",
            "whirlwindslash", "obsidianspire", "earthmaw", "runeward", "doombeam", "chaingrapple",
            "shadowstep", "sunderingcharge", "spearcyclone", "cataclysm", "tremorlance", "aegisrush",
            "gravecleaver");

    /** Ground attacks that reach a player keeping their distance. */
    public static final List<String> GROUND_FAR = List.of(
            "shieldbash", "obsidianspire", "doombeam", "chaingrapple", "shadowstep", "earthpillar",
            "sunderingcharge", "aegisrush");

    /** Projectiles and beams, thrown from the ground at medium and long range. */
    public static final List<String> RANGED = List.of(
            "lancesnipe", "meteorstorm", "voidbeam", "frostlance", "lightningspear", "shadowvolley",
            "chainlightning", "crystalbarrage", "arcaneorb", "voidrift", "arcanemissiles", "spiritbeam",
            "soultethers", "plaguebrand", "runemines", "obsidianprison", "shardburst", "gravityorb",
            "javelinvolley", "sweepinglaser");

    public static final List<String> AERIAL_CLOSE = List.of(
            "aerialrush", "crossslash", "novaburst", "obsidianwings", "bladering", "rainoflances",
            "phantomlegion", "chainhook", "spiralstorm");

    public static final List<String> AERIAL_MEDIUM = List.of(
            "sonicboom", "windcutter", "gravitywell", "darkorb", "aerialrush", "eclipsefall",
            "rainoflances", "voidmeteor", "phantomlegion", "chainhook", "spiralstorm");

    public static final List<String> AERIAL_FAR = List.of(
            "starfall", "lightningstorm", "heavenlyjudgment", "darkorb", "eclipsefall", "rainoflances",
            "voidmeteor", "spiralstorm");

    /** Defences that raise a defensive state; only one state holds at a time. */
    public static final List<String> DEFENSE_STATES = List.of(
            "stoneskin", "reflectbarrier", "absorbshield", "bulwark", "thornaura", "afterimage");

    /** Defences that give health back; only one runs at a time. */
    public static final List<String> DEFENSE_HEALS = List.of(
            "healingcircle", "regeneration", "soulsiphon", "obsidiancocoon");

    /** Rites that call minions to the fight; they share a cap on how many may be alive at once. */
    public static final List<String> SUMMONINGS = List.of(
            "lancesquires", "obsidianmender", "emberhounds", "voidwisps", "obsidianbrute",
            "elementalconclave", "shadowambush", "necropolisrite", "arcanecovenant");

    /** The call that brings one of the plugin's bosses into the fight: once per phase. */
    public static final String CHAMPION_CALL = "championcall";

    /** Long-charged attacks of enormous reach: the moments the whole arena has to run from. */
    public static final List<String> DESTRUCTIVES = List.of(
            "orbitalstrike", "meteorimpact", "supernova", "judgmentpillars", "earthsplitter",
            "voidcollapse", "obsidiantsunami", "solarlance", "worldbreaker", "apocalypserain");

    /** Attacks that are a kind of their own rather than one pick of a pool. */
    public static final String FLY_UP = "flyup";
    public static final String HOVER_BARRAGE = "hoverbarrage";
    public static final String SHIELD_SEAL = "shieldseal";
    public static final String AEGIS_JUDGMENT = "groundslam";
    public static final String TRIANGLE_CALL = "trianglecall";

    /** The kinds of attack the attack tick rolls between; the attack itself is picked inside the kind. */
    public enum Kind {MELEE, RANGED, FLIGHT, HOVER_BARRAGE, SHIELD_SEAL, AEGIS_JUDGMENT, DEFENSE, SUMMON, AERIAL,
        SUMMONING, DESTRUCTIVE}

    /**
     * What the Sentinel is doing when its attack tick comes round: which kinds can be thrown now.
     *
     * @param distance     to the nearest player
     * @param health       share of its health left, 0..1
     * @param flying       in the air (aerial attacks and ranged ones only)
     * @param sealed       behind its shield seal (ranged attacks, Aegis Judgment and the call for help)
     * @param shieldInHand its shield is in hand, so Aegis Judgment can throw it
     * @param defenseFree  no defensive state is up
     * @param healFree     no healing defence is running
     * @param summonFree   no call for help is running
     * @param summonRoom   fewer minions are alive than the cap, so another rite may be cast
     * @param championReady no boss has been called yet in this phase
     * @param destructiveReady the last destructive attack is far enough behind
     */
    public record Situation(double distance, double health, boolean flying, boolean sealed, boolean shieldInHand,
                            boolean defenseFree, boolean healFree, boolean summonFree, boolean summonRoom,
                            boolean championReady, boolean destructiveReady) {

        /** A grounded, healthy Sentinel with every kind available: a starting point for tests. */
        public static Situation open(double distance, double health) {
            return new Situation(distance, health, false, false, true, true, true, true, true, true, true);
        }
    }

    /** How likely each kind is when it is available; a kind at 0 is never rolled. */
    public record Weights(int melee, int ranged, int flight, int hoverBarrage, int shieldSeal, int aegisJudgment,
                          int defense, int summon, int aerial, int summoning, int destructive) {
        public static final Weights DEFAULT = new Weights(30, 20, 10, 8, 8, 8, 12, 25, 80, 10, 6);

        /** The same weights with the destructive kind {@code factor} times as likely. */
        public Weights withDestructive(int factor) {
            return new Weights(melee, ranged, flight, hoverBarrage, shieldSeal, aegisJudgment, defense, summon, aerial,
                    summoning, destructive * factor);
        }

        int of(Kind kind) {
            return switch (kind) {
                case MELEE -> melee;
                case RANGED -> ranged;
                case FLIGHT -> flight;
                case HOVER_BARRAGE -> hoverBarrage;
                case SHIELD_SEAL -> shieldSeal;
                case AEGIS_JUDGMENT -> aegisJudgment;
                case DEFENSE -> defense;
                case SUMMON -> summon;
                case AERIAL -> aerial;
                case SUMMONING -> summoning;
                case DESTRUCTIVE -> destructive;
            };
        }
    }

    /** Below this share of health the defences come into play. */
    public static final double DEFENSE_HEALTH = 0.9;
    /** Below this share of health the healing defences come into play. */
    public static final double HEAL_HEALTH = 0.7;

    private SentinelAttackPool() {
    }

    /**
     * The attack for one attack tick: a weighted roll for the kind, then a random attack of that kind
     * that is not among {@code recent}. Kinds with nothing fresh to throw drop out of the roll; when
     * every kind has dropped out the recent attacks are allowed again rather than standing idle.
     *
     * @param flightDone attacks already thrown during this flight, kept out of the aerial pick
     * @return the attack name ({@link #FLY_UP} to take off), or null when nothing can be thrown
     */
    public static String next(Situation s, Weights weights, Collection<String> recent, Collection<String> flightDone,
                              Random random) {
        String pick = next(s, weights, recent, flightDone, random, true);
        return pick != null ? pick : next(s, weights, recent, flightDone, random, false);
    }

    private static String next(Situation s, Weights weights, Collection<String> recent, Collection<String> flightDone,
                               Random random, boolean avoidRecent) {
        List<Kind> kinds = new ArrayList<>();
        List<List<String>> options = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (weights.of(kind) <= 0) continue;
            List<String> fresh = new ArrayList<>();
            for (String name : candidates(kind, s)) {
                if (avoidRecent && recent != null && recent.contains(name)) continue;
                if (kind == Kind.AERIAL && flightDone != null && flightDone.contains(name)) continue;
                fresh.add(name);
            }
            if (fresh.isEmpty()) continue;
            kinds.add(kind);
            options.add(fresh);
        }
        int total = 0;
        for (Kind kind : kinds) total += weights.of(kind);
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (int i = 0; i < kinds.size(); i++) {
            roll -= weights.of(kinds.get(i));
            if (roll < 0) {
                List<String> from = options.get(i);
                return from.get(random.nextInt(from.size()));
            }
        }
        return null;
    }

    /** Every attack of {@code kind} the Sentinel could throw in {@code s}. */
    static List<String> candidates(Kind kind, Situation s) {
        if (s.flying()) {
            return switch (kind) {
                case AERIAL -> s.distance() < 15 ? AERIAL_CLOSE : s.distance() < 35 ? AERIAL_MEDIUM : AERIAL_FAR;
                case RANGED -> RANGED;
                default -> List.of();
            };
        }
        if (s.sealed()) {
            return switch (kind) {
                case RANGED -> RANGED;
                case AEGIS_JUDGMENT -> s.shieldInHand() ? List.of(AEGIS_JUDGMENT) : List.of();
                case SUMMON -> s.summonFree() ? List.of(TRIANGLE_CALL) : List.of();
                default -> List.of();
            };
        }
        return switch (kind) {
            case MELEE -> s.distance() < 5 ? GROUND_CLOSE : s.distance() < 15 ? GROUND_MEDIUM : GROUND_FAR;
            case RANGED -> RANGED;
            case FLIGHT -> List.of(FLY_UP);
            case HOVER_BARRAGE -> List.of(HOVER_BARRAGE);
            case SHIELD_SEAL -> List.of(SHIELD_SEAL);
            case AEGIS_JUDGMENT -> s.shieldInHand() ? List.of(AEGIS_JUDGMENT) : List.of();
            case DEFENSE -> {
                if (s.health() >= DEFENSE_HEALTH) yield List.of();
                List<String> out = new ArrayList<>();
                if (s.defenseFree()) out.addAll(DEFENSE_STATES);
                if (s.healFree() && s.health() < HEAL_HEALTH) out.addAll(DEFENSE_HEALS);
                yield out;
            }
            case SUMMONING -> {
                List<String> out = new ArrayList<>();
                if (s.summonRoom()) out.addAll(SUMMONINGS);
                if (s.championReady()) out.add(CHAMPION_CALL);
                yield out;
            }
            case DESTRUCTIVE -> s.destructiveReady() ? DESTRUCTIVES : List.of();
            default -> List.of();
        };
    }

    /**
     * A random attack from {@code pool} that is not in {@code recent}; the whole pool when every
     * entry was used recently. Returns null only for an empty pool.
     */
    public static String pick(List<String> pool, Collection<String> recent, Random random) {
        if (pool == null || pool.isEmpty()) return null;
        List<String> fresh = new ArrayList<>(pool.size());
        for (String name : pool) {
            if (recent == null || !recent.contains(name)) fresh.add(name);
        }
        List<String> from = fresh.isEmpty() ? pool : fresh;
        return from.get(random.nextInt(from.size()));
    }

    /** Records an attack as the most recent one, forgetting the oldest beyond {@link #HISTORY}. */
    public static void remember(Deque<String> recent, String name) {
        if (recent == null || name == null) return;
        recent.remove(name);
        recent.addLast(name);
        while (recent.size() > HISTORY) {
            recent.removeFirst();
        }
    }
}
