package com.Chagui68.wiki;

import com.Chagui68.ritual.BossInvocationStructure;
import com.Chagui68.ritual.DioInvocationStructure;
import com.Chagui68.ritual.JackInvocationStructure;
import com.Chagui68.ritual.MaleniaInvocationStructure;
import com.Chagui68.ritual.NixInvocationStructure;
import com.Chagui68.ritual.PantheonAltarStructure;
import com.Chagui68.ritual.PantheonAltarStructure.Pantheon;
import com.Chagui68.ritual.RitualStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /mscwiki <structure>}: shows a summoning structure as displays one block in front of the player
 * for five seconds, so they can see how to build it, and says in chat how it is used. Nothing is placed
 * in the world.
 *
 * <p>Stairs look the way they do once built facing inwards: each faces the middle and takes the corner
 * shape Minecraft gives it from the stairs beside it. Heads and skulls are item displays, because a block
 * display does not draw them.</p>
 */
final class StructurePreview {

    private static final long LIFETIME_TICKS = 100L;

    /** One block of a structure, counted from its north-west corner on the ground layer. */
    record Part(int x, int y, int z, Material material) {
    }

    /** A block position inside a structure. */
    record Cell(int x, int y, int z) {
        Cell step(BlockFace face) {
            return new Cell(x + face.getModX(), y, z + face.getModZ());
        }
    }

    /** Every structure {@code /mscwiki} shows, with how it is used. */
    enum Structure {
        PORTAL("portal",
                "Ritual portal: light its 12 candles and stay inside the circle to travel to the Boss Dimension."
                        + " Built inside it, it brings you back.",
                "Portal del ritual: enciende sus 12 velas y quédate dentro del círculo para viajar a la Dimensión"
                        + " de Jefes. Construido dentro de ella, te devuelve."),
        SENTINEL("sentinel",
                "Obsidian Sentinel, in the Boss Dimension: light the 12 red candles, then drop an Echo Shard in"
                        + " the middle.",
                "Centinela de Obsidiana, en la Dimensión de Jefes: enciende las 12 velas rojas y suelta un"
                        + " Fragmento de Eco en el centro."),
        NIX("nix",
                "NIX, in the Boss Dimension: light the 4 red candles, right-click one, then drop an Execution"
                        + " Warrant on the anvil (a netherite axe or a wither skeleton skull also works).",
                "NIX, en la Dimensión de Jefes: enciende las 4 velas rojas, haz clic derecho en una y suelta una"
                        + " Execution Warrant sobre el yunque (también vale un hacha de netherita o una calavera de"
                        + " esqueleto wither)."),
        DIO("dio",
                "DIO, in the Boss Dimension: light the 4 yellow candles, right-click one, then drop a clock on the"
                        + " gold throne. Any head or skull works on the pillars.",
                "DIO, en la Dimensión de Jefes: enciende las 4 velas amarillas, haz clic derecho en una y suelta un"
                        + " reloj sobre el trono de oro. En los pilares vale cualquier cabeza o calavera."),
        MALENIA("malenia",
                "Malenia, in the Boss Dimension: light the 4 magenta candles, right-click one, then drop a"
                        + " torchflower on the nether wart heart.",
                "Malenia, en la Dimensión de Jefes: enciende las 4 velas magenta, haz clic derecho en una y suelta"
                        + " una antorchaflor sobre el corazón de verruga del Nether."),
        JACK("jack",
                "Jack Star: light the 4 candles, right-click one, then drop a Nether Star on the core (an Echo"
                        + " Shard, a Heart of the Sea, a beacon or an Architect Kernel also works).",
                "Jack Star: enciende las 4 velas, haz clic derecho en una y suelta una Estrella del Nether sobre el"
                        + " núcleo (también vale un Fragmento de Eco, un Corazón del Mar, un faro o un Architect"
                        + " Kernel)."),
        OLYMPUS("olympus", pantheonEnglish("Olympus"), pantheonSpanish("el Olimpo")),
        ASGARD("asgard", pantheonEnglish("Asgard"), pantheonSpanish("Asgard")),
        DUAT("duat", pantheonEnglish("the Duat"), pantheonSpanish("la Duat")),
        VOID("void", pantheonEnglish("the Void"), pantheonSpanish("el Vacío")),
        WITHERSTORM("witherstorm",
                "Wither Storm, in the Boss Dimension: build a Wither (soul sand in a T, three wither skeleton"
                        + " skulls) with the foot of the T on {core}. The last skull starts the storm.",
                "Tormenta Wither, en la Dimensión de Jefes: construye un Wither (arena de almas en T y tres"
                        + " calaveras de esqueleto wither) con el pie de la T sobre {core}. La última calavera"
                        + " desata la tormenta.");

        private final String id;
        private final String english;
        private final String spanish;

        Structure(String id, String english, String spanish) {
            this.id = id;
            this.english = english;
            this.spanish = spanish;
        }

