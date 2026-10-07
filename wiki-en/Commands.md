# 🛠️ Commands

All commands use the **`/msc`** root. **Permission:** `msc.admin` (server OP by default). `msc.admin.bypass` (also OP) skips the protected boss-dimension build and teleport rules.

```
/msc                           Show structured, categorized help menu
/msc spawn <type>              Summon a mob, boss, military strike, etc. (/msc spawn help [1-3])
/msc give <item> [amount] [player]  Give legendary weapons, armor or components (/msc give help [1-4])
/msc attack <name> [range]     Trigger an ArmorStandBoss attack/mechanic (/msc attack help [1-7])
/msc music <play|stop|list|disc>  Play / stop NBS songs, get a jukebox disc
/msc dimtp <world>             Teleport across worlds
/msc kill [type|all] [radius]  Safely kill/purge MSC custom creatures
/msc debug [player]            Break down each boss's damage to and from a player
/msc debug geometry [boss]     Draw a boss's hitbox and joints, or replay its walk
/msc reload                    Reload config.yml, merge new defaults and sync entities and bosses
```

Each command is detailed below.

`spawn` and `give` accept exactly one name per entity or item: the one in the tables below, which is also what tab completion offers. The names accepted by `spawn`, `give` and `attack` are declared in a single table per command (`SpawnCatalogue`, `GiveCatalogue` and `AttackCatalogue`): the same table feeds the executor, the help menu and the completer.

---

## /msc spawn <type>

Summons a single entity (or a tactical formation) at the executor's location. The following types are supported:

