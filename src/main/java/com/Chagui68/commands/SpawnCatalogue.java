package com.Chagui68.commands;

import com.Chagui68.MultiverseCreatures;
import com.Chagui68.entities.handler.MobHandler;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data behind {@code /msc spawn}: every entity the command can spawn, the one name it answers to, the
 * success and failure messages and the help entry.
 *
 * <p>The command used to be a ~200 line {@code switch} where the name list, the message text and
 * the help menu were three separate hardcoded copies of the same information. They now live in one
 * table: adding an entity means adding one {@link Type} here.
 */
final class SpawnCatalogue {

    /** Spawns one entity kind; {@code false} means the entity's own limits refused the spawn. */
    @FunctionalInterface
    interface Spawner {
        boolean spawn(MultiverseCreatures plugin, MobHandler mobHandler, Location location);
    }

    /**
     * One spawnable kind.
     *
     * @param id              the only name the command accepts, shown by the help and tab completion
     * @param spawnedName     completes "Spawned X!" on success
     * @param failureName     completes "Failed to spawn X." on failure
     * @param helpDescription help-menu description, colours included; {@code null} when undocumented
     * @param helpPage        1-based help page, or 0 to stay out of the menu
     * @param spawner         the bridge into the entity manager
     */
    record Type(String id, String spawnedName, String failureName,
                String helpDescription, int helpPage, Spawner spawner) {
        boolean documented() {
            return helpPage > 0;
        }
    }

    static final List<String> PAGE_TITLES = List.of(
            "BOSSES & APEX ENTITIES",
            "MILITARY STRIKE FORCE",
            "MULTIVERSE CREATURES & ELITES");

