# Rigged Bosses Implementation Plan

**Goal:** Give Malenia a second phase and better movement on a shared, reorganised engine; add four
new display-entity bosses on that engine; show every model in the web viewer; add elite mob
variants; clean the project.

**Architecture:** Malenia's skeleton maths and her fight loop are lifted out of `MaleniaRig` /
`MaleniaBoss` into a generic `rig` package (`Rig`, `RigForm`, `RigBody`, `RiggedBoss`, `Blast`).
Every boss is then a model (pieces + skeleton + keyframed moves) plus a small behaviour class.
The viewer already plays skeleton moves; it only learns three automatic bone kinds.

**Tech stack:** Java 21, Paper API, block displays, JUnit 5, Maven
(`C:\Users\danie\.m2\wrapper\dists\apache-maven-3.9.6\a53741d1\bin\mvn.cmd -q -o test`),
three.js viewer in `website/public/viewer/index.html`, `python website/build.py`.

**Spec:** the user's request in chat (2026-11-10). No separate spec file.

## Global Constraints

- New bosses are original characters: own names and looks. Mechanics only are borrowed from the
  request (bombs; gravity; two phases that cut max health; a white-eyed stalker).
- Packets: a new boss has at most 200 block displays. Poses are sent every 2 ticks in an attack,
  every 4 otherwise, never while no player is in range, and never for a piece that did not move.
- No new schedulers per effect: timed effects are `Hazard`s ticked by the boss's own loop.
- Existing config keys keep their names and defaults. `pom.xml` is the user's edit: leave it.
- Kinger's survival spawn stays undocumented.
- Baseline: `mvn -q -o test` passes before any change.

## Review Focus

- Chunk unload / reload mid-fight: the stand survives, the body is dressed again in the right phase.
- Phase change while an attack is under way: the attack is dropped, no hit lands during the change.
- Plugin disable mid-fight: bodies, bars and hazard props are removed.
- A boss with no player in range sends nothing.
- Elite roll when the base entity is disabled in config: no elite either.

## File Structure

```
entities/boss/rig/Rig.java          skeleton, keys, moves, sampling, automatic bones, posing
entities/boss/rig/RigForm.java      one look of a boss: rig, pieces, moves, weapon, trail colour
entities/boss/rig/RigBody.java      the displays: spawn, send (skips unmoved pieces), remove
entities/boss/rig/Hazard.java       a timed effect ticked by its boss; Blast = telegraph then burst
entities/boss/rig/RiggedBoss.java   stand, bar, phases, locomotion, attacks, hits, events
entities/boss/malenia/              MaleniaBoss, MaleniaModel, MaleniaRig, MaleniaUnbound (phase 2)
entities/boss/cinderwick/           Cinderwick, the Fuse Warden (bombs)
entities/boss/orrin/                Orrin, the Orbit Tyrant (gravity)
entities/boss/bellwarden/           The Bell Warden -> The Unrung (two forms, max-health toll)
entities/boss/herobrine/            Herobrine (stalker)
entities/EliteMobs.java             elite variants of the natural mobs
test .../rig/RigViewerExport.java   any RigForm as a viewer entry
```

## Tasks

1. **Engine.** `Rig` (from `MaleniaRig`: `Bone`, `Ease`, `Key`, `Move`, builder, `sample`,
   `motion`, `follow`, `legs`, `bones`, `pose`, `posed`, `toWorld`; automatic bones by name:
   `CAPE`/`HAIR` trail, `WING_R`/`WING_L` flap, `HALO` spins, `LEG_R..` stride).
   `RigBody.send` skips a piece whose transformation is within 1e-4 of the last sent.
   Test: `RigTest` (sampling, parents before children, unknown bone rejected, `changed`).
2. **Malenia on the engine.** Move her three files to `boss/malenia`; `MaleniaRig` keeps her
   bones, moves and the skirt; `MaleniaBoss extends RiggedBoss`. Existing Malenia tests pass.
3. **Movement** (in `RiggedBoss`, so every boss has it): turn rate limit, walk / run loops,
   circling inside reach while on cooldown, stepping back when crowded, aim tracking during a
   wind-up, a dodge after being hit. Malenia gains `walk`, `run`, `backstep` moves.
4. **Malenia phase two.** At 0 health she rises again as "Malenia, Blade Unbound": cape gone,
   ribbon wings, a halo of blade shards, a lit blade; new moves `ascend` (transition), `volley`,
   `skyfall`, `echo`, `crescent`. Config `entities.malenia.phase-two.*`.
5. **Cinderwick.** Palm brand (delayed blast on the player), ground mines, lobbed keg, fuse line.
6. **Orrin.** Pull, lift-and-drop, meteor rain, orbit stones thrown, stomp.
7. **Bell Warden / Unrung.** Slow bell golem; at half health the bell cracks and the Unrung
   steps out with a new body. Every hit tolls: the player's max health drops for 30 s.
8. **Herobrine.** Vanishes when stared at, blinks behind his target, lightning, darkness.
9. **Registration** for 5-8: main class, `SpawnCatalogue`, `BossInfinityRules`, `/msc debug
   model <boss> [move]`, config, wiki (en + es), boss-active check.
10. **Viewer.** `RigViewerExport` for every form; `StandViewerExport` lists them; viewer learns
    `WING_*`, `HALO`, `rig.leg`, trail colour. Check each model on the built site.
11. **Elites.** `EliteMobs`: after a natural MSC spawn, a roll of `0.7 x` that entity's own chance
    turns it elite (bigger, tougher, harder hitting, better drops). Config `general.elite.*`.
12. **Cleanup.** Unused imports, dead files and helpers, duplicates left by the refactor; then the
    full test suite and a site build.
