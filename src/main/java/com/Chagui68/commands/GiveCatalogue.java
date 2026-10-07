package com.Chagui68.commands;

import com.Chagui68.items.armor.EightHandledWheel;
import com.Chagui68.items.armor.ObsidianBastion;
import com.Chagui68.items.components.ArchitectKernel;
import com.Chagui68.items.components.BoneMarrow;
import com.Chagui68.items.components.ChaosCore;
import com.Chagui68.items.components.ChaosFragment;
import com.Chagui68.items.components.ChaosOrb;
import com.Chagui68.items.components.ChaosPowder;
import com.Chagui68.items.components.CompressedGoldBlock;
import com.Chagui68.items.components.CondensedChaosOrb;
import com.Chagui68.items.components.EnderCore;
import com.Chagui68.items.components.EnderFragment;
import com.Chagui68.items.components.ExecutionerWarrant;
import com.Chagui68.items.components.FrostHeart;
import com.Chagui68.items.components.HeadSlimeHeart;
import com.Chagui68.items.components.MagmaCore;
import com.Chagui68.items.components.MilitaryComponent;
import com.Chagui68.items.components.MoltenMarrow;
import com.Chagui68.items.components.MoltenNetherite;
import com.Chagui68.items.components.MoltenWheelCore;
import com.Chagui68.items.components.MultiversalCore;
import com.Chagui68.items.components.ObsidianShard;
import com.Chagui68.items.components.OssifiedPlate;
import com.Chagui68.items.components.ReaperCore;
import com.Chagui68.items.components.ReaperEssence;
import com.Chagui68.items.components.RefinedNetherite;
import com.Chagui68.items.components.RefinedWheelCore;
import com.Chagui68.items.components.ReinforcedBone;
import com.Chagui68.items.components.ReinforcedBoneBlock;
import com.Chagui68.items.components.SentinelCore;
import com.Chagui68.items.components.ShadowCloak;
import com.Chagui68.items.components.StarCore;
import com.Chagui68.items.components.StormCrystal;
import com.Chagui68.items.components.SwordMold;
import com.Chagui68.items.components.VenomGland;
import com.Chagui68.items.components.VoidEssence;
import com.Chagui68.items.components.WheelCore;
import com.Chagui68.items.components.WheelEssence;
import com.Chagui68.items.food.HeadSlimeGelatin;
import com.Chagui68.items.food.ScoobyCookie;
import com.Chagui68.items.misc.IceCrown;
import com.Chagui68.items.misc.MantisClaws;
import com.Chagui68.items.misc.MilitaryMine;
import com.Chagui68.items.misc.WirtsLantern;
import com.Chagui68.items.misc.offhand.FrostHeartOffhand;
import com.Chagui68.items.misc.offhand.MarrowAegis;
import com.Chagui68.items.misc.offhand.VeilwalkerMantle;
import com.Chagui68.items.weapons.magic.ChaosForge;
import com.Chagui68.items.weapons.magic.SentinelGrimoire;
import com.Chagui68.items.weapons.magic.SkyfireTalisman;
import com.Chagui68.items.weapons.melee.CinderGreatsword;
import com.Chagui68.items.weapons.melee.Excalibur;
import com.Chagui68.items.weapons.melee.NullshearEdge;
import com.Chagui68.items.weapons.melee.SoulreapScythe;
import com.Chagui68.items.weapons.melee.Venomfang;
import com.Chagui68.items.weapons.ranged.AetherPullshot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Data behind {@code /msc give}: the one name each custom item answers to, plus the text of the help
 * pages.
 *
 * <p>The command used to hold a ~60 case switch, a separate 20 line tab-completion list and four
 * pages of help lines, all describing the same items. The name table is now the single source of
 * truth for giving and for tab completion, and a unit test checks that every item named in the help
 * text actually resolves.
 */
final class GiveCatalogue {

    /**
     * One custom item.
     *
     * @param name     the only name the command accepts, as the help shows it
     * @param template item factory — kept lazy so loading this catalogue never initialises item
     *                 classes (they read Bukkit's item meta) until an item is really requested
     */
    record Entry(String name, Supplier<ItemStack> template) {}