    private static final List<Type> TYPES = List.of(
            // ----- Bosses & apex entities -----
            new Type("nix",
                    "NIX - The Executioner", "NIX - The Executioner",
                    "&cNIX - The Executioner &7(Scaffold Ritual Boss)", 1,
                    (plugin, mobs, location) -> plugin.getNixBoss().trySpawn(location)),
            new Type("dio",
                    "DIO", "DIO",
                    "&6DIO &7(The World — JoJo's Bizarre Adventure)", 1,
                    (plugin, mobs, location) -> plugin.getDioBoss().trySpawn(location)),
            new Type("witherstorm",
                    "Wither Storm", "Wither Storm",
                    "&5Wither Storm &7(Cracker's Wither Storm Mod)", 1,
                    (plugin, mobs, location) -> plugin.getWitherStormBoss().trySpawn(location)),
            new Type("armorstand",
                    "ArmorStand Boss", "ArmorStand Boss",
                    "&6The Ancient Armor Stand &7(Multiverse Boss)", 1,
                    (plugin, mobs, location) -> plugin.getArmorStandBoss().trySpawn(location)),
            new Type("mahoraga",
                    "Mahoraga", "Mahoraga",
                    "&fMahoraga &7(Adapting Divine General)", 1,
                    (plugin, mobs, location) -> plugin.getMahoraga().trySpawn(location)),
            new Type("garou",
                    "Garou [Hero Hunter]", "Garou",
                    "&bGarou &7(Martial Arts Miniboss)", 1,
                    (plugin, mobs, location) -> plugin.getGarouBoss().trySpawn(location)),
            new Type("kinger",
                    "Kinger", "Kinger",
                    "&5Kinger &7(The Chess King)", 1,
                    (plugin, mobs, location) -> plugin.getKinger().trySpawn(location)),
            new Type("disctrader",
                    "Disc Trader", "Disc Trader",
                    "&dDisc Trader &7(Music & Relic Merchant)", 1,
                    (plugin, mobs, location) -> plugin.getDiscTrader().trySpawn(location)),
            // ----- Military strike force -----
            new Type("zombietrap",
                    "Military Zombie Horse trap", "trap",
                    "&cHorse Trap Trigger &7(Spawns Military Army)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawn(location)),
            new Type("tank",
                    "Zombie Tank", "Zombie Tank",
                    "&aZombie Tank &7(Heavy Armor, Shield & Slam)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawnTank(location)),
            new Type("duelist",
                    "Military Skeleton Duelist", "Duelist",
                    "&dMilitary Duelist &7(Dynamic Sword/Bow Swap)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawnDuelist(location)),
            new Type("lancer",
                    "Zombie Lancer on horse", "Lancer",
                    "&eZombie Lancer &7(Charging Cavalry Rider)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawnLancer(location)),
            new Type("camel",
                    "Camel with riders", "Camel",
                    "&6Siege Camels &7(Husk Cavalry & Archers)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawnCamel(location)),
            new Type("sniper",
                    "Sniper Skeleton", "Sniper",
                    "&8Wither Sniper &7(High-velocity Bow Snipes)", 2,
                    (plugin, mobs, location) -> plugin.getZombieHorseTrap().trySpawnSniper(location)),
            // ----- Multiverse creatures & elites -----
            new Type("chaosmage",
                    "Chaos Mage", "Chaos Mage",
                    "&dChaos Mage &7(Magic Bolts & Blink)", 3,
                    (plugin, mobs, location) -> plugin.getChaosMage().trySpawn(location)),
            new Type("soulreaper",
                    "Soul Reaper", "Soul Reaper",
                    "&cSoul Reaper &7(Scythe Life Drain)", 3,
                    (plugin, mobs, location) -> plugin.getSoulReaper().trySpawn(location)),
            new Type("enderknight",
                    "Ender Knight", "Ender Knight",
                    "&5Ender Knight &7(Abyssal Void Blade)", 3,
                    (plugin, mobs, location) -> plugin.getEnderKnight().trySpawn(location)),
            new Type("obsidianguard",
                    "Obsidian Guard", "Obsidian Guard",
                    "&8Obsidian Guard &7(Reinforced Shield)", 3,
                    (plugin, mobs, location) -> plugin.getObsidianGuard().trySpawn(location)),
            new Type("stormcaller",
                    "Storm Caller", "Storm Caller",
                    "&bStorm Caller &7(Lightning Summoner)", 3,
                    (plugin, mobs, location) -> plugin.getStormCaller().trySpawn(location)),
            new Type("frostgolem",
                    "Frost Golem", "Frost Golem",
                    "&9Frost Golem &7(Cryo Slow Aura)", 3,
                    (plugin, mobs, location) -> plugin.getFrostGolem().trySpawn(location)),
            new Type("flameelemental",
                    "Flame Elemental", "Flame Elemental",
                    "&6Flame Elemental &7(Fire Nova)", 3,
                    (plugin, mobs, location) -> plugin.getFlameElemental().trySpawn(location)),
            new Type("shadowrogue",
                    "Shadow Rogue", "Shadow Rogue",
                    "&8Shadow Rogue &7(Stealth Infiltrator)", 3,
                    (plugin, mobs, location) -> plugin.getShadowRogue().trySpawn(location)),
            new Type("voidcrawler",
                    "Void Crawler", "Void Crawler",
                    "&5Void Crawler &7(End Abyss Parasite)", 3,
                    (plugin, mobs, location) -> plugin.getVoidCrawler().trySpawn(location)),
            new Type("boneshield",
                    "Bone Shield", "Bone Shield",
                    "&fBone Shield Skeleton &7(Arrow Defense)", 3,
                    (plugin, mobs, location) -> plugin.getBoneShield().trySpawn(location)),
            new Type("arrowskeleton",
                    "Arrow Skeleton", "Arrow Skeleton",
                    "&6Archer of the Arrow &7(Stand Arrow)", 3,
                    (plugin, mobs, location) -> plugin.getArrowSkeleton() != null
                            && plugin.getArrowSkeleton().trySpawn(location)),
            new Type("venomwitch",
                    "Venom Witch", "Venom Witch",
                    "&2Venom Witch &7(Toxic Splash Potions)", 3,
                    (plugin, mobs, location) -> plugin.getVenomWitch().trySpawn(location)),
            new Type("warlord",
                    "Warlord", "Warlord",
                    "&4Orcish Warlord &7(Berserker Rage)", 3,
                    (plugin, mobs, location) -> plugin.getWarlord().trySpawn(location)),
            new Type("creeperjr",
                    "Creeper Jr.", "Creeper Jr.",
                    "&aCreeper Jr. &7(Fast Micro-Exploder)", 3,
                    (plugin, mobs, location) -> plugin.getCreeperJr().trySpawn(location)),
            new Type("headslime",
                    "Head Slime", "Head Slime",
                    "&aHead Slime &7(Leaping Parasite)", 3,
                    (plugin, mobs, location) -> plugin.getHeadSlime().trySpawn(location)),
            new Type("merchant",
                    "Multiverse Merchant", "Multiverse Merchant",
                    "&eMultiverse Merchant &7(Custom Trades)", 3,
                    (plugin, mobs, location) -> {
                        mobs.spawnShaggy(location);
                        return true;
                    }),
            // ----- Spawnable but kept out of the help menu (unchanged behaviour) -----
            new Type("witherstorm2",
                    "Wither Storm (Growing Hunchback)", "Wither Storm",
                    null, 0,
                    (plugin, mobs, location) -> plugin.getWitherStormBoss().trySpawn(location,
                            com.Chagui68.entities.boss.witherstorm.WitherStormForm.GROWING)),
            new Type("witherstorm3",
                    "Wither Storm (Swollen Hunchback)", "Wither Storm",
                    null, 0,
                    (plugin, mobs, location) -> plugin.getWitherStormBoss().trySpawn(location,
                            com.Chagui68.entities.boss.witherstorm.WitherStormForm.PREGNANT)),
            new Type("witherstorm4",
                    "Wither Storm (Destroyer)", "Wither Storm",
                    null, 0,
                    (plugin, mobs, location) -> plugin.getWitherStormBoss().trySpawn(location,
                            com.Chagui68.entities.boss.witherstorm.WitherStormForm.DESTROYER)),
            new Type("witherstorm5",
                    "Wither Storm (Devourer)", "Wither Storm",
                    null, 0,
                    (plugin, mobs, location) -> plugin.getWitherStormBoss().trySpawn(location,
                            com.Chagui68.entities.boss.witherstorm.WitherStormForm.DEVOURER)),
            new Type("jack",
                    "JackStar — The System Architect", "JackStar Boss",
                    null, 0,
                    (plugin, mobs, location) -> plugin.getJackStarBoss().trySpawn(location)));

    private static final Map<String, Type> BY_NAME = buildIndex();

    private SpawnCatalogue() {}

    private static Map<String, Type> buildIndex() {
        Map<String, Type> index = new LinkedHashMap<>();
        for (Type type : TYPES) {
            if (type.id() == null || type.id().isBlank()) {
                throw new IllegalStateException("Spawn catalogue name must not be blank");
            }
            Type previous = index.put(type.id().toLowerCase(), type);
            if (previous != null) {
                throw new IllegalStateException("Duplicate spawn name '" + type.id() + "'");
            }
        }
        return Map.copyOf(index);
    }

    /** Resolves a name (case-insensitive) to its type, or {@code null} when nothing matches. */
    static Type find(String name) {
        if (name == null) return null;
        return BY_NAME.get(name.toLowerCase());
    }

    /** Runs the spawner of one type; callers decide what to do with the boolean result. */
    static boolean spawn(MultiverseCreatures plugin, MobHandler mobHandler, Type type, Location location) {
        return type.spawner().spawn(plugin, mobHandler, location);
    }

    static List<Type> types() {
        return TYPES;
    }

    static int pages() {
        return PAGE_TITLES.size();
    }

    static String pageTitle(int page) {
        return PAGE_TITLES.get(page - 1);
    }

    /** The rendered help lines of one page, in declaration order. */
    static List<String> helpLines(int page) {
        List<String> lines = new ArrayList<>();
        for (Type type : TYPES) {
            if (!type.documented() || type.helpPage() != page) continue;
            lines.add(" &e• " + type.id() + " &8- " + type.helpDescription());
        }
        return lines;
    }

    /** The name of every type, in declaration order — the source of tab completion. */
    static List<String> names() {
        List<String> all = new ArrayList<>();
        for (Type type : TYPES) {
            all.add(type.id());
        }
        return all;
    }
}
