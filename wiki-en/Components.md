# 🧪 Crafting Components

The plugin's loot system is intentionally **simple and thematic**: each mob drops a unique ingredient, and those ingredients are used to craft the legendary items. Drops happen on the mob's death and respect a `drop-chance` (in `config.yml`), which is rolled per-kill.

Drop sources — for each component, the mob and chance:

| Component | Source mob | Drop chance | Theme tag |
|---|---|---|---|
| **Wheel Essence** `§f§lWheel Essence` (NETHERITE_SCRAP) | Mahoraga | 75% | Multiverse (JJK) |
| **Chaos Orb** `§d§lChaos Orb` (NETHER_STAR) | Chaos Mage | 60% | Multiverse |
| **Ender Fragment** `§3§lEnder Fragment` (ENDER_PEARL) | Ender Knight | 55% | Multiverse |
| **Frost Heart** `§b§lFrost Heart` (BLUE_ICE) | Frost Golem | 75% | Multiverse |
| **Magma Core** `§6§lMagma Core` (MAGMA_CREAM) | Flame Elemental | 60% | Multiverse |
| **Storm Crystal** `§e§lStorm Crystal` (QUARTZ) | Storm Caller | 60% | Multiverse |
| **Venom Gland** `§2§lVenom Gland` (SPIDER_EYE) | Venom Witch | 60% | Multiverse |
| **Void Essence** `§5§lVoid Essence` (ENDER_EYE) | Void Crawler | 50% | Multiverse |
| **Reaper Essence** `§0§lReaper Essence` (SOUL_LANTERN) | Soul Reaper | 60% | Multiverse |
| **Reinforced Bone** `§f§lReinforced Bone` (BONE) | Bone Shield | 80% | Multiverse |
| **Bone Marrow** `§f§lBone Marrow` (BONE_MEAL) | crafted from **Reinforced Bone** + Redstone Blocks + Nether Wart | — | Multiverse |
| **Ossified Plate** `§f§lOssified Plate` (CALCITE) | crafted from **Bone Marrow** + Calcite + Diamond | — | Multiverse |
| **Molten Marrow** `§6§lMolten Marrow` (REDSTONE) | **Blast Furnace ONLY**: 1 Ossified Plate (100 ticks, 0.5 XP) | — | Multiverse |
| **Shadow Cloak Fragment** `§8§lShadow Cloak Fragment` (BLACK_WOOL) | Shadow Rogue | 50% | Multiverse |
| **Obsidian Shard** `§8§lObsidian Shard` (OBSIDIAN) | Obsidian Guard | 85% | Multiverse |
| **Head Slime Heart** `§a§lHead Slime Heart` (SLIME_BALL) | Head Slime | always (100%) | Slime Kingdom |
| **Military Component** `§a§lMilitary Component` (GUNPOWDER) | each ZombieHorseTrap unit | 30% (`zombie-horse-trap.military-component-drop-chance`) | Military |
| **Star Core** `§e§lStar Core` (NETHER_STAR) | special / "from a superior entity" | — | Multiverse |
| **Wheel Core** `§6§lWheel Core` (MUSIC_DISC_OTHERSIDE) | crafted from **Wheel Essence** + Diamond Block + Nether Star; **must be smelted in a Blast Furnace** | — | Multiverse (JJK) |
| **Molten Wheel Core** `§6§lMolten Wheel Core` (BLAZE_POWDER) | **Blast Furnace**: 1 Wheel Core (100 ticks, 0.5 XP) | — | Multiverse (JJK) |
| **Molten Netherite** `§8§lMolten Netherite` (ANCIENT_DEBRIS) | **Blast Furnace**: 1 Refined Netherite (100 ticks, 0.5 XP) | — | Multiverse |
| **Refined Wheel Core** `§6§lRefined Wheel Core` (MUSIC_DISC_OTHERSIDE) | crafted from **Molten Wheel Core** + **Molten Netherite** | — | Multiverse (JJK) |
| **Reaper Core** `§0§lReaper Core` (WITHER_ROSE) | crafted from **Reaper Essence** + Soul Sand + Nether Star | — | Multiverse |
| **Refined Netherite** `§8§lRefined Netherite` (NETHERITE_INGOT) | crafted from **4 Star Core** (corners) + **4 Netherite Scrap** (sides) + **Compressed Gold Block** (center, 9 Gold Blocks) | — | Multiverse |
| **Executioner's Warrant** `§4§lExecutioner Warrant` (PAPER) | crafted from Chains + Paper + Wither Skeleton Skull + Iron Ingot | — | Multiverse (NIX) |

Each component is just a `msc_<name>` tagged ingredient — it does **nothing on its own**, but it's required to craft the corresponding legendary item.

---

## Crafting chains (loot → item)

