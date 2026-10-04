package com.Chagui68.commands;

import java.util.List;

/**
 * Data behind {@code /msc attack}: the attacks documented in the help menu and offered by tab
 * completion.
 *
 * <p>The names are the keys accepted by {@code ArmorStandBoss#triggerAttack}, so a new attack is
 * added here once and both the help pages and the tab completion pick it up. Descriptions keep
 * their {@code &} colour codes; {@link CommandMenu} translates them when rendering.
 */
final class AttackCatalogue {

    /** One documented attack: its command name, its help description and the page it shows on. */
    record Entry(String name, String description, int page) {}

    static final List<String> PAGE_TITLES = List.of(
            "GROUND ATTACKS & EARTH CONTROL",
            "AERIAL ASSAULTS & CELESTIAL RUSHES",
            "RANGED ARTILLERY & MAGIC PROJECTIONS",
            "DEFENSIVE SHIELDS & MAGIC SEALS",
            "SUMMONING RITES & CHAMPIONS",
            "DESTRUCTIVE CATACLYSMS");

    private static final List<Entry> ENTRIES = List.of(
            // Ground
            new Entry("groundslam", "&7Shield hurled skyward to judge every player below", 1),
            new Entry("groundshatter", "&7Fissure wave that fractures the terrain", 1),
            new Entry("shieldbash", "&7Forward heavy rush that stuns targets", 1),
            new Entry("lancestorm", "&7Piercing lance barrage across the ground", 1),
            new Entry("earthpillar", "&7Stone pillars erupting from beneath foes", 1),
            new Entry("chaingrapple", "&7Iron chain hook pulling players in", 1),
            new Entry("warstomp", "&7Massive area shockwave knocking entities back", 1),
            new Entry("armorspikes", "&7Defensive spike retribution burst", 1),
            new Entry("vortexpull", "&7Gravitational vortex dragging entities to center", 1),
            new Entry("mirrorimage", "&7Illusionary decoys to confuse adversaries", 1),
            new Entry("doombeam", "&7Focused demonic ground laser sweep", 1),
            new Entry("lanceflurry", "&7Three rapid lance thrusts in a frontal cone", 1),
            new Entry("whirlwindslash", "&7Spinning sweep that drags foes in, then a finishing cut", 1),
            new Entry("executionsweep", "&7Devastating wide-arc executioner strike", 1),
            new Entry("obsidianspire", "&7Line of volcanic pillars shattering the ground ahead", 1),
            new Entry("earthmaw", "&7Stone jaws closing on everything in front", 1),
            new Entry("shadowstep", "&7Vanishes through a sigil to strike from behind", 1),
            new Entry("runeward", "&7Plants a pulsing rune ward that outlives the cast", 1),
            new Entry("sunderingcharge", "&7Spear-dragging charge that leaves an erupting fissure", 1),
            new Entry("spearcyclone", "&7Whirled spear unleashes a roaming cyclone", 1),
            new Entry("cataclysm", "&7Three rings of the arena erupt from the inside out", 1),
            new Entry("tremorlance", "&7Three spear blows, three widening shockwaves to jump", 1),
            new Entry("aegisrush", "&7Shield-first charge flinging everyone in its lane aside", 1),
            new Entry("gravecleaver", "&7Overhead cleave splitting a fissure of obsidian shards", 1),
            // Aerial
            new Entry("starfall", "&7Calling celestial stars crashing down", 2),
            new Entry("aerialrush", "&7High-speed aerial homing strike", 2),
            new Entry("sonicboom", "&7Acoustic blast wave penetrating defenses", 2),
            new Entry("lightningstorm", "&7Summoning consecutive lightning strikes", 2),
            new Entry("gravitywell", "&7Aerial singularity pulling upwards", 2),
            new Entry("crossslash", "&7Dual aerial sword cleave in cross shape", 2),
            new Entry("novaburst", "&7Explosive radiant detonation in midair", 2),
            new Entry("darkorb", "&7Floating orb radiating darkness damage", 2),
            new Entry("windcutter", "&7Slicing razor-wind blades", 2),
            new Entry("heavenlyjudgment", "&7Holy orbital beam strike", 2),
            new Entry("rainoflances", "&7Shower of holy lances from the sky", 2),
            new Entry("airslam", "&7Sky-dive slam pulverizing the landing zone", 2),
            new Entry("hoverbarrage", "&7Levitating volley of energy projectiles", 2),
            new Entry("eclipsefall", "&7Black eclipse disc dropped on the landing zone", 2),
            new Entry("bladering", "&7Orbiting lance ring fired out one by one", 2),
            new Entry("obsidianwings", "&7Wing beats sweeping obsidian shards outward", 2),
            new Entry("voidmeteor", "&7Obsidian meteor hurled down into a void crater", 2),
            new Entry("phantomlegion", "&7Spectral copies lunge through the target one by one", 2),
            new Entry("spiralstorm", "&7Lances raining down a spiral wound around the target", 2),
            new Entry("chainhook", "&7Hooked chains that drag whoever stays on the mark", 2),
            // Ranged / magic
            new Entry("lancesnipe", "&7High-velocity sniper lance projectile", 3),
            new Entry("meteorstorm", "&7Shower of flaming meteorites", 3),
            new Entry("voidbeam", "&7Linear void disintegration laser", 3),
            new Entry("frostlance", "&7Piercing glacial spear inflicting deep freeze", 3),
            new Entry("lightningspear", "&7Electrified javelin shocking targets", 3),
            new Entry("shadowvolley", "&7Multi-directional flurry of dark arrows", 3),
            new Entry("chainlightning", "&7Electric arc bouncing between nearby players", 3),
            new Entry("crystalbarrage", "&7Rapid crystal shards barrage", 3),
            new Entry("arcaneorb", "&7Pulsing magical sphere of pure arcane power", 3),
            new Entry("voidrift", "&7Dimensional tear distorting spacetime", 3),
            new Entry("arcanemissiles", "&7Homing arcane bolts seeking players", 3),
            new Entry("spiritbeam", "&7Piercing spectral light beam", 3),
            new Entry("soultethers", "&7Visible tethers hook players and reel them in", 3),
            new Entry("plaguebrand", "&7Brands a player with a plague that spreads", 3),
            new Entry("runemines", "&7Scatters armed runes that burst when stepped on", 3),
            new Entry("obsidianprison", "&7Cages of obsidian spikes collapse on each player", 3),
            new Entry("shardburst", "&7Fan of nine obsidian shards punched from the shield", 3),
            new Entry("gravityorb", "&7Slow void sphere that drags players in, then implodes", 3),
            new Entry("javelinvolley", "&7Three javelins thrown where the target is going", 3),
            new Entry("sweepinglaser", "&7Knee-high red beam sweeping a 120-degree arc", 3),
            // Defensive
            new Entry("stoneskin", "&7Hardens boss defense, reducing all damage", 4),
            new Entry("reflectbarrier", "&7Prismatic shield reflecting projectiles", 4),
            new Entry("absorbshield", "&7Barrier converting incoming damage into healing", 4),
            new Entry("shieldseal", "&7Protective ancient ward preventing melee strikes", 4),
            new Entry("healingcircle", "&7Radiant circle regenerating boss vitality", 4),
            new Entry("trianglecall", "&7Sacred geometric barrier summoning reinforcements", 4),
            new Entry("regeneration", "&7Orbiting obsidian shards mending the boss as it fights", 4),
            new Entry("soulsiphon", "&7Soul tethers draining nearby players to heal the boss", 4),
            new Entry("obsidiancocoon", "&7Invulnerable obsidian shell that heals, then bursts", 4),
            new Entry("bulwark", "&7Braced behind ramparts: a third of the damage, but rooted", 4),
            new Entry("thornaura", "&7Thorns that sting attackers and prick anyone close", 4),
            new Entry("afterimage", "&7Ghost copies that make a third of all hits miss", 4),
            // Summoning
            new Entry("lancesquires", "&7Two obsidian squires that lunge with their lances", 5),
            new Entry("obsidianmender", "&7Acolyte beaming life into the boss until it dies", 5),
            new Entry("emberhounds", "&7Pack of three burning wolves that set you alight", 5),
            new Entry("voidwisps", "&7Three void wisps that drift through walls and burst", 5),
            new Entry("obsidianbrute", "&7Twice-sized brute slamming the ground every few seconds", 5),
            new Entry("elementalconclave", "&7Flame Elemental, Frost Golem and Storm Caller at once", 5),
            new Entry("shadowambush", "&7Shadow Rogues and Void Crawlers rising around the target", 5),
            new Entry("necropolisrite", "&7A Soul Reaper flanked by two Bone Shields", 5),
            new Entry("arcanecovenant", "&7Chaos Mage, Venom Witch and Ender Knight together", 5),
            new Entry("championcall", "&7Once per phase: a random boss of the multiverse joins", 5),
            // Destructive
            new Entry("orbitalstrike", "&7Targeting grid, lock on, a red dome from orbit", 6),
            new Entry("meteorimpact", "&7House-sized meteor falling for five seconds", 6),
            new Entry("supernova", "&7A star swells and explodes; only its eye is safe", 6),
            new Entry("judgmentpillars", "&7Columns of light firing across the arena", 6),
            new Entry("earthsplitter", "&7A giant cross of fissures torn thirty blocks out", 6),
            new Entry("voidcollapse", "&7A black hole drags everyone in, then collapses", 6),
            new Entry("obsidiantsunami", "&7A wall of obsidian rolls over the arena; find the gap", 6),
            new Entry("solarlance", "&7A fourteen-block spear of sunlight hurled at you", 6),
            new Entry("worldbreaker", "&7Leaps into the sky and lands with three shockwaves", 6),
            new Entry("apocalypserain", "&7The sky turns red and meteors rain for five seconds", 6));

    private AttackCatalogue() {}

    static List<Entry> entries() {
        return ENTRIES;
    }

    static int pages() {
        return PAGE_TITLES.size();
    }

    /** Header shown by {@code /msc attack help <page>}; pages are 1-based and pre-clamped. */
    static String pageTitle(int page) {
        return PAGE_TITLES.get(page - 1);
    }

    /** The rendered help lines of one page, in declaration order. */
    static List<String> helpLines(int page) {
        return ENTRIES.stream()
                .filter(entry -> entry.page() == page)
                .map(AttackCatalogue::helpLine)
                .toList();
    }

    static String helpLine(Entry entry) {
        return " &e• " + entry.name() + " &8- " + entry.description();
    }

    /** Every documented attack name, in help order — the source of tab completion. */
    static List<String> names() {
        return ENTRIES.stream().map(Entry::name).toList();
    }
}
