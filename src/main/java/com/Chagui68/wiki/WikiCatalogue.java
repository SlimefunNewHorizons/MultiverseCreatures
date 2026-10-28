package com.Chagui68.wiki;

import com.Chagui68.items.armor.EightHandledWheel;
import com.Chagui68.items.armor.ObsidianBastion;
import com.Chagui68.items.armor.Worldbinder;
import com.Chagui68.items.components.*;
import com.Chagui68.items.food.HeadSlimeGelatin;
import com.Chagui68.items.food.ScoobyCookie;
import com.Chagui68.items.misc.IceCrown;
import com.Chagui68.items.misc.MantisClaws;
import com.Chagui68.items.misc.MilitaryMine;
import com.Chagui68.items.misc.WirtsLantern;
import com.Chagui68.items.misc.WitherStormHead;
import com.Chagui68.items.misc.offhand.FrostHeartOffhand;
import com.Chagui68.items.misc.offhand.MarrowAegis;
import com.Chagui68.items.misc.offhand.VeilwalkerMantle;
import com.Chagui68.items.potions.VampirePotions;
import com.Chagui68.items.weapons.magic.ChaosForge;
import com.Chagui68.items.weapons.magic.SentinelGrimoire;
import com.Chagui68.items.weapons.magic.SkyfireTalisman;
import com.Chagui68.items.weapons.melee.CinderGreatsword;
import com.Chagui68.items.weapons.melee.Excalibur;
import com.Chagui68.items.weapons.melee.ExecutionerGuillotine;
import com.Chagui68.items.weapons.melee.NullshearEdge;
import com.Chagui68.items.weapons.melee.SoulreapScythe;
import com.Chagui68.items.weapons.melee.Venomfang;
import com.Chagui68.items.weapons.ranged.AetherPullshot;
import com.Chagui68.items.weapons.ranged.ArchitectDeployer;
import com.Chagui68.stand.StandType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.Chagui68.wiki.WikiCategory.*;

/**
 * Every page of the wiki, in both languages.
 *
 * <p>Recipes are not written here: {@link WikiRecipes} reads them from the server. This class only
 * says what a recipe cannot — who drops an item, who sells it, how a boss is called — and the
 * pages that have no item at all. Built on first use, never while the class loads, because the
 * items read the server's item meta.</p>
 */
public final class WikiCatalogue {

    private static List<WikiEntry> entries;
    private static Map<String, WikiEntry> byId;

    private WikiCatalogue() {
    }

    public static synchronized List<WikiEntry> entries() {
        if (entries == null) {
            entries = Collections.unmodifiableList(build());
            Map<String, WikiEntry> index = new LinkedHashMap<>();
            for (WikiEntry entry : entries) {
                if (index.put(entry.id(), entry) != null) {
                    throw new IllegalStateException("Duplicate wiki page " + entry.id());
                }
            }
            byId = index;
        }
        return entries;
    }