```
Mahoraga ─┬─► Wheel Essence ────► Eight-Handled Wheel (helmet)
Chaos Mage ─► Chaos Orb ────────► Chaos Forge (reforge tool)
Ender Knight ─► Ender Fragment ─► Aether Pullshot (trident)
Frost Golem ─► Frost Heart ─────► Frost Heart (off-hand)
Flame Elemental ─► Magma Core ──► Cinder Greatsword
Storm Caller ─► Storm Crystal ──► Skyfire Talisman
Venom Witch ─► Venom Gland ─────► Venomfang (dagger)
Void Crawler ─► Void Essence ───► Nullshear Edge
Soul Reaper ─► Reaper Essence ──► Soulreap Scythe
Bone Shield ─► Reinforced Bone ─► [Bone Marrow ─► Ossified Plate ─► *Blast Furnace (only)* Molten Marrow] ─► Marrow Aegis (shield)
Shadow Rogue ─► Shadow Cloak ──► Veilwalker Mantle (off-hand)
Obsidian Guard ─► Obsidian Shard► Obsidian Bastion (4-piece set)
Head Slime ─► Head Slime Heart ─► Head Slime Gelatin (food)
ZombieHorseTrap ─► Military Comp► Military Mine (camouflaged TNT)
Superior entity ─► Star Core ───► Excalibur (and beyond...)
Mahoraga ─► Wheel Essence ─► Wheel Core ─► [Blast Furnace] Molten Wheel Core ─► Refined Wheel Core ─► Eight-Handled Wheel (helmet)
Refined Netherite ─► [Blast Furnace] Molten Netherite ─► (mixed with Molten Wheel Core)
Soul Reaper ─► Reaper Essence ─► Reaper Core ─► Soulreap Scythe
Obsidian Guard ─► Obsidian Shard + Refined Netherite (4 Star Core + 4 Scrap + Compressed Gold Block) ─► Obsidian Bastion
Soul Reaper ─► Reaper Essence ─► Edelwood Sap ─► Edelwood Oil ─► Pottsfield Gourd ─► Beatrice's Feather ─► The Beast's Antler ─► (+ Edelwood Oil) Wirt's Lantern
```

---

## 🏮 Wirt's Lantern chain (Over the Garden Wall)

Five components from the Unknown, each crafted from the one before it. The last one, with one more flask of Edelwood Oil, buys **Wirt's Lantern** from the Multiverse Merchant (one per merchant). Every recipe is shaped (3×3, crafting table).

| # | Component (material) | Recipe | Hard part |
|---|---|---|---|
| 1 | **Edelwood Sap** (RESIN_CLUMP) | `L R L / R H R / L R L` — L = Pale Oak Log ×4, R = **Reaper Essence** ×4, H = **Creaking Heart** | Reaper Essence drops from Soul Reapers (60%); the Creaking Heart needs Silk Touch in a Pale Garden |
| 2 | **Edelwood Oil** (DRAGON_BREATH) | `T G T / T S T / T B T` — T = Ghast Tear ×6, G = Grindstone, S = **Edelwood Sap**, B = Glass Bottle | six Ghast Tears from the Nether |
| 3 | **Pottsfield Gourd** (CARVED_PUMPKIN) | `B K B / C O C / B K B` — B = Bone Block ×4, K = **Skeleton Skull** ×2, C = Carved Pumpkin ×2, O = **Edelwood Oil** | Skeleton Skulls only drop from skeletons killed by a charged creeper |
| 4 | **Beatrice's Feather** (FEATHER) | `L E L / E G E / L E L` — L = Lapis Block ×4, E = **Echo Shard** ×4, G = **Pottsfield Gourd** | Echo Shards come from Ancient City chests |
| 5 | **The Beast's Antler** (DEAD_BUSH) | `R S R / D F D / R S R` — R = Wither Rose ×4, S = **Nether Star** ×2, D = Dark Oak Log ×2, F = **Beatrice's Feather** | two Withers, and wither roses from mobs the Wither kills |
| — | **Wirt's Lantern** | Multiverse Merchant trade: **The Beast's Antler** + **Edelwood Oil** | a second Edelwood Sap and Oil |

In all: 8 Reaper Essences, 2 Creaking Hearts, 12 Ghast Tears, 2 Skeleton Skulls, 4 Echo Shards and 2 Nether Stars. Like every component they do nothing on their own and cannot be placed, brewed, smelted or used in vanilla recipes. Their lore footer is *The Unknown*.

---

## Give command (testing only)

You can hand yourself any component directly for testing:

```
/msc give chaosorb
/msc give enderfragment
/msc give frostheart
/msc give magmacore
/msc give obsidianshard
/msc give reaperessence
/msc give reinforcedbone
/msc give bonemarrow
/msc give ossifiedplate
/msc give moltenmarrow
/msc give shadowcloak
/msc give stormcrystal
/msc give venomgland
/msc give voidessence
/msc give wheelessence
/msc give headslimeheart
/msc give militarycomponent
/msc give starcore
/msc give wheelcore
/msc give moltenwheelcore
/msc give moltennetherite
/msc give refinedwheelcore
/msc give reapercore
/msc give refinednetherite
/msc give edelwoodsap
/msc give edelwoodoil
/msc give pottsfieldgourd
/msc give beatricefeather
/msc give beastantler
```

See [Commands](./Commands.md) for the full reference.
