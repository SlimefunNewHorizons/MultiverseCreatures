# 🐉 Bosses

MultiverseCreatures includes **one final boss** and multiple formidable **minibosses and custom-modeled bosses**. All bosses are spawned via `/msc spawn <type>` (OP-only) and have configurable health/damage/cooldowns in `config.yml`.

> **Abandoned bosses:** any boss (Sentinel, Nix, Jack, DIO, Kinger, Mahoraga, Garou, the Wither Storm and the Pantheon gods) with no player within `boss-balance.despawn.radius` (50 blocks; `large-radius` 100 for the Sentinel and the Wither Storm's colossal forms) for `delay-ticks` (100, 5 s) vanishes with everything it summoned, so an abandoned fight stops costing the server. Spectators do not count. `enabled: false` turns it off.

---

## 🛡️ THE OBSIDIAN SENTINEL — Final Boss

A gigantic 7.5×-scale animated ArmorStand. The climax of the plugin. Unlike the dressed bosses, the Sentinel **is** the stand, so its scale is at once the size of the model and the box players hit.

| Stat | Default |
|---|---|
| Health | `armor-stand-boss.health` (default 3200) |
| Hitbox | `armor-stand-boss.hitbox-scale` (7.5) — scale of the boss's own stand: model size and hitbox at once, about fourteen blocks of warrior (clamped to 0.25–8) |
| Boss bar | `SEGMENTED_6`, red → blue across the phases (see `phase-thresholds`) |
| Music | `Undertale — Megalovania` (60-block range, stops on death) |
| Equipment | Full Netherite (Amethyst/Silence trim) + Netherite Lance + unbreakable Shield |
| Summon | `/msc spawn armorstand` (alias `armorstandboss`) |

### Phases (transitions happen at HP thresholds)

| Phase | HP % | Transition effect |
|---|---|---|
| 0 — Red | >80% | Rage: knockback + Weakness I to nearby players, large pentagram seal |
| 1 — Purple | >60% | Barrier: invulnerable 100t, heals +30 HP, celestial seal |
| 2 — Yellow | >40% | Storm: 15 lightning strikes over 12-block radius, Slowness II + Weakness II |
| 3 — Green | >20% | Despair: invulnerable 80t, AoE damage ×1.5 + Darkness II + Blindness I + Slowness III |
| 4 — Blue | ≤20% | final phase |

The ladder is data, not code: `armor-stand-boss.phase-thresholds` holds the health fractions at which each next phase begins, highest first. The number of phases is one more than the entries in that list, and it is also how many squares the boss bar title shows — one per phase, red for the phases still to come and grey for the ones already spent. Entries outside `(0, 1]` are dropped and duplicates collapsed, so a typo cannot leave the boss with a single phase.

### AI behaviour

- **One attack clock** — every attack shares the same pause: `attack-interval-ticks` (50, 2.5 s) after the last one ends, the attack tick rolls the *kind* of attack with `attack-type-weights` (melee 30, ranged 20, flight 10, hover barrage 8, shield seal 8, Aegis Judgment 8, defence 12) and then an attack of that kind. An attack cannot come back until three others have been thrown, whatever their kind.
- **Flying mode** rolls between aerial attacks (`aerial`, 80) and ranged ones (`ranged`) on the same clock; after five different aerial ones (and 10–20 s in the air) it lands or dives down with AirSlam, and never stays up more than 40 s.
- **Behind the shield seal** it rolls between ranged attacks, Aegis Judgment and the Triangle Call (`summon`, 25).
- **One attack at a time** — while an attack is animating the AI neither turns him nor starts the next one; the cooldowns only count while he is free, so a long attack never eats the pause after it.
- **Defences** (below 90% HP, on ground, one state at a time): **Stone Skin** (×0.5 dmg taken), **Reflect Barrier** (×0.7 dmg + 30% reflect), **Absorb Shield** (100-HP absorber that visually shifts blue → red), **Bulwark** (×0.35 dmg, but rooted behind ramparts), **Thorn Aura** (×0.8 dmg, every hit stings the attacker for 3, anyone within 6 blocks is pricked each second) and **Afterimage** (35% of hits miss). Their durations come from `defense-duration-*-ticks`: stone skin 200, reflect barrier 160, absorb shield 300, bulwark 120, thorn aura 200, afterimage 160.
- **Healing defences** (below 70% HP, one at a time): **Healing Circle**, **Regeneration** (heals `regeneration-heal-percent` (5%) over 8 s while it keeps fighting), **Soul Siphon** (tethers up to three players within 16 blocks and drains them, healing four times what it takes; running 20 blocks away snaps a tether) and **Obsidian Cocoon** (invulnerable for 3 s inside obsidian pillars while it heals `obsidian-cocoon-heal-percent` (4%), then a shockwave).
- **Ground recovery** — a grounded boss only attacks while `isOnGround` is true. If it ends up with no solid block under it (void, water, a hole, a cliff edge), it hovers silently forever. After `ground-recovery-grace-ticks` (40) without ground it teleports to the nearest column with a floor and headroom, preferring the area around its current target and falling back to the world spawn, then resumes attacking with its cooldowns reset.
- **Despawn** — with nobody inside a 100-block radius (`boss-balance.despawn.large-radius`) for `delay-ticks` (100, 5 s) it despawns and cleans up its tasks, seals, music and boss bar. With `delay-ticks: 0` it leaves as soon as the arena empties.
- **Penetrating damage** — with `penetrating-damage: true` the boss's own hits bypass armour and Protection enchantments (they are re-applied as `OUT_OF_WORLD` damage, capped at `max-damage-dealt` (15) per hit). Resistance is only *partially* pierced: `penetrating-resistance-pierce: 0.2` makes the boss ignore 20% of the potion's mitigation, so a player with Resistance I (20% reduction) still blocks 16% of the hit. `0.0` leaves Resistance fully effective, `1.0` ignores it entirely. Use `/msc debug [player]` after a hit to see the whole breakdown (event damage, the reductions credited back, the pierce applied and the final value); the same command also reports what Nix and Jack Star deal to and take from that player.

### Special mechanics

- **Aegis Judgment** (`groundslam`) — one kind of the attack tick: he hurls his shield into the sky, where it turns and casts a burning pentagram under every player for two seconds; when his spear comes down, columns of light fall from the shield into every pentagram — `seal-damage` (15) in a 4-block radius + knock-up — and the shield drops back into his hand.
- **Wings** — burning wings built like real ones: a dark bone that leaves his back between the shoulder blades and rises through an elbow and a wrist, long flight feathers hanging from it that glow from red to orange towards their tips, and a shorter row of coverts. They beat slowly, the whole wing turning about its root.
- **Shield Seal** — six great shields of light wheel around him at chest height for 200 ticks, ×0.7 incoming damage; they fold back into one at the end.
- **Healing Circle** — he kneels in a circle of green runes for 200 ticks with threads of light rising into him, healing `healing-circle-heal-percent` (5%) of his max HP spread evenly over the whole kneel. Hurting him inside the circle is the counter.
- **Hover Barrage ("CrossBarrage")** — rises if grounded and fires volleys of X-shaped beams from the air, `hover-barrage-damage` (12) + knockback.
- **Triangle Call** — he plants the spear and raises two standing seals of fire; columns of light come down through them and reinforcements step out (scales with player count):
  - Air mode: Infernal Ghast + Night Stalker Phantom (carrying Sniper Skeleton with Power V Infinity bow).
  - Ground mode: War Beast Ravager (300 HP, 24 dmg) carrying a Dark Priest Evoker (40 HP, Speed I).
  - Summons are `MSC_ArmorBossSummoned` tagged; friendly-fire between the boss and its summons is disabled.

### Phase passives and the phase change

Each change of phase is a scene: the Sentinel buckles to one knee, power spirals up out of the ground, it rises roaring inside a pillar of light (untouchable until the climax), the phase's blast goes off and every player within 60 blocks sees the passive it just gained. Passives stack:

| Phase | Passive | Effect |
|---|---|---|
| 2 (≤80%) | **Fury** | attacks come 15% sooner |
| 3 (≤60%) | **Obsidian Hide** | 15% less damage taken |
| 4 (≤40%) | **Tempest** | attacks 15% sooner again; a telegraphed lightning bolt on a player every 10 s |
| 5 (≤20%) | **Undying Will** | regenerates 0.25% of its health a second, deals 20% more damage, destructive attacks twice as likely |

### Summoning rites (10)

Rolled as their own kind (`attack-type-weights.summoning`, 10). Minions carry the summon tag, never hurt the boss or each other, and dissolve when their time runs out or the Sentinel falls; no rite is cast while `max-summons` (8) are alive.

- **New creatures:** `lancesquires` (two obsidian squires that lunge with their lances), `obsidianmender` (heals the boss 0.4% a second until killed), `emberhounds` (three burning wolves that set you alight), `voidwisps` (three wisps that drift through walls and burst), `obsidianbrute` (a twice-sized brute that slams the ground every four seconds).
- **The plugin's own creatures:** `elementalconclave` (Flame Elemental, Frost Golem, Storm Caller), `shadowambush` (Shadow Rogues and Void Crawlers around the target), `necropolisrite` (Soul Reaper and two Bone Shields), `arcanecovenant` (Chaos Mage, Venom Witch, Ender Knight).
- **`championcall`** — once per phase, a gate opens and a random boss of the plugin steps through (NIX, DIO, Garou, Mahoraga or Kinger). It fights until it falls; if the Sentinel falls first the gate takes it back. JackStar is left out: his arrival calls in another boss.

### Destructive attacks (10)

Rolled as their own kind (`attack-type-weights.destructive`, 6), at least `destructive-gap-ticks` (400, 20 s) apart. Each has a long, loud charge — a targeting grid on the floor, a warning on every screen in range — and a blow of enormous reach. The world is never broken; craters are made of debris.

`orbitalstrike` (grid locks on, beams converge from orbit, red dome), `meteorimpact` (a house-sized meteor falling for five seconds), `supernova` (only the eye of the storm at its feet is safe), `judgmentpillars` (columns of light across the arena and under every player), `earthsplitter` (a cross of fissures torn thirty blocks out), `voidcollapse` (a black hole drags everyone in, then collapses), `obsidiantsunami` (a wall of obsidian rolls over the arena; find the gap), `solarlance` (a fourteen-block spear of sunlight), `worldbreaker` (leaps thirty blocks up and lands with three shockwaves), `apocalypserain` (meteors rain for five seconds).

### Animated attacks

Every attack is a **choreography**: a telegraph on the floor that says *where* (red to yellow as it heats up), a wind-up of the body that says *when*, the blow, and a recovery back to the guard. The Sentinel's arms, legs, head and body move through real poses computed from the armor stand model, so the spear tip, the shield face and the hands are where the effects come from. While an attack plays, it owns the body: the AI does not turn him or start another attack until it is done. Props (shields, spears, obsidian pillars, meteors) are display entities tagged `MSC_AttackProp` and are removed when the attack ends or the server restarts.

### Attack registry — 96 attacks total

All attacks are classes extending `ChoreographedAttack` under `entities/boss/attack/<aerial|ground|ranged|defensive|summon|destructive>/`, registered in `ArmorStandBoss.initAttacks()` and dispatched polymorphically via `attackRegistry.get(name).execute(instance)`. Trigger any one manually:

```
/msc attack <attack-name> [range]
```

All 96 names are listed by `/msc attack help` (six pages, one per category) and offered by tab completion. `/msc attack` also accepts the mechanics above (`flyup`, `land`, `heal`, `reset`, the four `phase*` transitions) plus `hoverbarrage`'s legacy alias `crossbarrage`.

| Ground (24) | Aerial (20) | Ranged (20) | Defensive (12) |
|---|---|---|---|
| groundslam | starfall | lancesnipe | stoneskin |
| groundshatter | aerialrush | meteorstorm | reflectbarrier |
| shieldbash | sonicboom | voidbeam | absorbshield |
| lancestorm | lightningstorm | frostlance | shieldseal |
| earthpillar | gravitywell | lightningspear | healingcircle |
| chaingrapple | crossslash | shadowvolley | trianglecall |
| warstomp | novaburst | chainlightning |  |
| armorspikes | darkorb | crystalbarrage |  |
| vortexpull | windcutter | arcaneorb |  |
| mirrorimage | heavenlyjudgment | voidrift |  |
| doombeam | rainoflances | arcanemissiles |  |
| lanceflurry | airslam | spiritbeam |  |
| whirlwindslash | hoverbarrage (crossbarrage) | soultethers |  |
| executionsweep | eclipsefall | plaguebrand | regeneration |
| obsidianspire | bladering | runemines | soulsiphon |
| earthmaw | obsidianwings |  | obsidiancocoon |
| shadowstep | voidmeteor | obsidianprison | bulwark |
| runeward | phantomlegion |  | thornaura |
| sunderingcharge |  |  | afterimage |
| spearcyclone | spiralstorm | shardburst |  |
| cataclysm | chainhook | gravityorb |  |
| tremorlance |  | javelinvolley |  |
| aegisrush |  | sweepinglaser |  |
| gravecleaver |  |  |  |

Additional `/msc attack` targets for **mechanics & phase transitions**: `flyup`, `land`, `heal`, `reset`, `phaserage`, `phasebarrier`, `phasestorm`, `phasedespair`.

### Second wave — ten more attacks and how to read them

Each of the ten additions is animated around one unmistakable tell, so a player can name the attack from the first second of the wind-up. Damage overrides live under `entities.armor-stand-boss.*-damage`.

| Attack | Category | Animation signature | Damage key (default) |
|---|---|---|---|
| `obsidianspire` | Ground | Both arms rise straight overhead and the body leans back, then a line of volcanic pillars erupts one by one along the facing. Each pillar throws its victim **upwards**, and an Executioner Cross is drawn along the path. | `obsidian-spire-damage` (11) |
| `earthmaw` | Ground | Kneeling crouch while two rows of teeth open over a 34° frontal cone, cracked ground from a Quake Seal, then the rows **snap shut** and drag everyone caught inside to the centre. | `earth-maw-damage` (9) |
| `shadowstep` | Ground | He hunches with his arms crossed, a pentagram swallows him and he is **gone** — the only teleporting attack. He reappears 1.35 blocks behind the target through a second sigil and spins 360° into a backstab burst. | `shadow-step-damage` (15) |
| `runeward` | Ground | A **kneeling cast** inside a large pentagram, then he plants an amethyst rune that stays behind. Every 25 ticks the rune throbs and pushes a ring of runes outward (Weakness + Slowness) for 150 ticks before shattering. | `rune-ward-damage` (4) |
| `eclipsefall` | Aerial | Both arms lift straight up while a **dark disc** forms three blocks overhead, ringed by a pentagram. The disc then falls with a comet trail and detonates as a black shockwave that blinds everyone nearby. | `eclipse-fall-damage` (16) |
| `bladering` | Aerial | Eight netherite lances materialise in a **visible ring orbiting his body** under a wing seal. He spins with the ring, then widens it and launches the lances one by one with homing flight. | `blade-ring-damage` (7) |
| `obsidianwings` | Aerial | Four wing panels grow out of his shoulders and unfold. Three **wing beats** sweep the panels down and throw expanding rings of obsidian shards outward; the wings then snap shut and both slam down together into a shockwave. | `obsidian-wings-damage` (8), `obsidian-wings-slam-damage` (15) |
| `soultethers` | Ranged | Both arms rise overhead and soul embers drain out of every nearby player into his chest. A glowing **tether stays drawn** from his chest to up to four players while they are dragged in and drained, and at the end it snaps taut and reels everyone in hard. | `soul-tether-damage` (3), `soul-tether-snap-damage` (13) |
| `plaguebrand` | Ranged | He spreads both arms with the palms open and a sickly green circle closes on the chosen victim. The mark lands as a spore burst, drains its host every second and **jumps to anyone standing next to them**; his head keeps tracking the marked player after the hit. | `plague-brand-hit-damage` (9), `plague-brand-damage` (4) |
| `runemines` | Ranged | A low arm sweep across the ground under a runic triangle, then six runes are **flung out around the target**. They arm for 25 ticks (dim, flat) before turning violet and pulsing, and burst upward when stepped on — or fade out on their own after 140 ticks. | `rune-mine-damage` (8) |

The random rotations pick them up as well: `obsidianspire`, `earthmaw`, `shadowstep` and `runeward` in the ground tables, `eclipsefall`, `bladering` and `obsidianwings` in the aerial ones, and `soultethers`, `plaguebrand` and `runemines` in the ranged table. The three ranged attacks fire both on the ground and in the air; the seven others require the matching state.

### Third wave — six more attacks

| Attack | Category | Animation signature | Damage key (default) |
|---|---|---|---|
| `sunderingcharge` | Ground | Spear point dropped to the floor, weight back, the lane glowing ahead; then a sprint that **rips a molten fissure** behind the tip. At the end a rising cut throws a fan of obsidian blades out of the ground, and a moment later the whole fissure erupts in fire. | `sundering-charge-damage` (14) |
| `spearcyclone` | Ground | He whirls the spear overhead faster and faster inside a growing spiral of wind, then sweeps it down: a **cyclone** rolls after the target, dragging and lifting anyone near it, and bursts at the end of its run. | `spear-cyclone-damage` (4 per hit, ×3 on the burst) |
| `cataclysm` | Ground | Ultimate: he drives the spear into the earth and three bands of the floor glow, with **safe rings** of dark ground between them. From the inside out each band bursts in walls of fire and obsidian spikes. Stand in the gaps. | `cataclysm-damage` (20) |
| `voidmeteor` | Aerial | Both arms raised while a boulder of crying obsidian forms over his head in a shell of void; he hurls it at the target, it crashes down with a shockwave and leaves a **void crater** that blinds and weakens. | `void-meteor-damage` (18) |
| `phantomlegion` | Aerial | Four **spectral copies** of himself climb out of the floor around the target. One after another each marks its line and lunges straight through the ring; the last two strike together. | `phantom-legion-damage` (11) |
| `obsidianprison` | Ranged | He points the spear at each player: a rune circle opens under them, then a **cage of obsidian spikes** bursts up around it, leaning in, and collapses on itself. Leave the circle before the bars close. | `obsidian-prison-damage` (16) |

They are in the random rotations too: `sunderingcharge` in the medium and far ground tables, `spearcyclone` and `cataclysm` in the close and medium ones, `voidmeteor` and `phantomlegion` in the aerial tables and `obsidianprison` in the ranged one.

### Drops

1000 XP on death, plus the "THE OBSIDIAN SENTINEL / Has been defeated!" title broadcast. Lightning + wither-death sound on death.

---

## ⚙️ Mahoraga — Miniboss (Jujutsu Kaisen)

"Eight-Handled Sword Divergent Sila Divine General" — adaptation made manifest.

| Stat | Default |
|---|---|
| Health | 250 |
| Spawn | `/msc spawn mahoraga` |
| Natural spawn | `mahoraga.spawn-chance` (2%) — replaces Zombies |

**Adaptation logic (per-tick scan of target's armor & weapons):**

| Target's attribute | Mahoraga gains |
|---|---|
| Diamond/Netherite armor + Protection levels | scaled **Attack Damage** bonus |
| Slimefun / Tinker armor (`mahoraga.slimefun-adaptation`, soft-dependency) | **Attack Damage** bonus by material tier (0.3 soft → 3.0 Singularities/Infinity) |
| Full Infinity Singularity Mail Links set (`mahoraga.instakill-infinity-armor`) | **Instant kill** — pierces the "Infinite Defence" trait (damage = 1) |
| Tinker **Diamond** modification on the held weapon (`mahoraga.ignore-diamond-mod`) | **Always ignored** — the reflect-and-cancel of each hit never applies |
| Tinker sword with **Infinity Singularity** material (`mahoraga.infinity-weapon-adaptation`) | Mahoraga only takes **1 damage** per hit |
| Total Protection > 5 (Diamond/Netherite) | **Strength** amplifier = total/5 |
| Max Sharpness **or Smite** total on any weapon (5 levels per rank) | **Resistance** level (max 4) = floor((Sharpness + Smite)/5) |
| Knockback enchantments | **Knockback Resistance** = totalKnockback × 0.3 |
| Target > 4 blocks away | **Speed I** for 30 ticks |
| Target close | Speed removed |

Outfit: white stained-glass helmet + white leather armor (unbreakable).

**MSC friendly-fire protection:** if both damager and target carry any `MSC_` scoreboard tag, the damage event is cancelled — Mahoraga cannot harm other MSC mobs (and vice versa).

**Drops:** 75% chance **Wheel Essence**. 150 XP. Custom player-death messages via `mahoraga.death-messages`.

---

## ♟️ Kinger — Miniboss (The Amazing Digital Circus)

A living chess king: an invisible ArmorStand dressed in a 15-piece ItemDisplay suit — a wooden head (neck, face, crown and cross) with two eyes, a purple robe in two pieces, two sleeved arms and two legs that fold at the knee. The skeleton is hierarchical: the legs swing from the hips, the torso leans about the hip line and carries the head and both arms with it, and the head turns about the neck with the eyes and crown as one piece. The stand is unscaled — a plain 0.5 × 1.975-block hitbox that covers the body; only the small hands poke a few centimetres out of the sides.

| Stat | Default |
|---|---|
| Health | `kinger.health` (120) |
| Hitbox | `kinger.hitbox-scale` (1.0) — size of the invisible stand the suit is hit through; 1.0 is a plain stand and already covers the whole model (clamped to 0.25–8) |
| Aggro range | `kinger.aggro-range` (25 blocks) |
| Move speed | `kinger.move-speed` (0.32) |
| Melee range / radius / damage | `kinger.melee-range` (3) · `kinger.melee-radius` (3.5) · `kinger.melee-damage` (8) |
| Ranged range / damage | `kinger.ranged-range` (30) · `kinger.ranged-damage` (6) |
| Cooldowns | melee 25 ticks · ranged 45 ticks |
| Spawn | `/msc spawn kinger` — **or** place an ArmorStand |
| ArmorStand replacement | `kinger.spawn-on-armorstand-chance` (0.01 = 1% of placed ArmorStands become Kinger; set to 0 to disable) — respects `kinger.enabled` |

### AI behaviour

- **Chases** the nearest player within aggro range (walks at `move-speed`, snaps to the ground) and **faces** the target while tracking its head pitch.
- **Melee** (≤3 blocks): purple dust + smoke burst, `melee-damage` to all players within the melee radius, with a 1.3-velocity knockback.
- **Ranged** (>3 and ≤30 blocks): raises his right arm until it points at the target, then fires a **ShulkerBullet** from that hand (`MSC_KingerBullet`) — damage and a short Darkness, no levitation.
- **Animations**: a walk with arms swinging against the legs and knees that fold; a melee *Royal Decree* (both arms fly up, then sweep down and forward as he lunges — the hit lands at that moment); a ranged *Royal Command* (arm raised and pointed while he fires). His head follows the target from eye height: up for a player above him, down for one below.

**Boss bar:** purple "Kinger" bar, always updated with current health.

**Persistence:** tagged `MSC_Kinger`, so it survives plugin reloads and is picked up again on startup — a reload reattaches the suit it already spawned instead of building a second, overlapping body, and rebuilds its boss bar.

**Death:** removes all suit displays and broadcasts one of the chess-themed `kinger.death-messages` ("checked by the King", "knocked off the board", "lost the game"...).

---

## 🪓 NIX - The Executioner

A towering, ruthless executioner constructed from a custom **27-piece ItemDisplay model** using specialized player skins and matrix transformations. NIX possesses advanced AI, smooth limb movement via procedural JOML quaternion animations, and deadly execution mechanics.

| Stat | Default |
|---|---|
| Health | `nix-executioner.health` (2000.0) |
| Hitbox | `nix-executioner.hitbox-scale` (1.9) — size of the invisible stand the suit is hit through (clamped to 0.25–8) |
| Aggro range | `nix-executioner.aggro-range` (28.0 blocks) |
| Move speed | `nix-executioner.move-speed` (0.30) |
| Melee range / Cleave damage | `nix-executioner.melee-range` (3.5) · `nix-executioner.cleave-damage` (22.0) |
| Chain pull range | `nix-executioner.chain-range` (24.0 blocks) |
| Damage cap taken | `nix-executioner.max-damage-per-hit` (100.0 per hit; `0` disables the cap) |
| Cooldowns | melee 20 ticks · chain pull 80 ticks |
| Summon Ritual | **The Executioner's Scaffold** in Boss Dimension (sacrificing `Executioner's Warrant`) · `/msc spawn nix` (OP) |

### True damage and destructive attacks

Every hit NIX lands is **true damage**, the same kind the Obsidian Sentinel deals: armour and Protection are ignored, each hit is capped at `max-damage-dealt` (15) and Resistance keeps only part of its effect (`true-damage-pierce`, 0.2). On top of his specials he has three **destructive attacks**, at most one per `destructive-cooldown-ticks` (600): **Grand Guillotine** (a sixteen-block guillotine over the target; step off the line), **Blood Moon** (a red moon rises, then three jumpable waves of blood) and **Execution Day** (eight giant axes sweep in along their spokes, twice; stand between them).

### Abilities & Mechanics

- **Guillotine Cleave (Melee AOE):**
  When within melee reach, Nix winds up both arms and delivers a crushing downward cleave. Deals `cleave-damage` (22) in a 3.2-block frontal radius, knocks players back, and inflicts **Wither II (Bleed)** and **Slowness II**.
- **Chains of Judgment (Ranged Pull):**
  When a target tries to flee (between 5 and 24 blocks away), Nix casts spectral iron chains (`Sound.BLOCK_CHAIN_PLACE`) that bind the victim, pulling them violently toward Nix while inflicting **Darkness** and **Slowness III**.
- **Blood Harvest (Signature):**
  He flings both arms out to the sides while blood gathers in his hands and a warning ring closes on the floor, then whirls three full turns with blades of blood trailing from his hands. Every revolution cuts everyone within 4.5 blocks for `harvest-damage` (9) with Wither and drags them in; it ends in a ring of blood.
- **Gallows Leap (Signature):**
  A deep crouch with the arms thrown back while the landing spot glows under the target, then a leap of up to 18 blocks with both arms raised overhead. He lands with a two-handed slam: `gallows-damage` (18) within 4 blocks plus a knock-up, and a shockwave that hits for half on its way out.
- **Condemnation (Signature):**
  He raises his right arm and points at his victims: a gallows of particles stands over every player within aggro range (up to four), the blade trembling at the top and a red circle on the floor. 1.5 seconds later the arm chops down and every blade falls — `condemn-damage` (20), Wither II and Slowness for whoever is still in the circle.
  One signature move every `special-cooldown-ticks` (160) + up to 2 s, picked by distance; while one plays it owns the body.
- **The Executioner's Axe:**
  Nix carries a netherite axe in his right hand that follows his forearm and elbow. The basic attack is a guillotine chop: the axe goes up over his head and comes down in front of him, and the hit lands as the axe falls (60% of the swing). His head follows the player's eyes from his own instead of staring at the floor.
- **Execution Frenzy (Passive):**
  When target player health drops below **25%**, Nix enters an execution frenzy: movement speed increases by +30%, eyes emit crimson dust particles, and walking stride tempo accelerates.
- **Procedural Model Animations:**
  All 27 pieces (Head, Upper Torso, Lower Pelvis, 6-part Right Arm, 6-part Left Arm, 6-part Right Leg, 6-part Left Leg) feature synchronized walking counter-rotations, attack windups, and player-tracking head pitch. They follow an **invisible armour stand** (`MSC_NixBoss`) that carries the real health pool and the hitbox, and the joints live in `NixModel` (shoulders at x = ±0.3514, hips at ∓0.1171, neck at 1.650, torso at 1.171), so every limb swings around its own joint. Each arm and leg is a stack of two segments, so the **elbows and knees fold as well**: the `_4` piece carries the joint and the five pieces below it hinge on it while walking as well as during cleave swings (elbows bend during windup and snap straight on chop, with knees flexing into a squat). The export sits 0.066 blocks off the spine, so the parts are re-centred on the hitbox, and the stand is scaled 1.9 so its box (0.95 wide, 3.75 tall) covers the whole model instead of leaving the head outside a vanilla box.

**Boss bar:** Dark Red segmented bar displaying `NIX - The Executioner` with fog and darkened skies.

**Persistence:** Tagged `MSC_NixBoss` and `MSC_NixPart`, and every part also carries its own tag plus an owner tag for the stand it belongs to — a reload **adopts** the parts a live boss already has instead of building a second body on top, and orphans are cleaned up.

**Death:** Triggers lightning thunder, wither death sounds, a bloody particle explosion, drops 450 XP, and announces an execution end title to nearby players.

---

## 👨‍💻 JACK STAR — The System Architect

Five phases, three lives and a body built out of eleven skin heads.

### Boss stats

| Field | Value |
|---|---|
| Health | `jackstar-architect.health` (1000.0) |
| Hitbox | `jackstar-architect.hitbox-scale` (1.2) — size of the invisible stand the suit is hit through (clamped to 0.25–8) |
| Lives | 3 — the first two "deaths" run a **Watchdog** reboot that restores 50% HP, the last one 40% |
| Phases | 1 >80% · 2 >60% · 3 >40% · 4 >20% · 5 (kernel panic) ≤20% |
| Damage | `melee-damage` (16) · `slam-damage` (20) · `sigkill-damage` (35) |
| Defences | `dodge-chance` (0.22) Ultra Instinct dodge · `packet-loss-chance` (0.25) discards projectiles · firewall, firejail and cobweb builds |
| Aggro / speed / reach | `aggro-range` (32.0) · `move-speed` (0.32) · `melee-range` (3.8) |
| Summon Ritual | **The System Architect** ritual in the Boss Dimension · `/msc spawn jack` (OP) |

### Model

Eleven skin heads (`ItemDisplay`, tag `msc_jackstar_part`) form the head, the torso and two segments per arm and leg. They follow an **invisible armour stand** (`msc_jackstar_boss`) that carries the real health pool and the hitbox, so the visible body is what players aim at while the stand keeps the bookkeeping. The joints live in `JackModel` (shoulders at x = ±0.35, hips at ∓0.12, neck at 1.87) and every limb swings around its own joint, with counter-rotations while walking. Both arms and both legs are exported in two segments, so the **elbows and knees fold on top of that swing**: the forearm and shin hinge on their own joint while walking and during sword slashes (elbows folding through the swing arc and knees sinking into a combat stance). The parts are re-centred on the hitbox, which is why the body lines up with the stand instead of drifting most of a block to the side.

### Signature moves

One every `special-cooldown-ticks` (200) + up to 2 s, 10% sooner in phase 5, picked by distance and announced in the arena chat as a line of code. While one plays it owns the body and the regular routine waits.

| Move | Animation signature | Damage key (default) |
|---|---|---|
| **fork()** | He cocks his right arm back with a spinning wireframe cube in his hand, the left hand aiming, and throws it. Every time the cube lands it bursts and **forks into two** that hop sideways, three generations deep: 1 + 2 + 4 explosions, each with its landing ring shown in advance. | `fork-bomb-damage` (10; ×0.6 for the forks) |
| **Binary Rain** | Both hands raised, fingers typing at the sky while a sheet of green code scrolls over the arena. Glowing cells light up under and ahead of the players and a falling **1 or 0** crashes into each one. | `binary-rain-damage` (8) |
| **Stack Overflow** | A low sprinting stance with both blades swept back, then **four dashing cuts** through the target, each one pushed as a "[ ]" frame that stays drawn on the floor. When the stack is full it overflows: every frame detonates along its line in reverse order. | `stack-overflow-damage` (14; half on the dash itself) |

### True damage and destructive attacks

Every hit Jack Star lands is **true damage**, like the Sentinel's (`max-damage-dealt` 15, `true-damage-pierce` 0.2). Only one special starts every `special-attack-gap-ticks` (112, 5.6 s). From phase 2 he adds three **destructive attacks**, at most one per `destructive-cooldown-ticks` (500): **Kernel Nuke** (a five-second countdown, a locked grid and a dome eleven blocks wide), **Disk Format** (the arena becomes a grid of sectors and all but the green ones are wiped) and **sudo laser** (a beam swept all the way around him; only his feet are safe).

### Subprocesses

Three seconds after spawning, and again on every phase change — five times at most — Jack Star summons another boss 14 blocks away: Garou, Mahoraga, Chaos Mage, Obsidian Guard, Soul Reaper or NIX, drawn at random until one accepts. He stays on the field throughout: a subprocess is extra pressure, **never a shield**, so he keeps fighting and keeps taking damage while one is alive.

### Damage taken

Hits pass one door: the **Ultra Instinct** dodge first (0.22, raised to 0.45 in compressed form), then the **Load Balancer**, which leaves 65% on the boss and shares 35% among every non-creative player within 14 blocks. A landed hit always reaches the boss; `/msc debug` prints the intended hit, the split and the value applied.

**Drops:** 950 XP and the `ArchitectKernel`, with a final title for every player within 60 blocks.

---

## ⏱️ DIO — JoJo's Bizarre Adventure

DIO walks in menacingly (ゴゴゴ letters drift up around him) with his Stand **The World** floating behind his right shoulder. DIO is a visible armor stand in his yellow Part 3 outfit with his own face; The World is a larger stand with its own head, gold armour with emerald trim and a golden aura. Health is virtual, like Nix and Jack Star. Summoned at **The World's Throne** in the Boss Dimension (see [Ritual Dimension](Ritual-Dimension)) or with `/msc spawn dio` (OP).

| Field | Value |
|---|---|
| Health | `dio-brando.health` (1500) — below 50% he enrages ("WRYYYY!"): shorter pauses, longer time stop, more knives |
| Aggro / speed | `aggro-range` (32) · `move-speed` (0.26) |
| Damage cap taken | `max-damage-per-hit` (100) |

| Attack | What happens | Damage key (default) |
|---|---|---|
| **ZA WARUDO** | The World rises with its arms spread and a sphere of stopped time sweeps out `time-stop-radius` (40) blocks. For `time-stop-ticks` (100) players cannot move, attack, use items or shoot; mobs and projectiles freeze too, and he counts the seconds ("1-byō keika..."). He throws knives that stop in the air in a ring around everyone. "Toki wa ugokidasu": time moves again and every knife flies. Cooldown `time-stop-cooldown-ticks` (700). | `knife-damage` (5) per knife |
| **MUDA MUDA MUDA** | The World rushes up to 10 blocks in and buries the target under a barrage of fists, then a last two-fisted "MUDAAA!" that launches them. | `barrage-damage` (2.5 every 3 ticks) · `barrage-finisher-damage` (14) |
| **ROAD ROLLER DA!** | He leaps high over the target, a road roller appears and falls with him on its roof; The World pounds it into the ground ("MUDA MUDA") and it explodes. The landing ring is shown the whole time. | `road-roller-damage` (26; half on the explosion) |
| **Knife fan** | Knives drawn behind his head and thrown in a fan of 7 (9 enraged). | `knife-damage` (5) |
| **Space Ripper Stingy Eyes** | His eyes glow red, then two jets of pressurised fluid sweep across the arena. | `eye-beam-damage` (5 every 3 ticks) |
| **The World's punch** | His basic attack at close range: a heavy punch with knockback. | `punch-damage` (12) |

He greets anyone who walks up to him ("Oh? You're approaching me?"). His knives, the road roller and the menacing letters are display props removed when an attack ends, the boss dies or the server restarts; `/msc kill` removes DIO and The World.
### True damage and the Final Hour

Every hit DIO and The World land is **true damage**, like the Sentinel's (`max-damage-dealt` 15, `true-damage-pierce` 0.2). His destructive attack, **The Final Hour**, comes at most once per `final-hour-cooldown-ticks` (900): a golden clock face sixteen blocks wide spreads under him, its hand sweeps round and stops on one hour that glows green, then "ZA WARUDO" — The World pummels every other hour in turn. Run to the lit hour.

---

## 🌪️ WITHER STORM — Cracker's Wither Storm Mod

The Wither Storm of nonamecrackers2's mod, drawn with nothing but display entities: every box of the mod's model is a `BlockDisplay` (obsidian, black concrete, crying obsidian, the command block in its ribs...) and the original Wither's three heads are `ItemDisplay`s holding wither skeleton skulls. The geometry, the head positions, how the jaws open and how the tentacles sway all come from the mod's own model code, so every piece moves where the mod puts it.

### Summoning

Build **the Wither's structure** (a T of 4 soul sand or soul soil and 3 wither skeleton skulls) with the foot of the T resting on the **core block**: `wither-storm.summon.core-block` (`CRYING_OBSIDIAN` by default). No command block is needed. When the third skull goes on, the Wither that would have been born is swallowed with the core and the storm forms in its place: a vortex of dark matter, a lightning bolt and eleven invulnerable seconds (`forming-ticks`, 220) while it grows and its bar fills, then the blast of its birth (`explosion-power`, 7, like a Wither's). Built on any other block the structure still makes a plain Wither.

Also: `/msc spawn witherstorm` (OP), and `witherstorm2`…`witherstorm5` to start in a later form.

### The five forms

It grows by **eating**: what its tractor beams tear up and swallow adds points (`consume-points`: block 1, mob 6, bitten player 10, item 1) and at `forms.<form>.evolve-at` it evolves — every head roars, the old body shrinks, the new one grows, and its max health rises (damage already dealt stays).

| Form | Mod model | Size (scale 1) | Health | Mass takes | Beam | Flight | Evolves at |
|---|---|---|---|---|---|---|---|
| **The Hunchback** | Phase 1: the Wither with its command block and the first lump | 3 × 3.5 | ×1 | 100% | 20 | 5 | 200 |
| **The Growing Hunchback** | Phase 2: the mass swallowed the middle head and grew a jaw | 3.3 × 3.7 | ×1.5 | 90% | 26 | 6 | 600 |
| **The Swollen Hunchback** | Phase 3: the first three tentacles | 11.5 × 11.7 | ×2 | 80% | 32 | 8 | 1500 |
| **The Destroyer** | Phase 4: three heads on a flying mass | 61 × 62 | ×3.5 | 60% | 64 | 28 | 6000 |
| **The Devourer** | Phase 5: the colossal mass and nine tentacles | 110 × 114 | ×5 | 50% | 96 | 40 | — |

Base health is `wither-storm.health` (800) times `forms.<form>.health-multiplier`. Each form's size is set with `forms.<form>.scale` (1.0 is the mod's own size; the Devourer is over a hundred blocks wide).

### How it feeds

- **The hunchbacks (forms 1–3) are a black hole:** every 3 s they tear 3, 9 or 18 lumps of ground (by form) loose round them and pull them tumbling into their mass, like the mod's cluster source. That is how they grow into the Destroyer.
- **From the Destroyer on they eat through their beams:** where a beam meets the ground it tears blocks out and they fly into the mouth.
- It honours `mobGriefing` and protection plugins (every block goes through `EntityChangeBlockEvent`), only takes exposed blocks and never takes blocks with an inventory, bedrock, portals or command blocks. With `grief-blocks: false` the ground stays put but the storm still grows.
- It only eats while it has victims near: left alone, it does not eat the world.

### Tractor beam

As in the mod: **the first form has no beam**, in the next two hunchbacks only the middle head has one, and from the Destroyer on all three.

1. A beam only lights up once its head points at a victim it **can see** (no blocks in between).
2. It glows **thin and harmless** for `beam.charge-ticks` (30): that is the warning.
3. It **pulls** for `beam.hold-ticks` (120) at the mod's speed, `beam.pull-speed` (0.2 blocks a tick), slower within the last block.
4. It **rests** for `beam.rest-ticks` (100), and after a bite.

While its beam is on a head turns **slower than a player sprints**: run sideways across the cone and you break free. The hunchbacks only drag the victim they locked onto; the Destroyer and the Devourer drag anything inside the cone. The beam is a translucent cone of purple glass, as in the mod.

### Attacks

It attacks **every living thing** round it, players first (`attack-mobs`).

| Attack | What happens | Key (default) |
|---|---|---|
| **Bite** | Whatever reaches a mouth: a player is bitten (true damage + Wither II) and spat out, and that head rests; a mob or an item is devoured. Pets and named mobs are spat out. | `bite-damage` (14) |
| **Roar + flaming skull** | Every 20–50 s (`roar-interval-*`, like the mod) a head roars (Slowness round it) and spits a **flaming skull** that explodes in fire. | power per form (2.5 → 5) |
| **Wither skulls** | The hunchbacks shoot plain Wither skulls (10% charged). | — |
| **Tentacles** | The tentacle tips strike and fling whoever they sweep through. | `tentacle-damage` (10) |
| **Wither sickness** | Near the storm, hunger (20 s), then weakness (40 s), then wither and fatigue (80 s). Hostile mobs near it become **Sickened** and fight for it. | `sickness.*` |

### Special attacks

One every `specials.cooldown-ticks` (500, +0–10 s), never the same twice in a row, announced on the action bar:

| Attack | From | What happens | Key |
|---|---|---|---|
| **Cataclysmic Roar** | Hunchback | Every head roars at once, the sky goes dark (Darkness and Nausea) and a **shockwave** rolls along the ground. **Jump it**: anyone in the air is spared. | `roar-damage` (10) |
| **Skull Barrage** | Growing Hunchback | Every head spits a fan of flaming skulls for two seconds. | — |
| **Debris Rain** | Swollen Hunchback | The ground it ate comes back down: red circles mark where the rocks will land. | `debris-damage` (8) |
| **Abyssal Eruption** | Swollen Hunchback | The ground under its victims trembles and, a moment later, **tentacles** of its mass burst out and fling them. | `eruption-damage` (12) |
| **Singularity** | Destroyer | It holds still and drags the whole field into its core for 4 s, then the core bursts. | `singularity-damage` (16) |

Lightning also strikes and thunder rolls round the colossal forms.

### Summons

One call every `summons.cooldown-ticks` (700, +0–10 s), at most `summons.max` (8) alive at once. They all die with it.

| Summon | From | What it is |
|---|---|---|
| **Sickened Horde** | Hunchback | `horde-size` (4, +2 on the colossal forms) sickened zombies, skeletons, spiders, vindicators, husks and strays crawling out of purple rifts round a victim. |
| **Withered Symbiont** | Swollen Hunchback | The mod's minion: a giant wither skeleton (scale 1.5) in purple armour with a netherite sword and `symbiont-health` (220) health, spitting wither skulls. One at a time. |
| **Phantom Swarm** | Destroyer | Three sickened phantoms diving at a victim. |

Every direct hit it lands is **true damage** (`max-damage-dealt` 20, `true-damage-pierce` 0.2). Like every boss, its damage **adapts** to what each player has invested (`boss-balance.adaptive-damage`): its direct hits inside the true damage, its explosions, skulls and summons as plain damage. It pierces the Infinity set, and the Infinity sword deals it half, as with the other bosses.

### How the fight goes

- **Hitboxes:** the mass and each head are `Interaction` entities that follow the model. Swords deal the weapon's damage, scaled by the attack cooldown, plus Sharpness and **Smite** (the storm is undead like any Wither). Projectiles that enter a box count too.
- **Injuring the heads:** every projectile that reaches a head counts; after a few (1–2 on the hunchbacks, 3–8 on the colossal forms) the head is **injured** for `injury-ticks` (200): its beam goes dark, it spits a blue skull and takes ×1.5 damage. Whoever injured it **escapes**: the storm leaves them alone for `escape-ticks` (800, the mod's 40 s). With **every** head injured at once the storm is **exposed** for `exposed-ticks` (160) and takes ×`exposed-damage-multiplier` (2).
- **Playing dead:** the Destroyer and the Devourer, at `play-dead-threshold` (15%), fall to the ground with their jaws hanging for `play-dead-ticks` (200), taking ×1.5... then rise with every head roaring, a blast that hurts (`revive-damage` 12) and throws everyone back, and 10% of their health back. Only once.
- **Death:** it breaks apart in rays of light as its blocks burst one by one; it drops Nether Stars (1 to 5 by form) and experience, and cures everyone's sickness.

### Performance

The body rides the anchor (an invisible marker armour stand, `MSC_WitherStorm`), which keeps its form, health and what it has eaten: after a restart the storm is rebuilt where it was, and no piece of the body is persistent. Moving it costs one packet; animated pieces are sent again every 3 ticks and only when they moved more than 3 cm or 1°, and the body turns in steps the client smooths over half a second. The Destroyer uses the mod's low-detail mass (180 displays); `high-detail: true` draws the full one (548). With no player within 50 blocks (100 for the Destroyer and the Devourer) for 5 s it leaves with its summons (`boss-balance.despawn`).

Every form, pose and animation can be looked at in the model viewer `tools/stand-viewer/index.html`, which opens straight in a browser (double-click) after `mvn test`; `tools/stand-viewer/textures.ps1` adds your Minecraft's textures to it.
