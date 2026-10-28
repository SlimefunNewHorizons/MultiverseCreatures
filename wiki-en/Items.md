# 🎒 Items

This page covers signature misc items (Ice King's Crown, Mantis Claws, Wirt's Lantern), utility items (Military Mine), and food (Scooby Cookie, Head Slime Gelatin).

All items use `ItemBuilder` and unique `msc_<item>` PDC tags. Use **`/msc give <item>`** to obtain (OP-only).

---

## 🧊 Ice King's Crown (Adventure Time — Ooo)

> "Gunter, why you gotta be like that?"

A crown of eternal winter, granting mastery over ice and snow.

| Stat | Value |
|---|---|
| Material | HORN_CORAL_FAN |
| Slot | Main hand OR off-hand (cannot be placed as armor-stand helmet) |
| Passive combat | Holder takes **×0.8 damage** (20% reduction) + immune to FREEZE damage cause |

### Abilities

| Control | Ability | Cooldown |
|---|---|---|
| **Right-Click** (not sneaking) | **Snow Block Launch** — Select a snow/ice block within 5 blocks (raised visual). On a second right-click, target a LivingEntity within 30 blocks to **launch the block** as a `FallingBlock` projectile (ballistic velocity, gravity 0.08, max 100 ticks). | 10 s per launch |
| **Right-Click** (sneaking) | **Blizzard** — 5-second expanding AoE up to 8-block radius. 3 dmg/tick + Slowness II + Darkness II (40t) + outward knock + freezes nearby water to ICE. | 60 s |
| **Left-Click** | **Toggle Ice Path** — Walking on/over water converts it to `FROSTED_ICE` (3×3 area, 30% chance for adjacent blocks). Particles: SNOWFLAKE. | No cooldown |

**Snow Block Launch effects on impact (12 dmg + knockback +0.3 Y, freezes nearby water to ICE):**
- Snow block → Slowness II + Weakness I (5 s)
- Ice block → Slowness I + Weakness I + Nausea I (5 s)

Sounds: BLOCK_SNOW_STEP, BLOCK_NOTE_BLOCK_CHIME, ENTITY_SNOW_GOLEM_SHOOT, BLOCK_GLASS_BREAK, ENTITY_ELDER_GUARDIAN_CURSE, ENTITY_GENERIC_EXTINGUISH_FIRE, WEATHER_RAIN_ABOVE, CLOUD.

- **Give:** `/msc give icecrown`
- **Trade:** from the Multiverse Merchant for 48 Nether Stars + 64 Blue Ice
- **Theme:** Adventure Time (Ice King, Gunter)

---

## 🦗 Mantis Claws (Hollow Knight — Hallownest)

> "The mantis lords watch from above."

Claws forged from the silk and iron of Deepnest.

| Stat | Value |
|---|---|
| Material | SHEARS |
| Passive 1 | **Cling to walls** — Shift while sneaking + mid-air + against a wall → slow fall (downward velocity capped at -0.1) + Jump Boost II (40 t) for safe landing |
| Passive 2 | **Wall-jump** — Space while off-ground next to a wall → Y velocity = 0.55 (`WALL_JUMP_VERTICAL`), resets fall distance + sweep_attack sound + CRIT particles |
| Restriction | Cannot break blocks while holding |
| Unbreakable | Yes (hidden) |
| Custom model data | 1002 |
| Wall-jump implementation | Custom Netty packet handler intercepts `ServerboundPlayerInputPacket` to detect rising-edge jump inputs. |

- **Give:** `/msc give mantisclaws`
- **Trade:** from the Multiverse Merchant for 16 Iron Ingots + 8 String
- **Theme:** Hollow Knight (Deepnest, Mantis Lords)

---

## 🔮 Wirt's Lantern (Over the Garden Wall — the Unknown)

> "The flame knows no winter."

A lantern that holds a lost soul: the Beast's. It keeps the holder safe from monsters, never helps them fight.