    static final List<String> PAGE_TITLES = List.of(
            "LEGENDARY WEAPONS & MAGIC",
            "ARMOR SETS, RELICS & OFFHANDS",
            "BOSS CATALYSTS & APEX COMPONENTS",
            "CRAFTING MATERIALS & ESSENCES");

    /** Help bodies (everything after the "&e• ") for page 1; grouped lines are allowed. */
    private static final List<String> PAGE_1 = List.of(
            "excalibur &8- &6Holy Blade of Kings",
            "cindergreatsword &8- &cBlazing Heavy Greatsword",
            "nullshearedge &8- &5Void Spatial Slicer",
            "soulreapscythe &8- &8Life-draining Scythe",
            "venomfang &8- &2Poison-tipped Dagger",
            "aetherpullshot &8- &bGravitational Pull Bow",
            "chaosforge &8- &dChaos Casting Hammer",
            "skyfiretalisman &8- &6Celestial Skyfire Charm",
            "sentinelgrimoire &8- &9Guardian Spell Grimoire",
            "executionerguillotine &8- &4NIX Executioner Axe",
            "architectdeployer &8- &bJackStar Data Bow");

    private static final List<String> PAGE_2 = List.of(
            "eighthandledwheel &8- &fMahoraga's Sacred Wheel",
            "obsidianbastionhelmet &8- &8Obsidian Bastion Helmet",
            "obsidianbastionchestplate &8- &8Obsidian Bastion Chestplate",
            "obsidianbastionleggings &8- &8Obsidian Bastion Leggings",
            "obsidianbastionboots &8- &8Obsidian Bastion Boots",
            "icecrown &8- &bGlacial Monarch Crown",
            "wirtslantern &8- &6Illuminating Explorer Lantern",
            "mantisclaws &8- &aPreying Mantis Dual Claws",
            "militarymine &8- &cProximity Landmine",
            "frostheartoffhand &8- &9Cryo Shield Offhand",
            "marrowaegis &8- &fBone Marrow Aegis Shield",
            "veilwalkermantle &8- &5Shadow Veilwalker Cloak");

    private static final List<String> PAGE_3 = List.of(
            "executionerwarrant &8- &4NIX Scaffold Summon Warrant",
            "compressedgoldblock &8- &6Boss Altar Anchor Block",
            "multiversalcore &8- &dMultiverse Nexus Core",
            "wheelcore &8- &fDivergent Sila Wheel Core",
            "moltenwheelcore &8- &cMolten Infused Wheel Core",
            "refinedwheelcore &8- &bPurified Wheel Core",
            "reapercore &8- &8Soul Reaper Core",
            "sentinelcore &8- &9Ancient Sentinel Core",
            "endercore &8- &5End Void Dimensional Core",
            "starcore &8- &eCelestial Star Core",
            "chaoscore &8- &dRaw Concentrated Chaos Core",
            "scoobycookie &8- &6Mystery Scooby Snack (Food)",
            "executioneredge &8- &4NIX Axe Shard",
            "vampireblood &8/ &eunstableblood &8/ &ebearerelixir &8- &4DIO Blood & Elixir");

    private static final List<String> PAGE_4 = List.of(
            "reaperessence &8/ &evoidessence &8/ &ewheelessence &8- &7Essences",
            "stormcrystal &8/ &emagmacore &8/ &efrostheart &8- &7Elemental Cores",
            "obsidianshard &8/ &erefinednetherite &8/ &emoltennetherite &8- &7Metals",
            "headslimeheart &8/ &eheadslimegelatin &8/ &evenomgland &8- &7Organics",
            "shadowcloak &8/ &eswordmold &8/ &emilitarycomponent &8- &7Relics",
            "reinforcedbone &8/ &ereinforcedboneblock &8- &7Bone Crafting",
            "chaosorb &8/ &echaospowder &8/ &echaosfragment &8- &7Chaos Alch.",
            "condensedchaosorb &8/ &eenderfragment &8- &7Infused Catalysts");

    private static final List<List<String>> PAGES = List.of(PAGE_1, PAGE_2, PAGE_3, PAGE_4);