| Type | Entity |
|---|---|
| `merchant` | Multiverse Merchant ("Shaggy" Wandering Trader) |
| `mahoraga` | Mahoraga miniboss |
| `kinger` | Kinger miniboss |
| `garou` | Garou [Hero Hunter] miniboss |
| `nix` | NIX - The Executioner (custom 27-part model boss) |
| `dio` | DIO with his Stand The World (JoJo's Bizarre Adventure boss) |
| `witherstorm` | THE WITHER STORM, born out of its vortex in its first form (Cracker's Wither Storm Mod) |
| `witherstorm2` … `witherstorm5` | THE WITHER STORM already grown: Growing Hunchback, Swollen Hunchback, Destroyer, Devourer |
| `armorstand` | THE OBSIDIAN SENTINEL final boss |
| `jack` | JACK STAR — The System Architect (5 phases, 3 lives) |
| `creeperjr` | Creeper Jr. (×3 — spawns in trio) |
| `headslime` | Head Slime |
| `zombietrap` | Military Zombie Horse trap (full 5-unit army ambush) |
| `tank` | Zombie Tank (single unit) |
| `duelist` | Military Skeleton Duelist |
| `lancer` | Zombie Lancer + ZombieHorse |
| `camel` | Army Camel with riders |
| `sniper` | Sniper Skeleton |
| `boneshield` | Bone Shield |
| `chaosmage` | Chaos Mage |
| `enderknight` | Ender Knight |
| `flameelemental` | Flame Elemental |
| `frostgolem` | Frost Golem |
| `obsidianguard` | Obsidian Guard |
| `shadowrogue` | Shadow Rogue |
| `soulreaper` | Soul Reaper |
| `stormcaller` | Storm Caller |
| `venomwitch` | Venom Witch |
| `voidcrawler` | Void Crawler |
| `arrowskeleton` | Archer of the Arrow (Stand Arrow) |
| `warlord` | Orcish Warlord (berserker rage) |
| `disctrader` | Disc Trader — librarian villager selling music discs |

Details for each entity live in [Bosses](./Bosses.md) and [Creatures](./Creatures.md).

---

## /msc give <item> [amount] [player|@a|@p|@r|@s]

Amount defaults to 1 and can be 1–64. Without a target the item goes to the executor; otherwise it accepts an exact player name or a selector (`@e` is rejected on purpose).

### Weapons

| Item |
|---|
| `excalibur` |
| `cindergreatsword` |
| `nullshearedge` |
| `soulreapscythe` |
| `venomfang` |
| `aetherpullshot` |
| `skyfiretalisman` |
| `sentinelgrimoire` |
| `chaosforge` |
| `executionerguillotine` |
| `architectdeployer` |

### Armor & relics

| Item |
|---|
| `eighthandledwheel` |
| `obsidianbastionhelmet` |
| `obsidianbastionchestplate` |
| `obsidianbastionleggings` |
| `obsidianbastionboots` |
| `marrowaegis` |
| `veilwalkermantle` |
| `frostheartoffhand` |

### Misc items

| Item |
|---|
| `icecrown` |
| `mantisclaws` |
| `wirtslantern` |
| `militarymine` |
| `scoobycookie` |
| `headslimegelatin` |
| `vampireblood` |
| `unstableblood` |
| `bearerelixir` |

### Components

| Item |
|---|
| `starcore` |
| `militarycomponent` |
| `swordmold` |
| `headslimeheart` |
| `chaosorb` |
| `chaospowder` |
| `chaosfragment` |
| `chaoscore` |
| `condensedchaosorb` |
| `enderfragment` |
| `frostheart` |
| `magmacore` |
| `obsidianshard` |
| `reaperessence` |
| `reinforcedbone` |
| `reinforcedboneblock` |
| `bonemarrow` |
| `ossifiedplate` |
| `moltenmarrow` |
| `shadowcloak` |
| `stormcrystal` |
| `venomgland` |
| `voidessence` |
| `wheelessence` |
| `executioneredge` |

### Boss catalysts & core blocks

| Item |
|---|
| `wheelcore` |
| `moltenwheelcore` |
| `refinedwheelcore` |
| `reapercore` |
| `sentinelcore` |
| `endercore` |
| `multiversalcore` |
| `compressedgoldblock` |
| `refinednetherite` |
| `moltennetherite` |
| `executionerwarrant` |
| `architectkernel` |
| `garoucosmiccore` |

---

## /msc attack <name> [range]

Forces an attack or mechanic of the nearest Obsidian Sentinel within `range` blocks (default 100). Every attack and mechanic has **one name** — the ones below, which `/msc attack help [1-7]` lists and tab completion offers.

The command skips the AI's distance, cooldown and repetition rules, but keeps the ones that protect the boss: nothing starts while another attack is still running, an aerial attack needs the boss in the air (`flyup` first) and a ground one on the floor (`land` first), and a seal, healing circle, defence or full minion cap that is already up is not stacked. When it refuses, it says why.

### Ground attacks (24)

`groundslam`, `groundshatter`, `shieldbash`, `lancestorm`, `earthpillar`, `chaingrapple`, `warstomp`, `armorspikes`, `vortexpull`, `mirrorimage`, `doombeam`, `lanceflurry`, `whirlwindslash`, `executionsweep`, `obsidianspire`, `earthmaw`, `shadowstep`, `runeward`, `sunderingcharge`, `spearcyclone`, `cataclysm`, `tremorlance`, `aegisrush`, `gravecleaver`

### Aerial attacks (20)

`starfall`, `aerialrush`, `sonicboom`, `lightningstorm`, `gravitywell`, `crossslash`, `novaburst`, `darkorb`, `windcutter`, `heavenlyjudgment`, `rainoflances`, `airslam`, `hoverbarrage`, `eclipsefall`, `bladering`, `obsidianwings`, `voidmeteor`, `phantomlegion`, `spiralstorm`, `chainhook`

### Ranged & magic attacks (20)

`lancesnipe`, `meteorstorm`, `voidbeam`, `frostlance`, `lightningspear`, `shadowvolley`, `chainlightning`, `crystalbarrage`, `arcaneorb`, `voidrift`, `arcanemissiles`, `spiritbeam`, `soultethers`, `plaguebrand`, `runemines`, `obsidianprison`, `shardburst`, `gravityorb`, `javelinvolley`, `sweepinglaser`

### Defences, seals & heals (12)

`stoneskin`, `reflectbarrier`, `absorbshield`, `shieldseal`, `healingcircle`, `trianglecall`, `regeneration`, `soulsiphon`, `obsidiancocoon`, `bulwark`, `thornaura`, `afterimage`

### Summoning rites (10)

`lancesquires`, `obsidianmender`, `emberhounds`, `voidwisps`, `obsidianbrute`, `elementalconclave`, `shadowambush`, `necropolisrite`, `arcanecovenant`, `championcall`

### Destructive cataclysms (10)

`orbitalstrike`, `meteorimpact`, `supernova`, `judgmentpillars`, `earthsplitter`, `voidcollapse`, `obsidiantsunami`, `solarlance`, `worldbreaker`, `apocalypserain`

### Mechanics & phase transitions (7)

`flyup`, `land`, `reset`, `phaserage`, `phasebarrier`, `phasestorm`, `phasedespair`

The full list and details are on the [Bosses wiki page](./Bosses.md).

---

## /msc debug [player]

Diagnoses boss damage. Without an argument it targets the player you are looking at (within 30 blocks); you can also name a player, which is the only way to use it from the console.

The report has one section per boss (the Obsidian Sentinel, Nix and Jack Star) and reads the **most recent sample** for that player in both directions, so let the boss hit them (or hit the boss) once first:

- **DEALT to player** — the attack, the damage it asked for (`Intended`) and what the player actually took (`Applied`), after their armour and effects.
- **TAKEN from player** — the hit as it arrived (`Hit`), the boss's own mechanic (`cap N`, the load-balancer split, the defence multipliers, a dodge…), and the damage the boss applied.

The Sentinel's `DEALT` entry is the **penetrating breakdown** (event damage, the armour/Protection/Resistance credited back, the per-hit cap, the Resistance pierce), because every hit it lands goes through that pipeline. Nix's `TAKEN` entry shows its `max-damage-per-hit` cap; Jack Star's shows the Load Balancer split.

```
OBSIDIAN SENTINEL
  ▸ DEALT to player · penetrating
      Event : 3.52 · armour : -17.60 · protection : 0.00 · resistance : -0.88
      Through armour : 22.00 · cap : 15.00 · pierce : 20% · Resistance : level 1
      Final damage dealt : 12.60
  ▸ TAKEN from player · Incoming hit
      Hit : 120.00 · stone skin ×0.5, cap 50.0 · Applied : 50.00

NIX - THE EXECUTIONER
  ▸ DEALT to player · Guillotine Cleave
      Intended : 22.00 · Applied : 9.90
  ▸ TAKEN from player · Incoming hit
      Hit : 300.00 · cap 100.0 · Applied : 100.00

JACK STAR - THE SYSTEM ARCHITECT
  ▸ DEALT to player · Three-Slash
      Intended : 16.00 · Applied : 8.40
  ▸ TAKEN from player · Incoming hit
      Hit : 134.00 · load balancer: 46.9 shared · Applied : 87.10
```

Sections with nothing recorded are skipped; if no boss has touched the player yet the command says so instead of printing an empty breakdown. The samples are transient: a player's are dropped when they disconnect and a boss's when it dies or despawns, so the report always reflects the current session. The caps and multipliers live in `entities.<boss>.*` in `config.yml`; see [Installation](./Installation.md) and [Bosses](./Bosses.md) for what each knob does.

---

## /msc debug geometry [kinger|nix|jack|sentinel] [walk]

Draws a boss's **real hitbox** and the **joints its limbs swing around** in the world for ten seconds, following it as it moves. It targets the nearest boss of that kind within 32 blocks (omit the name for any dressed boss); the geometry is drawn with particles, so nothing is spawned and nothing is left behind.

- **Red** traces the twelve edges of the armour stand's bounding box — the box that actually takes the hits. Every visible piece has to be **inside** it, or a swing at that piece misses.
- **Cyan** marks each joint: shoulder, hip, waist, neck and — for NIX and Jack Star, whose arms and legs were exported in two segments — the **elbows and knees**. A limb has to hang from its dot, and the limb's far end has to stay the same distance from it while it swings.

The Sentinel has no joints of its own (it wears its armour on the stand), so it only draws the box. This is the in-game counterpart of the model tests: it exists so a model can be checked next to the boss instead of through a throwaway unit test.

Append **`walk`** (e.g. `/msc debug geometry kinger walk`) to replay a model's **walk cycle** instead of drawing a live boss: the same red hitbox, plus the limb skeleton — cyan pivots and joints, blue bones, green hands and feet — stepping in place on a rig two and a half blocks in front of you, at the pace the boss itself walks. No boss is spawned or provoked, so the knee and elbow folding can be judged on its own.

```
Drawing kinger for 10 s: hitbox 0.50 x 1.98 x 0.50 blocks (red), 8 joints (cyan).
Replaying kinger's walk in front of you for 10 s: hitbox (red), joints (cyan), bones (blue), hands and feet (green). Nothing was spawned.
```

---

## /msc music <play|stop|list|disc> [song] [loop]

Plays any `.nbs` file from `plugins/MultiverseCreatures/music/`. Songs are played via the `MusicManager` (note-block-stub protocol packets) to all nearby players within a configurable radius.

```
/msc music list                List all songs in the music folder
/msc music play Undertale-Megalovania true   Play (loop=true)
/msc music stop                 Stop current song
/msc music disc Megalovania     Give yourself the jukebox disc of a song
```

`/msc music disc <song>` gives the matching jukebox disc — insert it in a jukebox to play the song, right-click with an empty hand to eject it. See [Music](./Music.md) for the bundled songs, credits and the Disc Trader.

---

## /msc dimtp <world>

Teleports the executor across worlds/dimensions. Used for testing the boss dimension scaffolding and for quickly jumping between overworld/nether/the_end.

---

## /wiki [page|hand|en|es]

Opens the in-game wiki, for every player (`msc.wiki`, granted by default). Aliases: `/mscwiki`, `/mwiki`.

- Nine shelves: weapons, armor, relics, food and potions, creature drops, crafted components, boss loot, Stands and bosses.
- Each item page shows the item with its lore, its recipe as the server has it (crafting table, furnace, blast furnace, brewing stand or merchant trade), who drops it with the server's own chance, and what it is used in. Click an ingredient to open its page.
- English and Spanish: it opens in the client's language and the flag in the corner switches it (the choice is kept on the player).
- `/wiki venomfang` opens a page directly; `/wiki hand` opens the page of the item you hold; `/wiki es` or `/wiki en` switches the language.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `msc.admin` | OP only | Required for ALL `/msc` subcommands |
| *your own nodes* | — | Optional per-subcommand gates, declared under `commands.subcommand-permissions` |

There are no per-item or per-mob permissions yet. Server admins can gate the command behind a permission plugin (e.g. LuckPerms) by giving `msc.admin` only to trusted staff.

`commands.subcommand-permissions` maps a subcommand to one extra node. A subcommand named there needs its node **on top of** `msc.admin`; anything absent or blank stays open to whoever passed the main gate, so the shipped `{}` changes nothing:

```yaml
commands:
  permission: "msc.admin"
  op-only: true
  subcommand-permissions:
    debug: msc.debug
    attack: msc.attack
```