        String id() {
            return id;
        }

        /** The structure called {@code id}, or null. */
        static Structure byId(String id) {
            for (Structure structure : values()) {
                if (structure.id.equalsIgnoreCase(id)) return structure;
            }
            return null;
        }

        private static String pantheonEnglish(String name) {
            return "Pantheon altar of " + name + ", in the Boss Dimension with DrakesBosses: light the 4 candles,"
                    + " right-click one, then drop a god's offering on the core. The altar lists them when it wakes.";
        }

        private static String pantheonSpanish(String name) {
            return "Altar del panteón de " + name + ", en la Dimensión de Jefes con DrakesBosses: enciende las 4"
                    + " velas, haz clic derecho en una y suelta la ofrenda de un dios sobre el núcleo. El altar las"
                    + " lista al despertar.";
        }
    }

    private StructurePreview() {
    }

    static void show(Plugin plugin, Player player, Structure structure, WikiLang lang) {
        // The same key and fallback the Wither Storm reads its core block from.
        Material core = Material.matchMaterial(plugin.getConfig()
                .getString("entities.wither-storm.summon.core-block", "CRYING_OBSIDIAN"));
        if (core == null || !core.isBlock()) {
            core = Material.CRYING_OBSIDIAN;
        }
        BlockFace facing = player.getFacing();
        List<Part> parts = turnedFor(parts(structure, core), facing);
        int width = 0, height = 0, depth = 0;
        for (Part part : parts) {
            width = Math.max(width, part.x() + 1);
            height = Math.max(height, part.y() + 1);
            depth = Math.max(depth, part.z() + 1);
        }
        // One empty block between the player and the near edge, structure centred on the player's line.
        int reach = 2 + (facing.getModX() != 0 ? width : depth) / 2;
        Location origin = player.getLocation().getBlock().getLocation()
                .add(facing.getModX() * reach - width / 2, 0, facing.getModZ() * reach - depth / 2);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (!origin.clone().add(x, y, z).getBlock().isReplaceable()) {
                        player.sendMessage(Component.text(lang.pick("There is no space for the structure to appear.",
                                "No hay espacio para que aparezca la estructura."), NamedTextColor.RED));
                        return;
                    }
                }
            }
        }

        Map<Cell, BlockFace> stairs = new HashMap<>();
        for (Part part : parts) {
            if (isStairs(part.material())) {
                stairs.put(new Cell(part.x(), part.y(), part.z()), inwards(part.x(), part.z(), width, depth));
            }
        }
        World world = origin.getWorld();
        // Turned the way the player looks, a head's face looks back at them.
        float headYaw = (float) Math.toDegrees(Math.atan2(-facing.getModX(), facing.getModZ()));
        List<Display> displays = new ArrayList<>();
        for (Part part : parts) {
            Location at = origin.clone().add(part.x(), part.y(), part.z());
            if (isHead(part.material())) {
                at.add(0.5, 0.5, 0.5);
                at.setYaw(headYaw);
                displays.add(world.spawn(at, ItemDisplay.class, display -> {
                    display.setItemStack(new ItemStack(part.material()));
                    display.setPersistent(false);
                }));
                continue;
            }
            BlockData data = part.material().createBlockData();
            if (data instanceof Stairs stair) {
                Cell cell = new Cell(part.x(), part.y(), part.z());
                stair.setFacing(stairs.get(cell));
                stair.setShape(shape(stairs, cell));
            }
            displays.add(world.spawn(at, BlockDisplay.class, display -> {
                display.setBlock(data);
                display.setPersistent(false);
            }));
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> displays.forEach(Display::remove),
                LIFETIME_TICKS);
        String hint = lang.pick(structure.english, structure.spanish)
                .replace("{core}", core.name().toLowerCase(Locale.ROOT));
        player.sendMessage(Component.text(hint, NamedTextColor.GOLD));
    }

    /** The blocks of {@code structure}; the Wither Storm's stand on {@code witherCore}. */
    static List<Part> parts(Structure structure, Material witherCore) {
        return switch (structure) {
            case PORTAL -> fromMap(RitualStructure.getStructureBlocks());
            case SENTINEL -> fromMap(BossInvocationStructure.getStructureBlocks());
            case NIX -> altar(NixInvocationStructure.CANDLE_OFFSETS, NixInvocationStructure.CORNER_OFFSETS,
                    Material.ANVIL, Material.RED_CANDLE,
                    Material.POLISHED_BLACKSTONE_BRICKS, Material.IRON_CHAIN, Material.WITHER_SKELETON_SKULL);
            case DIO -> altar(DioInvocationStructure.CANDLE_OFFSETS, DioInvocationStructure.CORNER_OFFSETS,
                    Material.GOLD_BLOCK, Material.YELLOW_CANDLE,
                    Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.SKELETON_SKULL);
            case MALENIA -> altar(MaleniaInvocationStructure.CANDLE_OFFSETS, MaleniaInvocationStructure.CORNER_OFFSETS,
                    MaleniaInvocationStructure.CORE, MaleniaInvocationStructure.CANDLE,
                    MaleniaInvocationStructure.PILLAR, MaleniaInvocationStructure.PILLAR_TOP);
            case JACK -> altar(JackInvocationStructure.CANDLE_OFFSETS, JackInvocationStructure.CORNER_OFFSETS,
                    Material.RESPAWN_ANCHOR, Material.CYAN_CANDLE,
                    Material.CRYING_OBSIDIAN, Material.LIGHTNING_ROD);
            case OLYMPUS -> pantheon(Pantheon.OLYMPUS);
            case ASGARD -> pantheon(Pantheon.ASGARD);
            case DUAT -> pantheon(Pantheon.DUAT);
            case VOID -> pantheon(Pantheon.VOID);
            case WITHERSTORM -> witherStorm(witherCore);
        };
    }

    private static List<Part> fromMap(Map<Location, Material> blocks) {
        List<Part> parts = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : blocks.entrySet()) {
            if (entry.getValue() == Material.AIR) continue;
            Location at = entry.getKey();
            parts.add(new Part(at.getBlockX(), at.getBlockY(), at.getBlockZ(), entry.getValue()));
        }
        return parts;
    }

    /** A 5x5 altar: the core in the middle, its candles beside it, a pillar on each corner from the ground up. */
    private static List<Part> altar(int[][] candles, int[][] corners, Material core, Material candle,
                                    Material... pillar) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(2, 0, 2, core));
        for (int[] at : candles) {
            parts.add(new Part(at[0], at[1], at[2], candle));
        }
        for (int[] corner : corners) {
            for (int y = 0; y < pillar.length; y++) {
                parts.add(new Part(corner[0], y, corner[1], pillar[y]));
            }
        }
        return parts;
    }

    private static List<Part> pantheon(Pantheon pantheon) {
        Material[] pillar = new Material[PantheonAltarStructure.PILLAR_HEIGHT];
        Arrays.fill(pillar, pantheon.pillar());
        return altar(PantheonAltarStructure.CANDLE_OFFSETS, PantheonAltarStructure.CORNER_OFFSETS,
                pantheon.core(), pantheon.candle(), pillar);
    }

    /** The Wither's T of soul sand under its three skulls, the foot of the T on the storm's core block. */
    private static List<Part> witherStorm(Material core) {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(1, 0, 0, core));
        parts.add(new Part(1, 1, 0, Material.SOUL_SAND));
        for (int x = 0; x < 3; x++) {
            parts.add(new Part(x, 2, 0, Material.SOUL_SAND));
            parts.add(new Part(x, 3, 0, Material.WITHER_SKELETON_SKULL));
        }
        return parts;
    }

    /**
     * {@code parts} with x and z swapped for a player looking east or west, so a flat structure such as the
     * Wither's T stands across their view. Every other structure is the same both ways.
     */
    static List<Part> turnedFor(List<Part> parts, BlockFace facing) {
        if (facing.getModX() == 0) return parts;
        List<Part> turned = new ArrayList<>(parts.size());
        for (Part part : parts) {
            turned.add(new Part(part.z(), part.y(), part.x(), part.material()));
        }
        return turned;
    }

    /** Where a stair faces: towards the middle of a width x depth footprint, along x when it sits on a diagonal. */
    static BlockFace inwards(int x, int z, int width, int depth) {
        double dx = (width - 1) / 2.0 - x;
        double dz = (depth - 1) / 2.0 - z;
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    /** The shape Minecraft gives the stair at {@code at} from the stairs beside it (StairBlock#getStairsShape). */
    static Stairs.Shape shape(Map<Cell, BlockFace> stairs, Cell at) {
        BlockFace facing = stairs.get(at);
        BlockFace front = stairs.get(at.step(facing));
        if (front != null && crosses(front, facing) && stairs.get(at.step(front.getOppositeFace())) != facing) {
            return front == counterClockwise(facing) ? Stairs.Shape.OUTER_LEFT : Stairs.Shape.OUTER_RIGHT;
        }
        BlockFace back = stairs.get(at.step(facing.getOppositeFace()));
        if (back != null && crosses(back, facing) && stairs.get(at.step(back)) != facing) {
            return back == counterClockwise(facing) ? Stairs.Shape.INNER_LEFT : Stairs.Shape.INNER_RIGHT;
        }
        return Stairs.Shape.STRAIGHT;
    }

    private static boolean crosses(BlockFace a, BlockFace b) {
        return (a.getModX() != 0) != (b.getModX() != 0);
    }

    private static BlockFace counterClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            default -> BlockFace.NORTH;
        };
    }

    static boolean isStairs(Material material) {
        return material.name().endsWith("_STAIRS");
    }

    private static boolean isHead(Material material) {
        return material == Material.SKELETON_SKULL || material == Material.WITHER_SKELETON_SKULL;
    }
}