    private static final List<Entry> ENTRIES = List.of(
            // Food & cosmetic
            new Entry("scoobycookie", () -> ScoobyCookie.SCOOBY_COOKIE),
            new Entry("icecrown", () -> IceCrown.ICE_CROWN),
            new Entry("wirtslantern", () -> WirtsLantern.WIRTS_LANTERN),
            new Entry("starcore", () -> StarCore.STAR_CORE),
            // Stand Arrow arc: DIO, NIX and Jack Star
            new Entry("vampireblood",
                    () -> com.Chagui68.items.components.VampireBlood.VAMPIRE_BLOOD),
            new Entry("unstableblood", () -> com.Chagui68.items.potions.VampirePotions.UNSTABLE_BLOOD),
            new Entry("bearerelixir",
                    () -> com.Chagui68.items.potions.VampirePotions.BEARER_ELIXIR),
            new Entry("executioneredge",
                    () -> com.Chagui68.items.components.ExecutionerEdge.EXECUTIONER_EDGE),
            new Entry("executionerguillotine",
                    () -> com.Chagui68.items.weapons.melee.ExecutionerGuillotine.EXECUTIONER_GUILLOTINE),
            new Entry("architectdeployer",
                    () -> com.Chagui68.items.weapons.ranged.ArchitectDeployer.ARCHITECT_DEPLOYER),
            // Weapons
            new Entry("excalibur", () -> Excalibur.EXCALIBUR_SWORD),
            new Entry("aetherpullshot", () -> AetherPullshot.AETHER_PULLSHOT),
            new Entry("chaosforge", () -> ChaosForge.CHAOS_FORGE),
            new Entry("cindergreatsword", () -> CinderGreatsword.CINDER_GREATSWORD),
            new Entry("nullshearedge", () -> NullshearEdge.NULLSHEAR_EDGE),
            new Entry("soulreapscythe", () -> SoulreapScythe.SOULREAP_SCYTHE),
            new Entry("venomfang", () -> Venomfang.VENOMFANG),
            new Entry("skyfiretalisman", () -> SkyfireTalisman.SKYFIRE_TALISMAN),
            new Entry("sentinelgrimoire", () -> SentinelGrimoire.GRIMOIRE),
            new Entry("swordmold", () -> SwordMold.SWORD_MOLD),
            new Entry("mantisclaws", () -> MantisClaws.MANTIS_CLAWS_ITEM),
            new Entry("militarymine", () -> MilitaryMine.MILITARY_MINE),
            // Armor
            new Entry("eighthandledwheel", () -> EightHandledWheel.EIGHT_HANDLED_WHEEL),
            new Entry("obsidianbastionhelmet", () -> ObsidianBastion.HELMET),
            new Entry("obsidianbastionchestplate", () -> ObsidianBastion.CHESTPLATE),
            new Entry("obsidianbastionleggings", () -> ObsidianBastion.LEGGINGS),
            new Entry("obsidianbastionboots", () -> ObsidianBastion.BOOTS),
            // Off-hand misc
            new Entry("frostheartoffhand", () -> FrostHeartOffhand.FROST_HEART_OFFHAND),
            new Entry("marrowaegis", () -> MarrowAegis.MARROW_AEGIS),
            new Entry("veilwalkermantle", () -> VeilwalkerMantle.VEILWALKER_MANTLE),
            new Entry("militarycomponent", () -> MilitaryComponent.MILITARY_COMPONENT),
            new Entry("shadowcloak", () -> ShadowCloak.SHADOW_CLOAK),
            // Boss catalysts & apex components
            new Entry("executionerwarrant",
                    () -> ExecutionerWarrant.EXECUTIONER_WARRANT),
            new Entry("architectkernel", () -> ArchitectKernel.ARCHITECT_KERNEL),
            new Entry("garoucosmiccore",
                    () -> com.Chagui68.items.components.GarouCosmicCore.GAROU_COSMIC_CORE),
            new Entry("multiversalcore", () -> MultiversalCore.MULTIVERSAL_CORE),
            new Entry("compressedgoldblock", () -> CompressedGoldBlock.COMPRESSED_GOLD_BLOCK),
            new Entry("wheelcore", () -> WheelCore.WHEEL_CORE),
            new Entry("moltenwheelcore", () -> MoltenWheelCore.MOLTEN_WHEEL_CORE),
            new Entry("refinedwheelcore", () -> RefinedWheelCore.REFINED_WHEEL_CORE),
            new Entry("reapercore", () -> ReaperCore.REAPER_CORE),
            new Entry("sentinelcore", () -> SentinelCore.SENTINEL_CORE),
            new Entry("endercore", () -> EnderCore.ENDER_CORE),
            new Entry("chaoscore", () -> ChaosCore.CHAOS_CORE),
            // Crafting materials & essences
            new Entry("headslimeheart", () -> HeadSlimeHeart.HEAD_SLIME_HEART),
            new Entry("headslimegelatin", () -> HeadSlimeGelatin.HEAD_SLIME_GELATIN),
            new Entry("chaosorb", () -> ChaosOrb.CHAOS_ORB),
            new Entry("chaospowder", () -> ChaosPowder.CHAOS_POWDER),
            new Entry("chaosfragment", () -> ChaosFragment.CHAOS_FRAGMENT),
            new Entry("condensedchaosorb", () -> CondensedChaosOrb.CONDENSED_CHAOS_ORB),
            new Entry("enderfragment", () -> EnderFragment.ENDER_FRAGMENT),
            new Entry("frostheart", () -> FrostHeart.FROST_HEART),
            new Entry("magmacore", () -> MagmaCore.MAGMA_CORE),
            new Entry("obsidianshard", () -> ObsidianShard.OBSIDIAN_SHARD),
            new Entry("reaperessence", () -> ReaperEssence.REAPER_ESSENCE),
            new Entry("reinforcedbone", () -> ReinforcedBone.REINFORCED_BONE),
            new Entry("reinforcedboneblock", () -> ReinforcedBoneBlock.REINFORCED_BONE_BLOCK),
            new Entry("bonemarrow", () -> BoneMarrow.BONE_MARROW),
            new Entry("ossifiedplate", () -> OssifiedPlate.OSSIFIED_PLATE),
            new Entry("moltenmarrow", () -> MoltenMarrow.MOLTEN_MARROW),
            new Entry("stormcrystal", () -> StormCrystal.STORM_CRYSTAL),
            new Entry("venomgland", () -> VenomGland.VENOM_GLAND),
            new Entry("voidessence", () -> VoidEssence.VOID_ESSENCE),
            new Entry("wheelessence", () -> WheelEssence.WHEEL_ESSENCE),
            new Entry("refinednetherite", () -> RefinedNetherite.REFINED_NETHERITE),
            new Entry("moltennetherite", () -> MoltenNetherite.MOLTEN_NETHERITE));

