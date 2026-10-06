# Wither Storm forms

The Wither Storm (`entities/boss/witherstorm`) is drawn from the five text files in
`src/main/resources/witherstorm/`. Each one is a form of the storm as Cracker's Wither Storm Mod
4.2.1 builds it: the mod's part tree (pivot and rest rotation of every part), every box with the block
it is drawn with, how each subtree is scaled, and the head and tentacle numbers its animation runs on.

| File | Mod model | Boxes |
|---|---|---|
| `hunchback.txt` | `WitherStormHunchbackModel` + `HunchbackBodyModel` | 43 |
| `growing.txt` | `WitherStormGrowingHunchbackModel` + `GrowingHunchbackMassModel` | 94 |
| `pregnant.txt` | `WitherStormPregnantHunchbackModel` + `PregnantHunchbackBodyModel` | 121 |
| `destroyer.txt` | `WitherStormDestroyerModel` + `DestroyerBodyModel` | 548 |
| `devourer.txt` | `WitherStormDevourerModel` + `LowResDevourerBodyModel` | 324 |

The Devourer uses the mod's low-detail mass: its full one is 3,558 voxels, and every box is one
display entity.

## Regenerating them

```powershell
.\tools\wither-storm\generate.ps1 -Mod "C:\path\to\witherstormmod-1.20.1-4.2.1-all.jar"
```

The script decompiles the mod's model classes with Fernflower, lifts the shared builders out of them
(`gen.Extract`), compiles the mass models against a handful of stand-ins for Minecraft's model
builders (`src/net/minecraft/...`) and runs `gen.Gen`, which:

- composes each form the way the mod's `createLayerDefinition` does;
- picks each box's block from the colour of its face on `wither_storm.png` (teeth, the beam
  emitter and the command block are named outright), and lights the boxes the emissive decal lights;
- greedy-merges voxel masses into larger boxes when that leaves fewer of them;
- speckles the mass with a few darker blocks and glowing crying obsidian.

No code of the mod is stored here: it is read from the decompiled sources each time.

## Seeing them

`mvn test` writes `target/stand-viewer/models.json`, and `tools/stand-viewer/` shows every form, in
its poses and animations, with its tractor beams and hitboxes.

The viewer draws the blocks, the command block and the wither skeleton skulls with the game's own
textures once `tools/stand-viewer/textures.ps1` has copied them out of your Minecraft client into
`target/stand-viewer/textures` (they are Mojang's, so they are never committed). Without them it falls
back to flat stand-in colours.