| Stat | Value |
|---|---|
| Material | SOUL_LANTERN |
| Slot | Main OR off-hand |
| Passive task | Every `mob-repel.interval-ticks` (20), for every online player holding it: <br>• Night Vision I (100 t) <br>• **Repels hostile mobs** within `mob-repel.radius` (12) blocks: outward push (`horizontal-force` 0.6, `vertical-force` 0.2, capped at 0.6 up) and 30% SOUL_FIRE_FLAME. Villagers, pets, armor stands and the holder's own mount are left alone <br>• Hostile mobs drop the holder as their target (`target-protection`) |
| Mob protection | After holding it for `arm-ticks` (30, 1.5 s), damage caused by mobs (melee, their arrows, fireballs, explosions) is cancelled. **Players, falls, fire, lava, drowning, hunger, the void and `/kill` still hurt.** |
| No harm | While it is held, and for `disarm-ticks` (60, 3 s) after putting it away, the holder deals **no damage to anything**, players included: melee, projectiles, Stands, TNT, pets, fire, lava buckets, flint & steel and poison/wither potions are all blocked. Swapping to a weapon between hits gives no immunity. |
| Boss Dimension | It does **nothing** there: no protection, repel, de-targeting, night vision or damage block. |
| Placing | Never placed or hung on an armor stand, anywhere (it would lose what makes it Wirt's). |
| PDC tag | `msc_wirts_lantern` |
| Config | `items.wirts-lantern`: `enabled`, `mob-repel.*`, `night-vision.enabled`, `target-protection.enabled`, `mob-protection.enabled`, `arm-ticks`, `disarm-ticks` |

- **Give:** `/msc give wirtslantern`
- **Trade:** from the Multiverse Merchant for **The Beast's Antler** + **Edelwood Oil**, the end of the Edelwood chain (see [Components](./Components.md#-wirts-lantern-chain-over-the-garden-wall))
- **Theme:** Over the Garden Wall (Wirt, the Beast and the Edelwood trees of the Unknown)

---

## 🧨 Military Mine (Military)

> "One step is all it takes."

A crafted explosive device auto-camouflaged to look like surrounding terrain.

| Stat | Value |
|---|---|
| Material | TNT |
| Camouflage | Auto-determined from block below, or most common block within 3-block radius. Excludes: bedrock, barriers, banners, signs, doors, trapdoors, beds, shulker boxes, redstone attachments, plants, carpets, candles, eggs, spawners, etc. |
| Detonation triggers | A non-creative/flying player **walks onto the block above**; block **break**; **right-click** the block; or another explosion removes the entry |
| Explosion power | 4.0 (block-breaking) |
| PDC tag | `msc_military_mine` |

- **Give:** `/msc give militarymine`
- **Craft:** requires **Military Component** (dropped by ZombieHorseTrap units)
- **Theme:** Military (ZombieHorseTrap chain)

---

## 🍪 Food

### Scooby Cookie (Mystery Inc.)

> "Scooby-Dooby-Doo... This tastes like courage!"

| Stat | Value |
|---|---|
| Material | COOKIE |
| Food | 2 · Saturation 0.4 |
| **Effect on consume** | **Resistance VI** (amplifier 5) for 10 seconds (200 ticks) |
| PDC tag | `msc_scooby_cookie` |

**Lore:** *"A mysterious cookie pulsating with otherworldly energy."*

- **Give:** `/msc give scoobycookie`
- **Trade:** from the Multiverse Merchant (5 for 20 Diamonds)
- **Theme:** Mystery Inc. (Scooby-Doo)

---

### Head Slime Gelatin (Slime Kingdom)

> "Slimy yet satisfying!"

A bouncy, wobbly, strangely tasty gelatin.

| Stat | Value |
|---|---|
| Material | MAGENTA_GLAZED_TERRACOTTA |
| Food | 4 · Saturation 2.4 |
| **Right-click effect** | Adds the user's UUID to `HeadSlime.immunePlayers` set for **10 seconds** (200 ticks). Witch particle ring renders around the immune player; any Head Slime attached auto-detaches from them. |
| PDC tag | `msc_head_slime_gelatin` |

- **Give:** `/msc give headslimegelatin`
- **Craft:** from **Head Slime Heart** (dropped by Head Slime)
- **Theme:** Slime Kingdom