    private static final Map<String, Entry> BY_NAME = buildIndex();

    private GiveCatalogue() {}

    private static Map<String, Entry> buildIndex() {
        Map<String, Entry> index = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            if (entry.name() == null || entry.name().isBlank()) {
                throw new IllegalStateException("Give catalogue name must not be blank");
            }
            if (index.put(entry.name().toLowerCase(), entry) != null) {
                throw new IllegalStateException("Duplicate give name '" + entry.name() + "'");
            }
        }
        return Map.copyOf(index);
    }

    /**
     * Builds a fresh copy of the item registered under {@code name}, or {@code null} when the
     * name is unknown. Callers own the returned stack.
     */
    static ItemStack find(String name) {
        if (name == null) return null;
        Entry entry = BY_NAME.get(name.toLowerCase());
        return entry == null ? null : entry.template().get().clone();
    }

    static List<Entry> entries() {
        return ENTRIES;
    }

    static int pages() {
        return PAGES.size();
    }

    static String pageTitle(int page) {
        return PAGE_TITLES.get(page - 1);
    }

    /** The rendered help bodies of one page, in declaration order. */
    static List<String> helpLines(int page) {
        return PAGES.get(page - 1);
    }

    /** The name of every entry, in declaration order — the source of tab completion. */
    static List<String> names() {
        List<String> all = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            all.add(entry.name());
        }
        return all;
    }
}