    public static WikiEntry byId(String id) {
        entries();
        return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT));
    }

    public static List<WikiEntry> in(WikiCategory category) {
        List<WikiEntry> list = new ArrayList<>();
        for (WikiEntry entry : entries()) {
            if (entry.category() == category) {
                list.add(entry);
            }
        }
        return list;
    }

    // ----------------------------------------------------------------- sources

    private static WikiSource mob(Material egg, String mobEn, String mobEs, String spawnEn, String spawnEs,
                                  String section, double fallback) {
        return WikiSource.drop(egg, mobEn, mobEs, spawnEn, spawnEs, "entities." + section + ".drop-chance", fallback);
    }

    private static WikiSource merchant(String priceEn, String priceEs) {
        return WikiSource.of(Material.WANDERING_TRADER_SPAWN_EGG, "Multiverse Merchant", "Multiverse Merchant",
                "Sold by the Multiverse Merchant, a Wandering Trader that sometimes arrives from another world, for "
                        + priceEn + ".",
                "Lo vende el Multiverse Merchant, un Comerciante Errante que a veces llega desde otro mundo, por "
                        + priceEs + ".");
    }

    private static WikiRecipe trade(ItemStack first, ItemStack second, ItemStack result) {
        return WikiRecipe.trade(first, second, result);
    }

    private static ItemStack of(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private static ItemStack amount(ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static ItemStack awkwardPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        if (potion.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(PotionType.AWKWARD);
            potion.setItemMeta(meta);
        }
        return potion;
    }

    // ------------------------------------------------------------------ pages

    private static List<WikiEntry> build() {
        List<WikiEntry> list = new ArrayList<>();
        weapons(list);
        armor(list);
        relics(list);
        consumables(list);
        drops(list);
        crafted(list);
        bossLoot(list);
        stands(list);
        bosses(list);
        return list;
    }

    private static void weapons(List<WikiEntry> list) {
        list.add(WikiEntry.item("excalibur", WEAPONS, () -> Excalibur.EXCALIBUR_SWORD)
                .source(merchant("16 Star Cores and 32 Netherite Ingots", "16 Star Core y 32 lingotes de netherita"))
                .recipe(() -> trade(amount(StarCore.STAR_CORE, 16), of(Material.NETHERITE_INGOT, 32),
                        Excalibur.EXCALIBUR_SWORD.clone()))
                .build());
        list.add(WikiEntry.item("cindergreatsword", WEAPONS, () -> CinderGreatsword.CINDER_GREATSWORD).build());
        list.add(WikiEntry.item("nullshearedge", WEAPONS, () -> NullshearEdge.NULLSHEAR_EDGE).build());
        list.add(WikiEntry.item("soulreapscythe", WEAPONS, () -> SoulreapScythe.SOULREAP_SCYTHE).build());
        list.add(WikiEntry.item("venomfang", WEAPONS, () -> Venomfang.VENOMFANG).build());
        list.add(WikiEntry.item("executionerguillotine", WEAPONS, () -> ExecutionerGuillotine.EXECUTIONER_GUILLOTINE)
                .about("NIX's own axe. Its blade comes from the Executioner's Edges he drops.",
                        "El hacha de NIX. Su hoja sale de los Executioner's Edge que él suelta.")
                .build());
        list.add(WikiEntry.item("aetherpullshot", WEAPONS, () -> AetherPullshot.AETHER_PULLSHOT).build());
        list.add(WikiEntry.item("architectdeployer", WEAPONS, () -> ArchitectDeployer.ARCHITECT_DEPLOYER)
                .about("Jack Star's legacy, built round the Architect Kernel he drops.",
                        "El legado de Jack Star, construido alrededor del Architect Kernel que suelta.")
                .source(WikiSource.of(Material.SPECTRAL_ARROW, "Damage", "Daño",
                        "Guided arrows deal 68% more damage and sudo rm -rf deals 16.8 to each target. Against the "
                                + "Wither Storm its arrows count as arrows: their own 150 cap, then twice the damage.",
                        "Las flechas guiadas hacen un 68% más de daño y sudo rm -rf hace 16.8 a cada objetivo. Contra el "
                                + "Wither Storm sus flechas cuentan como flechas: su propio tope de 150 y luego el doble "
                                + "de daño."))
                .build());
        list.add(WikiEntry.item("chaosforge", WEAPONS, () -> ChaosForge.CHAOS_FORGE).build());
        list.add(WikiEntry.item("skyfiretalisman", WEAPONS, () -> SkyfireTalisman.SKYFIRE_TALISMAN).build());
        list.add(WikiEntry.item("sentinelgrimoire", WEAPONS, () -> SentinelGrimoire.GRIMOIRE).build());
    }

    private static void armor(List<WikiEntry> list) {
        list.add(WikiEntry.item("eighthandledwheel", ARMOR, () -> EightHandledWheel.EIGHT_HANDLED_WHEEL)
                .about("Mahoraga's wheel. It adapts to every kind of damage you take.",
                        "La rueda de Mahoraga. Se adapta a cada tipo de daño que recibes.")
                .build());
        String bastionEn = "Wear all four pieces of the Obsidian Bastion for its set bonus.";
        String bastionEs = "Lleva las cuatro piezas del Obsidian Bastion para activar su bonificación de set.";
        list.add(WikiEntry.item("obsidianbastionhelmet", ARMOR, () -> ObsidianBastion.HELMET).about(bastionEn, bastionEs).build());
        list.add(WikiEntry.item("obsidianbastionchestplate", ARMOR, () -> ObsidianBastion.CHESTPLATE).about(bastionEn, bastionEs).build());
        list.add(WikiEntry.item("obsidianbastionleggings", ARMOR, () -> ObsidianBastion.LEGGINGS).about(bastionEn, bastionEs).build());
        list.add(WikiEntry.item("obsidianbastionboots", ARMOR, () -> ObsidianBastion.BOOTS).about(bastionEn, bastionEs).build());
        String worldbinderEn = "Full set: +50% max health (bosses do not hit harder for it), 30% less damage taken,"
                + " 40% less boss true damage, no single hit from a boss, player or mob takes more than 35% of max"
                + " health, +35% damage dealt, 15% of the damage dealt heals you, +25% speed on land and in water,"
                + " immune to fire, lava and drowning. Potions last 20% longer, and healing, Regeneration,"
                + " Resistance and Strength work 20% better. Rebound (15%): a hit is reflected back 20% stronger."
                + " Defy Death: survive a lethal hit from anything and stay invulnerable for 3 s (90 s cooldown)."
                + " Unseen (10% when hit): fully invisible, armor and held items included, and no boss or mob"
                + " targets you for 5 s (30 s cooldown).";
        String worldbinderEs = "Set completo: +50% de vida máxima (los jefes no pegan más fuerte por ella), 30% menos"
                + " de daño recibido, 40% menos de daño verdadero de jefes, ningún golpe de un jefe, jugador o mob"
                + " quita más del 35% de la vida máxima, +35% de daño infligido, el 15% del daño infligido te cura,"
                + " +25% de velocidad en tierra y agua, inmunidad al fuego, la lava y el ahogamiento. Las pociones"
                + " duran 20% más, y la curación, Regeneración, Resistencia y Fuerza rinden 20% más. Rebote (15%):"
                + " refleja un golpe 20% más fuerte. Desafiar a la muerte: sobrevive a un golpe letal de lo que sea"
                + " y queda invulnerable 3 s (enfriamiento 90 s). Invisible (10% al recibir golpe): totalmente"
                + " invisible, armadura y objetos incluidos, y ningún jefe ni mob te elige como objetivo durante 5 s"
                + " (enfriamiento 30 s).";
        list.add(WikiEntry.item("worldbinderhelmet", ARMOR, () -> Worldbinder.HELMET).about(worldbinderEn, worldbinderEs).build());
        list.add(WikiEntry.item("worldbinderchestplate", ARMOR, () -> Worldbinder.CHESTPLATE).about(worldbinderEn, worldbinderEs).build());
        list.add(WikiEntry.item("worldbinderleggings", ARMOR, () -> Worldbinder.LEGGINGS).about(worldbinderEn, worldbinderEs).build());
        list.add(WikiEntry.item("worldbinderboots", ARMOR, () -> Worldbinder.BOOTS).about(worldbinderEn, worldbinderEs).build());
        list.add(WikiEntry.item("marrowaegis", ARMOR, () -> MarrowAegis.MARROW_AEGIS).build());
        list.add(WikiEntry.item("frostheartoffhand", ARMOR, () -> FrostHeartOffhand.FROST_HEART_OFFHAND).build());
        list.add(WikiEntry.item("veilwalkermantle", ARMOR, () -> VeilwalkerMantle.VEILWALKER_MANTLE).build());
    }

    private static void relics(List<WikiEntry> list) {
        list.add(WikiEntry.item("icecrown", RELICS, () -> IceCrown.ICE_CROWN)
                .source(merchant("48 Nether Stars and 64 Blue Ice", "48 estrellas del Nether y 64 de hielo azul"))
                .recipe(() -> trade(of(Material.NETHER_STAR, 48), of(Material.BLUE_ICE, 64), IceCrown.ICE_CROWN.clone()))
                .build());
        list.add(WikiEntry.item("mantisclaws", RELICS, () -> MantisClaws.MANTIS_CLAWS_ITEM)
                .source(merchant("16 Iron Ingots and 8 String", "16 lingotes de hierro y 8 de cuerda"))
                .recipe(() -> trade(of(Material.IRON_INGOT, 16), of(Material.STRING, 8), MantisClaws.MANTIS_CLAWS_ITEM.clone()))
                .build());
        list.add(WikiEntry.item("wirtslantern", RELICS, () -> WirtsLantern.WIRTS_LANTERN)
                .about("Held in either hand: hostile mobs are pushed away and stop targeting you, and after 1.5 s "
                                + "they cannot hurt you. Players, falls, fire, the void and /kill still can. While you "
                                + "hold it, and for 3 s after putting it away, you cannot hurt anything. It goes dark "
                                + "in the Boss Dimension.",
                        "En cualquier mano: los mobs hostiles se apartan y dejan de apuntarte, y tras 1,5 s no "
                                + "pueden hacerte daño. Los jugadores, las caídas, el fuego, el vacío y /kill sí. Mientras "
                                + "la llevas, y 3 s después de guardarla, no puedes dañar a nada. En la Dimensión de "
                                + "Jefes se apaga.")
                .source(merchant("The Beast's Antler and a flask of Edelwood Oil",
                        "The Beast's Antler y un frasco de Edelwood Oil"))
                .recipe(() -> trade(BeastAntler.BEAST_ANTLER.clone(), EdelwoodOil.EDELWOOD_OIL.clone(),
                        WirtsLantern.WIRTS_LANTERN.clone()))
                .build());
        list.add(WikiEntry.item("militarymine", RELICS, () -> MilitaryMine.MILITARY_MINE)
                .about("Place it and it takes the look of the ground around it. One step is enough.",
                        "Al colocarla toma el aspecto del suelo que la rodea. Basta con un paso.")
                .build());
        list.add(WikiEntry.info("musicdiscs", RELICS, Material.MUSIC_DISC_13, NamedTextColor.AQUA,
                        "Music Discs", "Discos de música")
                .about("Custom songs that play in any jukebox, from Megalovania to Bohemian Rhapsody.",
                        "Canciones propias que suenan en cualquier tocadiscos, de Megalovania a Bohemian Rhapsody.")
                .source(WikiSource.of(Material.VILLAGER_SPAWN_EGG, "Disc Trader", "Disc Trader",
                        "A villager that sometimes spawns as a Disc Trader sells every disc for 16 Emeralds.",
                        "Un aldeano que a veces aparece como Disc Trader vende cada disco por 16 esmeraldas."))
                .build());
    }

    private static void consumables(List<WikiEntry> list) {
        list.add(WikiEntry.item("scoobycookie", CONSUMABLES, () -> ScoobyCookie.SCOOBY_COOKIE)
                .source(merchant("20 Diamonds (5 cookies)", "20 diamantes (5 galletas)"))
                .source(WikiSource.of(Material.COMMAND_BLOCK, "Jack Star", "Jack Star",
                        "Jack Star throws debug cookies during his fight.",
                        "Jack Star lanza galletas de depuración durante su combate."))
                .recipe(() -> trade(of(Material.DIAMOND, 20), null, amount(ScoobyCookie.SCOOBY_COOKIE, 5)))
                .build());
        list.add(WikiEntry.item("headslimegelatin", CONSUMABLES, () -> HeadSlimeGelatin.HEAD_SLIME_GELATIN).build());
        list.add(WikiEntry.item("unstableblood", CONSUMABLES, () -> VampirePotions.UNSTABLE_BLOOD)
                .recipe(() -> WikiRecipe.brewing(VampireBlood.VAMPIRE_BLOOD.clone(), awkwardPotion(),
                        VampirePotions.UNSTABLE_BLOOD.clone()))
                .build());
        list.add(WikiEntry.item("bearerelixir", CONSUMABLES, () -> VampirePotions.BEARER_ELIXIR)
                .about("Drink it to become a vampire who can carry a Stand. See the Stands section.",
                        "Bébela para volverte un vampiro capaz de portar un Stand. Mira la sección de Stands.")
                .recipe(() -> WikiRecipe.brewing(new ItemStack(Material.WITHER_ROSE), VampirePotions.UNSTABLE_BLOOD.clone(),
                        VampirePotions.BEARER_ELIXIR.clone()))
                .build());
    }

    private static void drops(List<WikiEntry> list) {
        list.add(WikiEntry.item("wheelessence", DROPS, () -> WheelEssence.WHEEL_ESSENCE)
                .source(mob(Material.ZOMBIE_HEAD, "Mahoraga", "Mahoraga",
                        "A miniboss that very rarely rises in place of a Zombie.",
                        "Un minijefe que muy rara vez aparece en lugar de un Zombi.", "mahoraga", 0.75))
                .build());
        list.add(WikiEntry.item("chaosorb", DROPS, () -> ChaosOrb.CHAOS_ORB)
                .source(mob(Material.EVOKER_SPAWN_EGG, "Chaos Mage", "Chaos Mage",
                        "Takes the place of some Evokers, in mansions and in raids.",
                        "Ocupa el lugar de algunos Evocadores, en mansiones y en asaltos.", "chaos-mage", 0.6))
                .build());
        list.add(WikiEntry.item("enderfragment", DROPS, () -> EnderFragment.ENDER_FRAGMENT)
                .source(mob(Material.ENDERMAN_SPAWN_EGG, "Ender Knight", "Ender Knight",
                        "Takes the place of some Endermen.",
                        "Ocupa el lugar de algunos Endermans.", "ender-knight", 0.55))
                .build());
        list.add(WikiEntry.item("frostheart", DROPS, () -> FrostHeart.FROST_HEART)
                .source(mob(Material.IRON_GOLEM_SPAWN_EGG, "Frost Golem", "Frost Golem",
                        "Some Iron Golems, natural or built, awaken as Frost Golems.",
                        "Algunos Gólems de Hierro, naturales o construidos, despiertan como Frost Golem.",
                        "frost-golem", 0.75))
                .build());
        list.add(WikiEntry.item("magmacore", DROPS, () -> MagmaCore.MAGMA_CORE)
                .source(mob(Material.BLAZE_SPAWN_EGG, "Flame Elemental", "Flame Elemental",
                        "Takes the place of some Blazes in Nether fortresses.",
                        "Ocupa el lugar de algunos Blazes en las fortalezas del Nether.", "flame-elemental", 0.6))
                .build());
        list.add(WikiEntry.item("stormcrystal", DROPS, () -> StormCrystal.STORM_CRYSTAL)
                .source(mob(Material.WITCH_SPAWN_EGG, "Storm Caller", "Storm Caller",
                        "Takes the place of some Witches, raids included.",
                        "Ocupa el lugar de algunas Brujas, también en asaltos.", "storm-caller", 0.6))
                .build());
        list.add(WikiEntry.item("venomgland", DROPS, () -> VenomGland.VENOM_GLAND)
                .source(mob(Material.WITCH_SPAWN_EGG, "Venom Witch", "Venom Witch",
                        "Takes the place of some Witches, raids included.",
                        "Ocupa el lugar de algunas Brujas, también en asaltos.", "venom-witch", 0.6))
                .build());
        list.add(WikiEntry.item("voidessence", DROPS, () -> VoidEssence.VOID_ESSENCE)
                .source(mob(Material.SPIDER_SPAWN_EGG, "Void Crawler", "Void Crawler",
                        "Takes the place of some Spiders.",
                        "Ocupa el lugar de algunas Arañas.", "void-crawler", 0.5))
                .build());
        list.add(WikiEntry.item("reaperessence", DROPS, () -> ReaperEssence.REAPER_ESSENCE)
                .source(mob(Material.WITHER_SKELETON_SPAWN_EGG, "Soul Reaper", "Soul Reaper",
                        "Takes the place of some Wither Skeletons.",
                        "Ocupa el lugar de algunos Esqueletos Wither.", "soul-reaper", 0.6))
                .build());
        list.add(WikiEntry.item("reinforcedbone", DROPS, () -> ReinforcedBone.REINFORCED_BONE)
                .source(mob(Material.SKELETON_SPAWN_EGG, "Bone Shield", "Bone Shield",
                        "Takes the place of some Skeletons.",
                        "Ocupa el lugar de algunos Esqueletos.", "bone-shield", 0.8))
                .build());
        list.add(WikiEntry.item("shadowcloak", DROPS, () -> ShadowCloak.SHADOW_CLOAK)
                .source(mob(Material.SKELETON_SPAWN_EGG, "Shadow Rogue", "Shadow Rogue",
                        "Takes the place of some Skeletons.",
                        "Ocupa el lugar de algunos Esqueletos.", "shadow-rogue", 0.5))
                .build());
        list.add(WikiEntry.item("obsidianshard", DROPS, () -> ObsidianShard.OBSIDIAN_SHARD)
                .source(mob(Material.ZOMBIE_SPAWN_EGG, "Obsidian Guard", "Obsidian Guard",
                        "A heavy zombie that takes the place of some Zombies.",
                        "Un zombi pesado que ocupa el lugar de algunos Zombis.", "obsidian-guard", 0.85))
                .build());
        list.add(WikiEntry.item("headslimeheart", DROPS, () -> HeadSlimeHeart.HEAD_SLIME_HEART)
                .source(WikiSource.of(Material.SLIME_SPAWN_EGG, "Head Slime", "Head Slime",
                        "Always dropped by the Head Slime, which takes the place of some Slimes.",
                        "Siempre lo suelta el Head Slime, que ocupa el lugar de algunos Slimes."))
                .build());
        list.add(WikiEntry.item("militarycomponent", DROPS, () -> MilitaryComponent.MILITARY_COMPONENT)
                .source(WikiSource.drop(Material.ZOMBIE_HORSE_SPAWN_EGG, "Military Army", "Military Army",
                        "Under a full moon a zombie horse may wander in; get close and a whole army ambushes you. "
                                + "Every soldier can drop one.",
                        "Con luna llena puede aparecer un caballo zombi; si te acercas, un ejército entero te embosca. "
                                + "Cada soldado puede soltar uno.",
                        "entities.zombie-horse-trap.military-component-drop-chance", 0.3))
                .build());
    }

    private static void crafted(List<WikiEntry> list) {
        list.add(WikiEntry.item("starcore", CRAFTED, () -> StarCore.STAR_CORE).build());
        list.add(WikiEntry.item("swordmold", CRAFTED, () -> SwordMold.SWORD_MOLD).build());
        list.add(WikiEntry.item("reinforcedboneblock", CRAFTED, () -> ReinforcedBoneBlock.REINFORCED_BONE_BLOCK).build());
        list.add(WikiEntry.item("bonemarrow", CRAFTED, () -> BoneMarrow.BONE_MARROW).build());
        list.add(WikiEntry.item("ossifiedplate", CRAFTED, () -> OssifiedPlate.OSSIFIED_PLATE).build());
        list.add(WikiEntry.item("moltenmarrow", CRAFTED, () -> MoltenMarrow.MOLTEN_MARROW)
                .about("Only a Blast Furnace is hot enough to melt the plate.",
                        "Solo un Alto horno está lo bastante caliente para fundir la placa.")
                .build());
        list.add(WikiEntry.item("chaospowder", CRAFTED, () -> ChaosPowder.CHAOS_POWDER).build());
        list.add(WikiEntry.item("chaosfragment", CRAFTED, () -> ChaosFragment.CHAOS_FRAGMENT).build());
        list.add(WikiEntry.item("chaoscore", CRAFTED, () -> ChaosCore.CHAOS_CORE).build());
        list.add(WikiEntry.item("condensedchaosorb", CRAFTED, () -> CondensedChaosOrb.CONDENSED_CHAOS_ORB).build());
        list.add(WikiEntry.item("endercore", CRAFTED, () -> EnderCore.ENDER_CORE).build());
        list.add(WikiEntry.item("wheelcore", CRAFTED, () -> WheelCore.WHEEL_CORE).build());
        list.add(WikiEntry.item("moltenwheelcore", CRAFTED, () -> MoltenWheelCore.MOLTEN_WHEEL_CORE).build());
        list.add(WikiEntry.item("compressedgoldblock", CRAFTED, () -> CompressedGoldBlock.COMPRESSED_GOLD_BLOCK).build());
        list.add(WikiEntry.item("refinednetherite", CRAFTED, () -> RefinedNetherite.REFINED_NETHERITE).build());
        list.add(WikiEntry.item("moltennetherite", CRAFTED, () -> MoltenNetherite.MOLTEN_NETHERITE).build());
        list.add(WikiEntry.item("refinedwheelcore", CRAFTED, () -> RefinedWheelCore.REFINED_WHEEL_CORE).build());
        list.add(WikiEntry.item("reapercore", CRAFTED, () -> ReaperCore.REAPER_CORE).build());
        list.add(WikiEntry.item("multiversalcore", CRAFTED, () -> MultiversalCore.MULTIVERSAL_CORE).build());
        // Wirt's Lantern chain: each one is made from the one before it.
        list.add(WikiEntry.item("edelwoodsap", CRAFTED, () -> EdelwoodSap.EDELWOOD_SAP)
                .about("First step towards Wirt's Lantern. The Reaper Essence comes from Soul Reapers; the Creaking "
                                + "Heart needs Silk Touch in a Pale Garden.",
                        "Primer paso hacia la Linterna de Wirt. La Reaper Essence sale de los Soul Reaper; el Corazón "
                                + "de Crujidor necesita Toque de Seda en un Jardín Pálido.")
                .build());
        list.add(WikiEntry.item("edelwoodoil", CRAFTED, () -> EdelwoodOil.EDELWOOD_OIL)
                .about("Second step. The merchant also takes one flask with the Beast's Antler.",
                        "Segundo paso. El comerciante también pide un frasco junto al Beast's Antler.")
                .build());
        list.add(WikiEntry.item("pottsfieldgourd", CRAFTED, () -> PottsfieldGourd.POTTSFIELD_GOURD)
                .about("Third step. Skeleton Skulls only drop from skeletons killed by a charged creeper.",
                        "Tercer paso. Las calaveras de esqueleto solo caen de esqueletos que mata un creeper cargado.")
                .build());
        list.add(WikiEntry.item("beatricefeather", CRAFTED, () -> BeatriceFeather.BEATRICE_FEATHER)
                .about("Fourth step. Echo Shards are found in Ancient City chests.",
                        "Cuarto paso. Los Fragmentos de Eco se encuentran en los cofres de las Ciudades Antiguas.")
                .build());
        list.add(WikiEntry.item("beastantler", CRAFTED, () -> BeastAntler.BEAST_ANTLER)
                .about("Last step: trade it with a flask of Edelwood Oil to the Multiverse Merchant for Wirt's Lantern.",
                        "Último paso: cámbialo con un frasco de Edelwood Oil al Multiverse Merchant por la Linterna de "
                                + "Wirt.")
                .build());
    }

    private static void bossLoot(List<WikiEntry> list) {
        list.add(WikiEntry.item("sentinelcore", BOSS_LOOT, () -> SentinelCore.SENTINEL_CORE)
                .source(new WikiSource(Material.ARMOR_STAND, "The Obsidian Sentinel", "The Obsidian Sentinel",
                        "The final boss of the Boss Dimension drops it when he falls.",
                        "El jefe final de la Dimensión de Jefes lo suelta al caer.",
                        "entities.armor-stand-boss.sentinel-core-drop-chance", 100.0, true))
                .build());
        list.add(WikiEntry.item("vampireblood", BOSS_LOOT, () -> VampireBlood.VAMPIRE_BLOOD)
                .source(WikiSource.of(Material.CLOCK, "DIO", "DIO",
                        "DIO always leaves 1 to 2 Vampire Blood behind when he falls.",
                        "DIO siempre deja entre 1 y 2 Vampire Blood al caer."))
                .build());
        list.add(WikiEntry.item("executioneredge", BOSS_LOOT, () -> ExecutionerEdge.EXECUTIONER_EDGE)
                .source(new WikiSource(Material.NETHERITE_AXE, "NIX - The Executioner", "NIX - The Executioner",
                        "NIX drops one Edge, and sometimes a second one.",
                        "NIX suelta un Filo y, a veces, un segundo.",
                        "entities.nix-executioner.edge-drop-chance", 1.0, false))
                .build());
        list.add(WikiEntry.item("executionerwarrant", BOSS_LOOT, () -> ExecutionerWarrant.EXECUTIONER_WARRANT)
                .about("The offering that summons NIX. See Bosses & Rituals.",
                        "La ofrenda que invoca a NIX. Mira Jefes y rituales.")
                .build());
        list.add(WikiEntry.item("architectkernel", BOSS_LOOT, () -> ArchitectKernel.ARCHITECT_KERNEL)
                .source(WikiSource.of(Material.COMMAND_BLOCK, "JackStar", "JackStar",
                        "JackStar always drops it when his last process ends.",
                        "JackStar siempre lo suelta cuando termina su último proceso."))
                .build());
        list.add(WikiEntry.item("garoucosmiccore", BOSS_LOOT, () -> GarouCosmicCore.GAROU_COSMIC_CORE)
                .source(WikiSource.of(Material.WITHER_SKELETON_SKULL, "Garou", "Garou",
                        "Garou always drops it. He very rarely takes the place of a Wither Skeleton.",
                        "Garou siempre lo suelta. Muy rara vez aparece en lugar de un Esqueleto Wither."))
                .build());
        list.add(WikiEntry.item("witherstormhead", BOSS_LOOT, () -> WitherStormHead.of("the Wither Storm"))
                .about("A trophy: one of the Wither Storm's heads, named after the form it fell in. It can be placed "
                                + "like any skull.",
                        "Un trofeo: una de las cabezas del Wither Storm, con el nombre de la forma en la que cayó. Se "
                                + "puede colocar como cualquier calavera.")
                .source(WikiSource.of(Material.WITHER_ROSE, "Wither Storm", "Wither Storm",
                        "The Wither Storm always drops its head and one Nether Star when it falls.",
                        "El Wither Storm siempre suelta su cabeza y una Estrella del Nether al caer."))
                .build());
    }

    private static void stands(List<WikiEntry> list) {
        list.add(WikiEntry.info("thearrow", STANDS, Material.SPECTRAL_ARROW, NamedTextColor.GOLD,
                        "Awakening a Stand", "Despertar un Stand")
                .about("The Arrow judges whoever it pierces. DIO's blood makes you a Stand bearer: the Arrow can no "
                                + "longer kill you and always awakens your Stand.",
                        "La Flecha juzga a quien atraviesa. La sangre de DIO te vuelve portador de Stand: la Flecha ya "
                                + "no puede matarte y siempre despierta tu Stand.")
                .source(WikiSource.of(Material.SKELETON_SKULL, "The Archer of the Arrow", "El Archer of the Arrow",
                        "A crowned skeleton that takes the place of some Skeletons. A few of its shots are the Arrow.",
                        "Un esqueleto con corona que ocupa el lugar de algunos Esqueletos. Algunos de sus disparos "
                                + "son la Flecha."))
                .source(WikiSource.of(Material.POTION, "With DIO's blood", "Con la sangre de DIO",
                        "Drink the Bearer's Elixir and you are a Stand bearer: the Arrow never kills you and always "
                                + "awakens a random Stand.",
                        "Bebe el Bearer's Elixir y serás portador de Stand: la Flecha nunca te mata y siempre despierta "
                                + "un Stand al azar."))
                .source(new WikiSource(Material.SPECTRAL_ARROW, "Without it: death chance", "Sin ella: probabilidad de morir",
                        "Without the Elixir, the Arrow kills most of those it pierces. The survivors awaken a Stand.",
                        "Sin el Elixir, la Flecha mata a la mayoría de quienes atraviesa. Los que sobreviven despiertan "
                                + "un Stand.",
                        "entities.arrow-skeleton.death-chance", 0.7, false))
                .source(WikiSource.of(Material.WITHER_SKELETON_SKULL, "Unworthy", "Indigno",
                        "Whoever the Arrow kills is marked unworthy: it gives them no Stand until they drink the "
                                + "Bearer's Elixir, which lifts the mark.",
                        "Quien muere por la Flecha queda marcado como indigno: no le dará un Stand hasta que beba el "
                                + "Bearer's Elixir, que borra la marca."))
                .source(WikiSource.of(Material.KNOWLEDGE_BOOK, "Controls", "Controles",
                        "Sneak + F summons your Stand or sends it back. F uses its ability. /stand shows yours.",
                        "Agachado + F invoca o retira tu Stand. F usa su habilidad. /stand muestra el tuyo."))
                .build());
        list.add(WikiEntry.info("vampirism", STANDS, Material.RED_DYE, NamedTextColor.DARK_RED,
                        "Vampirism", "Vampirismo")
                .about("The Bearer's Elixir also makes you a vampire: Strength and Speed at night, Night Vision and "
                                + "life steal. The sun burns you with true damage under the open sky.",
                        "El Bearer's Elixir también te convierte en vampiro: Fuerza y Velocidad de noche, Visión "
                                + "nocturna y robo de vida. El sol te quema con daño verdadero bajo el cielo abierto.")
                .build());
        stand(list, StandType.HERMIT_PURPLE,
                "Thorned purple vines coil round your arm.",
                "Enredaderas moradas con espinas rodean tu brazo.",
                "F: Spirit Photography. Every living thing nearby glows and the nearest player is pointed out.",
                "F: Fotografía espiritual. Todo ser vivo cercano brilla y se señala al jugador más próximo.");
        stand(list, StandType.MAGICIANS_RED,
                "A bird-headed warrior of fire.",
                "Un guerrero de fuego con cabeza de ave.",
                "F: Crossfire Hurricane. A fan of burning ankhs that sets its targets alight.",
                "F: Crossfire Hurricane. Un abanico de cruces de fuego que incendia a sus objetivos.");
        stand(list, StandType.CRAZY_DIAMOND,
                "A pink Stand covered in hearts, with a diamond on its chest.",
                "Un Stand rosa cubierto de corazones, con un diamante en el pecho.",
                "F: Restoration. Heals the player you look at, or you, and mends the item they hold.",
                "F: Restauración. Cura al jugador que miras, o a ti, y repara el objeto que sostiene.");
        stand(list, StandType.KILLER_QUEEN,
                "A cat-eared Stand with skulls on its belt and hand.",
                "Un Stand con orejas de gato y calaveras en el cinturón y la mano.",
                "Passive: First Bomb, every player you hit explodes. F or /stand sha: Sheer Heart Attack chases the "
                        + "player you choose.",
                "Pasiva: Primera Bomba, cada jugador que golpeas explota. F o /stand sha: Sheer Heart Attack "
                        + "persigue al jugador que elijas.");
        stand(list, StandType.STAR_PLATINUM,
                "A Stand with a black mane, a red scarf and fists of steel.",
                "Un Stand de melena negra, bufanda roja y puños de acero.",
                "F: ORA ORA barrage on whoever stands in front of you. Sneak + left-click with an empty hand: "
                        + "Star Platinum: The World stops time.",
                "F: ráfaga ORA ORA a quien tengas delante. Agachado + clic izquierdo con la mano vacía: Star "
                        + "Platinum: The World detiene el tiempo.");
        stand(list, StandType.THE_WORLD,
                "DIO's own Stand, golden with emerald hearts.",
                "El propio Stand de DIO, dorado y con corazones de esmeralda.",
                "F: MUDA MUDA barrage. Sneak + left-click with an empty hand: ZA WARUDO, a longer time stop.",
                "F: ráfaga MUDA MUDA. Agachado + clic izquierdo con la mano vacía: ZA WARUDO, una detención del "
                        + "tiempo más larga.");
    }

    private static void stand(List<WikiEntry> list, StandType type, String lookEn, String lookEs,
                              String powerEn, String powerEs) {
        list.add(WikiEntry.info(type.key(), STANDS, type.icon(), type.textColor(),
                        "「" + type.displayName() + "」", "「" + type.displayName() + "」")
                .about(lookEn, lookEs)
                .source(WikiSource.of(Material.BLAZE_POWDER, "Abilities", "Habilidades", powerEn, powerEs))
                .source(new WikiSource(Material.SPECTRAL_ARROW, "Rarity", "Rareza",
                        "How likely the Arrow is to awaken this Stand, out of every Stand.",
                        "Probabilidad de que la Flecha despierte este Stand, entre todos los Stands.",
                        "stands." + type.key() + ".weight", type.defaultWeight(), false))
                .build());
    }

    private static void bosses(List<WikiEntry> list) {
        list.add(WikiEntry.info("bossdimension", BOSSES, Material.CRYING_OBSIDIAN, NamedTextColor.DARK_PURPLE,
                        "The Boss Dimension", "La Dimensión de Jefes")
                .about("Every great boss is fought in a wasteland of its own. Build the Ritual Structure in the "
                                + "overworld to get there.",
                        "Cada gran jefe se enfrenta en un páramo propio. Construye la Estructura Ritual en el mundo "
                                + "normal para llegar.")
                .source(WikiSource.of(Material.OBSIDIAN, "The Ritual Structure", "La Estructura Ritual",
                        "A 7x7 platform of blackstone with crying obsidian and obsidian in the middle, ringed by 12 "
                                + "candles. Light them all and stand inside for five seconds.",
                        "Una plataforma de 7x7 de piedra negra con obsidiana llorosa y obsidiana en el centro, rodeada "
                                + "de 12 velas. Enciéndelas todas y quédate dentro cinco segundos."))
                .source(WikiSource.of(Material.SPLASH_POTION, "No natural healing", "Sin curación natural",
                        "Health does not come back on its own: only potion effects heal (instant health, regeneration; "
                                + "golden apples and totems still work).",
                        "La vida no vuelve sola: solo curan los efectos de poción (curación instantánea, regeneración; "
                                + "las manzanas doradas y los tótems siguen funcionando)."))
                .source(WikiSource.of(Material.COOKED_BEEF, "Eat to stay fed", "Come para no pasar hambre",
                        "The Saturation effect does nothing there: hunger only comes back by eating.",
                        "El efecto de Saturación no funciona allí: el hambre solo se recupera comiendo."))
                .source(WikiSource.of(Material.ELYTRA, "Nobody flies", "Nadie vuela",
                        "No flight of any kind: /fly from any plugin, creative flight and elytras are all turned off.",
                        "Nada de volar: el /fly de cualquier plugin, el vuelo creativo y las élitras quedan desactivados."))
                .source(WikiSource.of(Material.FEATHER, "Low gravity, hard falls", "Poca gravedad, caídas duras",
                        "Gravity is 30% lower, so you jump higher and fall slower, but falls hurt 70% more, and 20% "
                                + "of a fall is true damage that no armour or enchantment stops.",
                        "La gravedad es un 30% menor: saltas más alto y caes más despacio, pero las caídas hacen un 70% "
                                + "más de daño, y el 20% de una caída es daño verdadero que ninguna armadura ni "
                                + "encantamiento frena."))
                .source(WikiSource.of(Material.SHIELD, "Every boss holds out", "Todos los jefes aguantan",
                        "No boss takes more than 10 damage from a single hit, however strong it is.",
                        "Ningún jefe recibe más de 10 de daño de un solo golpe, por fuerte que sea."))
                .source(WikiSource.of(Material.DIAMOND_SWORD, "Sharp swords are blunted", "Las espadas afiladas se embotan",
                        "Every boss but Mahoraga and Kinger takes 2% less from a sword for every 5 levels of "
                                + "Sharpness it has, 50% less at most (Sharpness 125). It is taken off after the 10 cap.",
                        "Todos los jefes menos Mahoraga y Kinger reciben un 2% menos de una espada por cada 5 niveles "
                                + "de Filo que tenga, un 50% menos como máximo (Filo 125). Se resta después del tope de 10."))
                .source(WikiSource.of(Material.IRON_CHESTPLATE, "Weaker Resistance", "Resistencia más débil",
                        "The Resistance effect blocks 10% less than usual there: Resistance I blocks 18% instead of "
                                + "20%. It counts against the bosses' true damage too.",
                        "El efecto de Resistencia bloquea un 10% menos de lo normal allí: Resistencia I bloquea un 18% "
                                + "en vez de un 20%. También cuenta contra el daño verdadero de los jefes."))
                .source(WikiSource.of(Material.GHAST_TEAR, "Slower Regeneration", "Regeneración más lenta",
                        "The Regeneration effect heals half as fast there.",
                        "El efecto de Regeneración cura la mitad de rápido allí."))
                .build());
        list.add(WikiEntry.info("obsidiansentinel", BOSSES, Material.ARMOR_STAND, NamedTextColor.DARK_GRAY,
                        "The Obsidian Sentinel", "The Obsidian Sentinel")
                .about("The final boss: a giant knight with a spear and a shield of obsidian.",
                        "El jefe final: un caballero gigante con lanza y escudo de obsidiana.")
                .source(WikiSource.of(Material.RED_CANDLE, "Invocation Circle", "Círculo de Invocación",
                        "In the Boss Dimension, light a 5x5 ring of 12 red candles and drop an Echo Shard in the middle.",
                        "En la Dimensión de Jefes, enciende un anillo de 5x5 con 12 velas rojas y suelta un Fragmento "
                                + "de Eco en el centro."))
                .source(WikiSource.of(Material.NETHER_STAR, "Loot", "Botín",
                        "The Sentinel Core.", "El Sentinel Core."))
                .build());
        list.add(WikiEntry.info("nix", BOSSES, Material.NETHERITE_AXE, NamedTextColor.DARK_RED,
                        "NIX - The Executioner", "NIX - The Executioner")
                .about("A hooded executioner with chains and a great axe.",
                        "Un verdugo encapuchado con cadenas y una gran hacha.")
                .source(WikiSource.of(Material.ANVIL, "The Executioner's Scaffold", "El Patíbulo del Verdugo",
                        "In the Boss Dimension: an anvil, 4 lit red candles beside it and 4 gallows in the corners "
                                + "(base, chain, skull). Right-click a candle, then drop an Execution Warrant on the anvil.",
                        "En la Dimensión de Jefes: un yunque, 4 velas rojas encendidas a su lado y 4 horcas en las "
                                + "esquinas (base, cadena, calavera). Clic derecho en una vela y suelta un Execution "
                                + "Warrant sobre el yunque."))
                .source(WikiSource.of(Material.NETHERITE_SCRAP, "Loot", "Botín",
                        "Executioner's Edges.", "Executioner's Edge."))
                .build());
        list.add(WikiEntry.info("dio", BOSSES, Material.CLOCK, NamedTextColor.GOLD,
                        "DIO", "DIO")
                .about("DIO and The World. He stops time, throws knives and drops a road roller.",
                        "DIO y The World. Detiene el tiempo, lanza cuchillos y deja caer una apisonadora.")
                .source(WikiSource.of(Material.GOLD_BLOCK, "The World's Throne", "El Trono de The World",
                        "In the Boss Dimension: a gold block, 4 lit yellow candles beside it and 4 pillars in the "
                                + "corners (gold block, emerald block, a head). Right-click a candle, then drop a Clock.",
                        "En la Dimensión de Jefes: un bloque de oro, 4 velas amarillas encendidas a su lado y 4 "
                                + "pilares en las esquinas (oro, esmeralda, una cabeza). Clic derecho en una vela y suelta "
                                + "un Reloj."))
                .source(WikiSource.of(Material.RED_DYE, "Loot", "Botín",
                        "Vampire Blood.", "Vampire Blood."))
                .build());
        list.add(WikiEntry.info("malenia", BOSSES, Material.TORCHFLOWER, NamedTextColor.RED,
                        "Malenia, Blade of Miquella", "Malenia, Blade of Miquella")
                .about("The undefeated Empyrean. Every hit she lands heals her, and below half health she unleashes "
                                + "the Waterfowl Dance.",
                        "La Empírea invicta. Cada golpe que acierta la cura, y por debajo de la mitad de vida desata "
                                + "la Danza del Ave Acuática.")
                .source(WikiSource.of(Material.NETHER_WART_BLOCK, "The Scarlet Bloom", "La Flor Escarlata",
                        "In the Boss Dimension: a nether wart block, 4 lit magenta candles beside it and 4 pillars in "
                                + "the corners (crimson hyphae, shroomlight). Right-click a candle, then drop a "
                                + "Torchflower on the heart.",
                        "En la Dimensión de Jefes: un bloque de verruga del Nether, 4 velas magenta encendidas a su lado "
                                + "y 4 pilares en las esquinas (hifas carmesí, brillongo). Clic derecho en una vela y "
                                + "suelta una Antorchaflor sobre el corazón."))
                .source(WikiSource.of(Material.EXPERIENCE_BOTTLE, "Loot", "Botín",
                        "500 experience.", "500 de experiencia."))
                .build());
        list.add(WikiEntry.info("jackstar", BOSSES, Material.COMMAND_BLOCK, NamedTextColor.DARK_AQUA,
                        "JackStar - The System Architect", "JackStar - The System Architect")
                .about("The architect of the server. He revives twice and calls other bosses as subprocesses.",
                        "El arquitecto del servidor. Revive dos veces e invoca a otros jefes como subprocesos.")
                .source(WikiSource.of(Material.LODESTONE, "The Multiverse Terminal", "La Terminal del Multiverso",
                        "In the Boss Dimension: a respawn anchor or lodestone, 4 lit cyan or light blue candles beside "
                                + "it and a lightning rod on each corner pillar. Offer an Architect Kernel, a Nether Star, "
                                + "an Echo Shard, a Heart of the Sea or a Beacon.",
                        "En la Dimensión de Jefes: un nexo de reaparición o una magnetita, 4 velas cian o azul claro "
                                + "encendidas a su lado y un pararrayos en cada pilar de esquina. Ofrece un Architect "
                                + "Kernel, una Estrella del Nether, un Fragmento de Eco, un Corazón del Mar o un Faro."))
                .source(WikiSource.of(Material.NETHER_STAR, "Loot", "Botín",
                        "The Architect Kernel.", "El Architect Kernel."))
                .build());
        list.add(WikiEntry.info("mahoraga", BOSSES, Material.ZOMBIE_HEAD, NamedTextColor.WHITE,
                        "Mahoraga", "Mahoraga")
                .about("The Divine General. He adapts to whatever hurts him.",
                        "El General Divino. Se adapta a todo lo que le hace daño.")
                .source(WikiSource.of(Material.ZOMBIE_SPAWN_EGG, "Where", "Dónde",
                        "Very rarely rises in place of a Zombie in the overworld.",
                        "Muy rara vez aparece en lugar de un Zombi en el mundo normal."))
                .source(WikiSource.of(Material.NETHERITE_SCRAP, "Loot", "Botín",
                        "Wheel Essence.", "Wheel Essence."))
                .build());
        list.add(WikiEntry.info("garou", BOSSES, Material.WITHER_SKELETON_SKULL, NamedTextColor.DARK_PURPLE,
                        "Garou", "Garou")
                .about("The Hero Hunter, a martial artist who fights barehanded.",
                        "El Cazador de Héroes, un artista marcial que pelea con las manos desnudas.")
                .source(WikiSource.of(Material.WITHER_SKELETON_SPAWN_EGG, "Where", "Dónde",
                        "Very rarely takes the place of a Wither Skeleton.",
                        "Muy rara vez aparece en lugar de un Esqueleto Wither."))
                .source(WikiSource.of(Material.NETHER_STAR, "Loot", "Botín",
                        "The Garou Cosmic Core.", "El Garou Cosmic Core."))
                .build());
        list.add(WikiEntry.info("witherstorm", BOSSES, Material.WITHER_ROSE, NamedTextColor.DARK_PURPLE,
                        "Wither Storm", "Wither Storm")
                .about("A Wither that swallowed a command block. It eats the world to grow, through five forms, each "
                                + "one bigger and stronger than the last.",
                        "Un Wither que se tragó un bloque de comandos. Se come el mundo para crecer, a través de cinco "
                                + "formas, cada una más grande y fuerte que la anterior.")
                .source(WikiSource.of(Material.CRYING_OBSIDIAN, "Summoning", "Invocación",
                        "Only in the Boss Dimension: build a Wither (a T of 4 soul sand and 3 wither skeleton skulls) "
                                + "with its foot on crying obsidian. The Wither is swallowed and the storm forms in its place.",
                        "Solo en la Dimensión de Jefes: construye un Wither (una T de 4 de arena de almas y 3 calaveras "
                                + "de esqueleto wither) con su base sobre obsidiana llorosa. El Wither es tragado y la "
                                + "tormenta se forma en su lugar."))
                .source(WikiSource.of(Material.OBSIDIAN, "Five forms", "Cinco formas",
                        "The Hunchback, the Growing Hunchback, the Swollen Hunchback, the Destroyer and the Devourer. "
                                + "It evolves by eating blocks, mobs and players.",
                        "El Jorobado, el Jorobado Creciente, el Jorobado Hinchado, el Destructor y el Devorador. "
                                + "Evoluciona comiendo bloques, criaturas y jugadores."))
                .source(WikiSource.of(Material.PURPLE_STAINED_GLASS, "Tractor beam", "Rayo tractor",
                        "A thin beam is the warning; then it pulls you in and hurts you the whole time, and "
                                + "Regeneration does not heal you while it holds you. Run sideways across it to break free.",
                        "Un rayo fino es el aviso; luego te arrastra y te hace daño todo el tiempo, y la Regeneración "
                                + "no te cura mientras te sujeta. Corre de lado a través de él para soltarte."))
                .source(WikiSource.of(Material.ARROW, "Weak spots", "Puntos débiles",
                        "Projectiles that hit a head injure it: it takes more damage and leaves you alone for a while. "
                                + "With every head injured the storm is exposed.",
                        "Los proyectiles que dan en una cabeza la hieren: recibe más daño y te deja en paz un rato. "
                                + "Con todas las cabezas heridas la tormenta queda expuesta."))
                .source(WikiSource.of(Material.BOW, "Arrows hurt it most", "Las flechas le duelen más",
                        "An arrow has its own cap of 150 damage instead of 10, and then deals twice that: up to 300 "
                                + "from a single arrow.",
                        "Una flecha tiene su propio tope de 150 de daño en vez de 10, y luego hace el doble: hasta 300 "
                                + "de una sola flecha."))
                .source(WikiSource.of(Material.DIAMOND_SWORD, "Swords", "Espadas",
                        "Melee hits stay capped at 10, and a sword loses 2% for every 5 levels of Sharpness, 50% at "
                                + "most.",
                        "Los golpes cuerpo a cuerpo siguen con tope de 10, y una espada pierde un 2% por cada 5 niveles "
                                + "de Filo, un 50% como máximo."))
                .source(WikiSource.of(Material.NETHER_STAR, "Loot", "Botín",
                        "One Nether Star and the Wither Storm Head.", "Una Estrella del Nether y la Wither Storm Head."))
                .build());
        list.add(WikiEntry.info("witherstormevent", BOSSES, Material.CRYING_OBSIDIAN, NamedTextColor.LIGHT_PURPLE,
                        "Wither Storm Event", "Wither Storm Event")
                .about("The Wither Storm of the server's events: 25% more health, it grows 30% faster and it has five "
                                + "destructive attacks of its own. When it falls, the chat ranks who hurt it most.",
                        "El Wither Storm de los eventos del servidor: 25% más de vida, crece un 30% más rápido y tiene "
                                + "cinco ataques destructivos propios. Cuando cae, el chat muestra quién le hizo más daño.")
                .source(WikiSource.of(Material.CLOCK, "When", "Cuándo",
                        "Only during server events, in the Boss Dimension. Everything on the Wither Storm's page holds "
                                + "for it too.",
                        "Solo durante los eventos del servidor, en la Dimensión de Jefes. Todo lo de la página del "
                                + "Wither Storm vale también para él."))
                .source(WikiSource.of(Material.FIRE_CHARGE, "Void Meteors", "Void Meteors",
                        "Red circles appear round you and a meteor crashes into each. Step out of the circles.",
                        "Aparecen círculos rojos a tu alrededor y cae un meteoro en cada uno. Sal de los círculos."))
                .source(WikiSource.of(Material.TNT, "Rift Trench", "Rift Trench",
                        "Red sparks run along the ground towards you, then it blows open along that line. Move sideways.",
                        "Unas chispas rojas avanzan por el suelo hacia ti y luego revienta a lo largo de esa línea. "
                                + "Apártate hacia un lado."))
                .source(WikiSource.of(Material.CRACKED_DEEPSLATE_BRICKS, "Crumbling Quake", "Crumbling Quake",
                        "A ring of collapse rolls outward from under the storm, tearing up the ground. Run from it.",
                        "Un anillo de derrumbe se expande desde debajo de la tormenta y arranca el suelo. Huye de él."))
                .source(WikiSource.of(Material.WITHER_SKELETON_SKULL, "Skull Nova", "Skull Nova",
                        "Three waves of blue wither skulls fly out in every direction. Hide behind solid blocks.",
                        "Tres oleadas de calaveras wither azules salen en todas direcciones. Escóndete tras bloques sólidos."))
                .source(WikiSource.of(Material.END_ROD, "Annihilation Beam", "Annihilation Beam",
                        "It stops, charges and sweeps a beam across the ground through you. Run the other way.",
                        "Se detiene, carga y barre el suelo con un rayo que pasa por ti. Corre hacia el otro lado."))
                .source(WikiSource.of(Material.GOLD_INGOT, "Ranking", "Ranking",
                        "When it falls, the whole server sees the top 3 teams (their members' damage added up) and the "
                                + "top 3 players by damage dealt to it.",
                        "Cuando cae, todo el servidor ve el top 3 de equipos (el daño de sus miembros sumado) y el top 3 "
                                + "de jugadores por daño hecho al jefe."))
                .source(WikiSource.of(Material.WHITE_BANNER, "Teams: /mscevent team", "Equipos: /mscevent team",
                        "With no team: create <name>, join <team> (once invited), list, info. As a member: leave. As "
                                + "the leader: add <player> to invite, remove <member>, leader <member> to hand the team "
                                + "over, disband. If the leader leaves, the next member leads.",
                        "Sin equipo: create <nombre>, join <equipo> (si te invitaron), list, info. Como miembro: leave. "
                                + "Como líder: add <jugador> para invitar, remove <miembro>, leader <miembro> para ceder el "
                                + "liderazgo, disband. Si el líder se va, lidera el siguiente miembro."))
                .build());
        list.add(WikiEntry.info("pantheonaltar", BOSSES, Material.CHISELED_QUARTZ_BLOCK, NamedTextColor.YELLOW,
                        "The Pantheon Altar", "El Altar del Panteón")
                .about("Calls the gods of DrakesBosses into the Boss Dimension. It only works with DrakesBosses "
                                + "installed.",
                        "Llama a los dioses de DrakesBosses a la Dimensión de Jefes. Solo funciona con DrakesBosses "
                                + "instalado.")
                .source(WikiSource.of(Material.QUARTZ_PILLAR, "The altar", "El altar",
                        "5x5: a core block in the middle, 4 candles round it and a 2-block pillar on each corner, all "
                                + "of the pantheon's kind.",
                        "5x5: un bloque núcleo en el centro, 4 velas a su alrededor y un pilar de 2 bloques en cada "
                                + "esquina, todos del tipo del panteón."))
                .source(WikiSource.of(Material.WHITE_CANDLE, "Olimpo", "Olimpo",
                        "White candles, chiseled quartz core, quartz pillars.",
                        "Velas blancas, núcleo de cuarzo cincelado, pilares de cuarzo."))
                .source(WikiSource.of(Material.GREEN_CANDLE, "Asgard", "Asgard",
                        "Green candles, chiseled deepslate core, spruce log pillars.",
                        "Velas verdes, núcleo de pizarra profunda cincelada, pilares de tronco de abeto."))
                .source(WikiSource.of(Material.ORANGE_CANDLE, "Duat", "Duat",
                        "Orange candles, chiseled sandstone core, smooth sandstone pillars.",
                        "Velas naranjas, núcleo de arenisca cincelada, pilares de arenisca lisa."))
                .source(WikiSource.of(Material.PURPLE_CANDLE, "The Void", "El Vacío",
                        "Purple candles, end stone bricks core, purpur pillars.",
                        "Velas moradas, núcleo de ladrillos de piedra del End, pilares de púrpur."))
                .source(WikiSource.of(Material.LIGHTNING_ROD, "The offering", "La ofrenda",
                        "Light the candles, right-click one and drop the god's offering on the core. The chat lists the "
                                + "offerings each altar takes.",
                        "Enciende las velas, haz clic derecho en una y suelta la ofrenda del dios sobre el núcleo. El "
                                + "chat muestra las ofrendas que acepta cada altar."))
                .build());
        list.add(WikiEntry.info("monster", BOSSES, Material.SKELETON_SKULL, NamedTextColor.WHITE,
                        "The Monster", "El Monster")
                .about("A tall, pale thing that waits in the dark where you are not looking. It does no damage: it "
                                + "is there to scare you.",
                        "Una cosa alta y pálida que espera en la oscuridad donde no miras. No hace daño: está ahí para "
                                + "asustarte.")
                .source(WikiSource.drop(Material.BLACK_CANDLE, "Where", "Dónde",
                        "In the dark: behind you, on the ceiling, on a chest or in a tight hole. If you hear breathing "
                                + "nearby, it is already there.",
                        "En la oscuridad: a tu espalda, en el techo, sobre un cofre o en un hueco estrecho. Si oyes "
                                + "respirar cerca, ya está ahí.",
                        "entities.monster.chance", 0.04))
                .source(WikiSource.of(Material.SOUL_TORCH, "Before it comes", "Antes de que llegue",
                        "For half a minute: steps behind you that stop when they are close, a door somewhere near, the "
                                + "torches round you going out one by one, and maybe a glimpse of it at the edge of your "
                                + "sight, gone before you can look at it. Only you hear and see any of it.",
                        "Durante medio minuto: pasos a tu espalda que se paran cuando están cerca, una puerta cerca, las "
                                + "antorchas a tu alrededor apagándose una a una y quizá un vistazo de él al borde de tu "
                                + "vista, que desaparece antes de que puedas mirarlo. Solo tú lo oyes y lo ves."))
                .source(WikiSource.of(Material.ENDER_EYE, "If you see it", "Si lo ves",
                        "Its head turns slowly to follow you. Then it comes for you, your sight pulsing dark, and its "
                                + "face ends up in yours, screaming. Then it is gone.",
                        "Su cabeza gira despacio para seguirte. Luego va a por ti, con tu vista latiendo a oscuras, y su "
                                + "cara acaba en la tuya, gritando. Luego desaparece."))
                .source(WikiSource.of(Material.OAK_DOOR, "If it leaves", "Si se va",
                        "Left unseen too long, it goes without a sound, but leaves its footprints behind for a while, "
                                + "and a door near where it waited may be standing open.",
                        "Si nadie lo ve durante mucho rato, se va sin un sonido, pero deja sus huellas un tiempo, y una "
                                + "puerta cerca de donde esperaba puede quedar abierta."))
                .build());
    }
}
