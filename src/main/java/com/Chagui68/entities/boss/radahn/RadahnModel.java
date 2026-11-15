package com.Chagui68.entities.boss.radahn;

import com.Chagui68.entities.boss.rig.Rig;
import com.Chagui68.entities.boss.rig.Rig.Ease;
import com.Chagui68.entities.boss.rig.Rig.Move;
import com.Chagui68.entities.boss.rig.Rig.MoveBuilder;
import com.Chagui68.entities.boss.rig.RigForm;
import com.Chagui68.utils.DisplaySculpt.Piece;
import com.Chagui68.utils.HeadSculpt;
import com.Chagui68.utils.HeadSculpt.Art;
import com.Chagui68.utils.HeadSculpt.Box;
import org.bukkit.Color;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.Chagui68.utils.DisplaySculpt.noise;
import static com.Chagui68.utils.DisplaySculpt.v;

/**
 * Starscourge Radahn: a warlord in engraved gold plate astride a horse far too small for him.
 * A tall lion-faced helm with a tusk at either cheek under a mane of red locks, a dark pelt and a
 * rust mantle round his neck, fur under strongbox pauldrons, lion-faced greaves, a torn cape, a
 * greatbow and two giant arrows on his back, and in each fist a bent black greatsword inlaid with gold.
 *
 * <p>He faces {@code +z} with the horse's hooves on the origin, his right side on {@code -x}. Every
 * piece belongs to a bone of {@link #RIG}. The horse carries him: its legs are the skeleton's walking
 * legs (the forelegs {@code LEG_R} and {@code SHIN_R}, the hind legs {@code LEG_L} and {@code SHIN_L}),
 * and {@code MOUNT} turns horse and rider about the hind hooves, so it can rear. He rests the way he
 * stands guard, arms out and blades raised, so his moves turn the swords down from there
 * ({@code HAND_R}, {@code HAND_L}). The cape rides {@code CAPE}, which trails on its own.</p>
 *
 * <p>His weapons change hands ({@link Rig.Carry}): each sword is a part of its own on the hand that
 * holds it ({@code SWORD_R}, {@code SWORD_L}) and the bow one on his back ({@code BOW}). As a move
 * turns the dial {@code DRAW} to 100 the swords go to his back and the bow to his left hand. The two
 * arrows in his back ({@code ARROW}, {@code ARROW_2}) go to his right hand on {@code TAKE} and
 * {@code TAKE_2}, and are spent ({@link Rig.Spend}) on {@code LOOSE} and {@code LOOSE_2} as he looses.</p>
 *
 * <p>He is drawn with textured player heads ({@link HeadSculpt}): a plate is a few heads wide and
 * its engraving is the pixel art of their skins, painted here. A head only shows a skin Mojang
 * hosts, so after the art changes the skins are uploaded again
 * ({@code website/models/stand-skins/heads.py}, which writes {@code radahn/skins.txt}).</p>
 */
public final class RadahnModel {

    // Sand gold, from its highlight down to the brown its engraving is cut in.
    private static final int PALE = 0xe6d795, LIGHT = 0xd2b876, GOLD = 0xbe9d60, SHADE = 0xa07a4a, OCHRE = 0x8a613d, LINE = 0x613729;
    private static final int BONE = 0xd6cfba, BONE_MID = 0xbdb39a, BONE_SHADE = 0x9a8c74;
    private static final int FUR = 0xcfc9c2, FUR_MID = 0xaaa3a2, FUR_SHADE = 0x878086;
    private static final int PELT = 0x2a2225, PELT_MID = 0x3b3437, PELT_LIGHT = 0x59535a;
    private static final int MANE = 0x80322d, MANE_LIGHT = 0xa5452f, MANE_BRIGHT = 0xc8643c, MANE_DARK = 0x691f23, MANE_DEEP = 0x4a1219;
    private static final int CLOTH = 0x5a2d2b, CLOTH_LIGHT = 0x6b3a35, CLOTH_DARK = 0x45211f, CLOTH_DEEP = 0x301718;
    private static final int WRAP = 0x3c3334, WRAP_LIGHT = 0x4c4243, WRAP_DARK = 0x2c2426;
    private static final int STEEL = 0x4d4856, STEEL_LIGHT = 0x6d6470, STEEL_DARK = 0x2b2730;
    private static final int BLADE = 0x25232a, BLADE_DEEP = 0x17161b, BLADE_RIM = 0x3a3840, BLADE_EDGE = 0x5a5863, INLAY = 0xb89554;
    private static final int LEATHER = 0x492e30, LEATHER_LIGHT = 0x5e423f, LEATHER_DARK = 0x3c2324;
    private static final int HIDE = 0x7b412e, HIDE_LIGHT = 0x935a3f, HIDE_DARK = 0x512823, HIDE_DEEP = 0x381e1c;
    private static final int WOOD = 0x6b4a36, WOOD_DARK = 0x4e3426;
    private static final int SLATE = 0x2c2c36, SLATE_LIGHT = 0x3d3d49, SLATE_DARK = 0x1e1e26;

    /** The inks of the pixel art below; any other character leaves the plate as it is. */
    private static final Map<Character, Integer> INK = Map.ofEntries(
            Map.entry('P', PALE), Map.entry('L', LIGHT), Map.entry('G', GOLD), Map.entry('S', SHADE), Map.entry('O', OCHRE), Map.entry('o', LINE),
            Map.entry('s', STEEL), Map.entry('t', STEEL_LIGHT), Map.entry('k', STEEL_DARK),
            Map.entry('m', CLOTH), Map.entry('n', CLOTH_DARK),
            Map.entry('u', HIDE), Map.entry('U', HIDE_LIGHT), Map.entry('d', HIDE_DARK), Map.entry('D', HIDE_DEEP),
            Map.entry('l', SLATE), Map.entry('j', INLAY));

    /**
     * The left half of the helm's face: an eye boxed in under the brow beside the pale crest, the
     * cheek arched round the muzzle, a row of teeth, the steel guard across the mouth, and a keyed jaw.
     */
    private static final String[] FACE = {
            "ooooooPP",
            "oOOOOoPP",
            "oOPPOoPP",
            "oOPPOoPP",
            "oOOOOoLP",
            "ooooooGL",
            "L..o....",
            "L..o..PP",
            "L...o.PP",
            "L...o...",
            "L....o..",
            "Loo...oo",
            "PPoPPoPP",
            "kkkkkkkk",
            "sssssstt",
            "ksssssts",
            "oooook.t",
            "o...okPt",
            "o.o.okks",
            "o.o.okkk",
            "o.oookkk",
            "o....ooo",
            "o.oo....",
            "oooooooo"};
    /** The left half of the lion on a greave: its frame, a pale brow and nose over a dark eye, a mouth turned down, a fret under its chin. */
    private static final String[] LION = {
            "oooooooo",
            "o.......",
            "o.oooooo",
            "o.o.....",
            "o.o.PPPP",
            "o.o.PooP",
            "o.o....P",
            "o.o...PP",
            "o.o..ooo",
            "o.o..o..",
            "o.o.....",
            "o.o.oo.o",
            "o.o.o...",
            "o.oooooo",
            "o.......",
            "oooooooo"};
    /**
     * The left half of his chest: the rust mantle with its brooch and chain, then three of the
     * breastplate's six lobes, each with a pale ridge and a point turned to his middle, the outer
     * ones shorter. The innermost runs on down his waist.
     */
    private static final String[] BREAST = {
            "mmmmnmmmmmmmmmmm",
            "mnmmmmmmooooommm",
            "mmmmmnmmoPPPomGm",
            "mmnmmmmmoPPPommG",
            "mmmmmmnmooooommm",
            "noooonoooonoooon",
            "oLLLLoLLLLoLLLLo",
            "oLPPGoLPPGoLPPGo",
            "oLPPGoLPPGoLPPGo",
            "oGPPGoLPPGoLPPGo",
            "oGPGSoLPPGoLPPGo",
            "ooGSSoGPPGoLPPGo",
            ".oooooGPPGoGPPGo",
            "......oGGSoGPPGo",
            "......ooSSoGPPGo",
            ".......ooooGGPGo"};
    /** The left half of his waist: the point of the innermost lobe, the chevron that crosses under it, and a toothed rim. */
    private static final String[] WAIST = {
            "......oGGGGo",
            "......oGGGSo",
            ".......oGSSo",
            "oo......oooo",
            "..ooo.......",
            ".....ooo....",
            "oooooooooooo",
            "PoPoPoPoPoPo"};
    /** The inlay of a blade, from its point down to its guard: two feelers, a comb, an eye in its rings, a comb, a stem. */
    private static final String[] INLAY_ART = {
            "..j...j..",
            "..j...j..",
            "..j...j..",
            "j.j.j.j.j",
            "jjjjjjjjj",
            "..j.j.j..",
            "jjjjjjjjj",
            "j.j.j.j.j",
            "....j....",
            "..jjjjj..",
            ".j.....j.",
            ".j.jjj.j.",
            ".j.j.j.j.",
            ".j.jjj.j.",
            ".j.....j.",
            "..jjjjj..",
            "....j....",
            "j.j.j.j.j",
            "jjjjjjjjj",
            "..j.j.j..",
            "jjjjjjjjj",
            "j.j.j.j.j",
            "....j....",
            "....j...."};
    /** The horse's brow: the stripes of its forelock, its eyes either side of a blaze. */
    private static final String[] BROW = {
            "dUUUUUUd",
            "duuuuuud",
            "dDDDDDDd",
            "duuuuuud",
            "dDDDDDDd",
            "duuuuuud",
            "dDDDDDDd",
            "duuuuuud",
            "dDDDDDDd",
            "duuuuuud",
            "dDDuuDDd",
            "kuuPPuuk",
            "duuPPuud",
            "duuPPuud",
            "duuPPuud",
            "duuPPuud"};
    /** The horse's muzzle: a gold noseband and its buckle, the end of the blaze, nostrils, the line of its mouth. */
    private static final String[] MUZZLE = {
            "GGGGGGGG",
            "oGGPPGGo",
            "GGGGGGGG",
            "duuPPuud",
            "duuPPuud",
            "duuuuuud",
            "duuuuuud",
            "duuuuuud",
            "duuuuuud",
            "dUuuuuUd",
            "dkuuuukd",
            "dkuuuukd",
            "duuuuuud",
            "dDDDDDDd",
            "duuuuuud",
            "dddddddd"};
    /** A flap of his loincloth: slate, a gold cross on it, gold along its outer edge and its hem. */
    private static final String[] FLAP = {
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "GlllGlll",
            "GllGGGll",
            "GlllGlll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "Glllllll",
            "GGGGGGGG",
            "oooooooo"};

    /** The mane's streaks, from the dark of its roots to its brightest: a lock is darker at its edges than down its middle. */
    private static final int[][] HAIR = {
            {MANE_DEEP, MANE_DARK, MANE_DEEP, MANE_DARK, MANE_DARK, MANE_DEEP, MANE_DARK, MANE_DEEP},
            {MANE_DARK, MANE, MANE_DARK, MANE, MANE, MANE_DARK, MANE, MANE_DARK},
            {MANE_DARK, MANE, MANE_LIGHT, MANE_BRIGHT, MANE_LIGHT, MANE, MANE, MANE_DARK},
            {MANE, MANE_LIGHT, MANE_BRIGHT, MANE_BRIGHT, MANE_LIGHT, MANE, MANE_LIGHT, MANE}};
    private static final int[] SIDES = {-1, 1};
    private static final Vector3f X = v(1, 0, 0), Y = v(0, 1, 0);
    private static final Quaternionf LEVEL = new Quaternionf();

    private static final Vector3f SHOULDER_R = v(-.66, 2.52, 0), ELBOW_R = v(-1.02, 1.95, .08), WRIST_R = v(-1.48, 2.00, .38);
    private static final Vector3f SHOULDER_L = v(.66, 2.52, 0), ELBOW_L = v(1.02, 1.95, .08), WRIST_L = v(1.48, 2.00, .38);
    /** The horse's legs turn here: the forelegs' shoulder and knee, the hind legs' hip and hock. */
    private static final Vector3f FORE = v(0, .64, .26), FORE_KNEE = v(0, .34, .27), HIND = v(0, .64, -.58), HIND_HOCK = v(0, .34, -.57);

    /** A blade runs straight from its grip to its bend, this far along; past it, it turns this much back towards the body. */
    private static final float BEND = 1.46f, TURN = .384f;
    /** The bow on his back: how far behind him it hangs, its two tips, and the handle its limbs meet at. */
    private static final float BOW_Z = -.42f;
    private static final Vector3f BOW_TOP = v(-.92, 3.92, BOW_Z), BOW_LOW = v(1.40, 1.33, BOW_Z), BOW_HANDLE = v(-.34, 1.78, BOW_Z);
    /** The two arrows stood in his back, each from its head to the top of its shaft, where its nock is. */
    private static final Vector3f[][] SHAFTS = {{v(.46, 2.72, -.34), v(1.16, 3.66, -.70)}, {v(.78, 2.52, -.34), v(1.80, 3.12, -.62)}};
    /**
     * How an arrow lies in his right hand: the way it points, at the bow's grip, and the way its
     * feathers hang. Fitted at full draw; his hand is turned to keep it so in every pose he shoots from.
     */
    private static final Vector3f SHOT = v(-.966, -.224, -.128), FLETCH = v(.176, -.958, .228);
    /** How far the second arrow of a volley fans out from the first, in radians. */
    private static final float FAN = .16f;

    public static final Rig RIG = Rig.skeleton(.64f)
            .bone("ROOT", null, v(0, 1.60, 0))
            .bone("DRAW", "ROOT", v(0, 0, 0))
            .bone("TAKE", "ROOT", v(0, 0, 0)).bone("TAKE_2", "ROOT", v(0, 0, 0))
            .bone("LOOSE", "ROOT", v(0, 0, 0)).bone("LOOSE_2", "ROOT", v(0, 0, 0))
            .bone("MOUNT", "ROOT", v(0, 0, -.58))
            .bone("HORSE", "MOUNT", v(0, .92, .32))
            .bone("PELVIS", "MOUNT", v(0, 1.25, 0))
            .bone("CHEST", "PELVIS", v(0, 1.45, 0))
            .bone("HEAD", "CHEST", v(0, 2.38, 0))
            .bone("CAPE", "CHEST", v(0, 2.34, -.50))
            .bone("BOW", "CHEST", BOW_HANDLE)
            .bone("ARROW", "CHEST", nock(0)).bone("ARROW_2", "CHEST", nock(1))
            .bone("ARM_R", "CHEST", SHOULDER_R).bone("FOREARM_R", "ARM_R", ELBOW_R).bone("HAND_R", "FOREARM_R", WRIST_R)
            .bone("SWORD_R", "HAND_R", grip(-1))
            .bone("ARM_L", "CHEST", SHOULDER_L).bone("FOREARM_L", "ARM_L", ELBOW_L).bone("HAND_L", "FOREARM_L", WRIST_L)
            .bone("SWORD_L", "HAND_L", grip(1))
            .bone("LEG_R", "MOUNT", FORE).bone("SHIN_R", "LEG_R", FORE_KNEE)
            .bone("LEG_L", "MOUNT", HIND).bone("SHIN_L", "LEG_L", HIND_HOCK)
            // The weapons change hands on the dial DRAW: the bow to his left hand, each sword to his back.
            .carry("BOW", "HAND_L", "DRAW", drawn())
            .carry("SWORD_R", "CHEST", "DRAW", stowed(-1))
            .carry("SWORD_L", "CHEST", "DRAW", stowed(1))
            // An arrow goes from his back to his right hand on TAKE, and is spent on LOOSE as the real one flies.
            .carry("ARROW", "HAND_R", "TAKE", nocked(0)).carry("ARROW_2", "HAND_R", "TAKE_2", nocked(1))
            .spend("ARROW", "LOOSE").spend("ARROW_2", "LOOSE_2")
            .build();

    /** Riding: what every bone of the rider a move keys holds at the gallop, so a move begins and ends in the saddle at speed. */
    private static final String RIDE = "ROOT 0 0 0; CHEST 10 0 0; HEAD -6 0 0; CAPE 38 0 0; DRAW 0 0 0; "
            + "ARM_R 0 0 0; FOREARM_R 0 0 0; HAND_R 0 0 0; ARM_L 0 0 0; FOREARM_L 0 0 0; HAND_L 0 0 0";
    /** The horse at a halt, for the moves that rear it; the moves at the gallop key it every tick ({@link #horse}). */
    private static final String STAND = "; MOUNT 0 0 0; HORSE 4 0 0; LEG_R 0 0 0; SHIN_R 0 0 0; LEG_L 0 0 0; SHIN_L 0 0 0";
    /** His arms thrown back over his shoulders: where a sword is put away and the bow is taken. */
    private static final String REACH = "ARM_R -150 0 -20; FOREARM_R -40 0 0; HAND_R 0 0 0; ARM_L -150 0 20; FOREARM_L -40 0 0; HAND_L 0 0 0";
    /**
     * Archery from the saddle. He nocks with the bow held low and canted before him, his right hand at
     * its string. At full draw he sits side on, his left shoulder to the mark, his bow arm out at it and
     * his right hand at his cheek, its elbow back at his shoulder; loosed, that hand flies back past his
     * ear. At a man, and at the sky. The arms were fitted so they stay clear of his breastplate and helm.
     */
    private static final String NOCK = "CHEST 10 -24 0; HEAD 6 20 0; ARM_L -64 -23 -16; FOREARM_L 27 -1 -60; HAND_L 24 73 85; "
            + "ARM_R -61 25 18; FOREARM_R -28 90 56; HAND_R 38 -9 22";
    private static final String AIM_LEVEL = "CHEST 4 -50 0; HEAD -4 44 0; ARM_L -65 -6 2; FOREARM_L -21 -34 -62; HAND_L 13 85 58; "
            + "ARM_R -93 15 10; FOREARM_R -43 110 66; HAND_R 0 0 0";
    private static final String LOOSE_LEVEL = "CHEST 4 -50 0; HEAD -4 44 0; ARM_L -72 -10 -2; FOREARM_L -8 -29 -64; HAND_L 9 81 60; "
            + "ARM_R -92 -1 -6; FOREARM_R -69 113 62; HAND_R 0 0 0";
    private static final String AIM_SKY = "CHEST -18 -45 0; HEAD 14 40 0; ARM_L -102 -23 -16; FOREARM_L 30 -3 -60; HAND_L 8 74 115; "
            + "ARM_R -66 14 4; FOREARM_R -20 98 68; HAND_R 2 30 -44";
    private static final String LOOSE_SKY = "CHEST -18 -45 0; HEAD 14 40 0; ARM_L -100 -23 -18; FOREARM_L 29 -1 -60; HAND_L 4 73 116; "
            + "ARM_R -50 -1 -2; FOREARM_R -34 102 64; HAND_R 2 30 -44";
    /** A stride of the gallop, in ticks, and how far he rides in a tick of a riding cut: his speed in the saddle. */
    private static final int GALLOP = 12;
    private static final float PACE = .4f;
    /** At the gallop the forelegs land this far into a stride (a third of it) after the hind legs. */
    private static final double LAG = Math.PI * 2 / 3;

    // He never leaves the saddle: PELVIS is not keyed, the horse's legs do the running, and MOUNT rears
    // the two of them. The blades point up at rest: a hand's positive pitch tips its sword forward and
    // down. DRAW is the dial of Rig.Carry: at 100 the bow is in his left hand and the swords are on his back.
    public static final List<Move> MOVES = List.of(
            RIG.move("idle", "Loom", 80)
                    .key(0, Ease.SMOOTH, "")
                    .key(40, Ease.SMOOTH, "CHEST 3 0 0; HEAD -3 0 0; HORSE 7 0 0; ARM_R 0 0 -4; ARM_L 0 0 4; move 0 .03 0")
                    .key(80, Ease.SMOOTH, "CHEST 0 0 0; HEAD 0 0 0; HORSE 0 0 0; ARM_R 0 0 0; ARM_L 0 0 0; move 0 0 0")
                    .build(),
            run(),
            // Riding by: the blade goes back on his sword side and comes round across the horse's neck.
            horse(RIG.move("sweep", "Crescent sweep", 30), 30, GALLOP, 32, PACE)
                    .key(0, Ease.SMOOTH, RIDE)
                    .key(10, Ease.SMOOTH, "CHEST 10 -55 0; HEAD -6 30 0; ARM_R 0 -35 -15; HAND_R 100 0 0; ARM_L -8 0 18")
                    .key(16, Ease.SNAP, "CHEST 10 60 0; HEAD -6 -22 0; ARM_R 0 50 0; HAND_R 106 0 0; ARM_L -4 0 28")
                    .key(22, Ease.SMOOTH, "CHEST 8 64 0; ARM_R 0 58 0")
                    .key(30, Ease.SMOOTH, RIDE)
                    .trail(10, 18).aim(4)
                    .build(),
            // The quick one: up over his sword shoulder and straight down past the horse's flank.
            horse(RIG.move("slash", "Riding cut", 22), 22, GALLOP, 32, PACE)
                    .key(0, Ease.SMOOTH, RIDE)
                    .key(7, Ease.SMOOTH, "CHEST 4 -28 0; HEAD -6 20 0; ARM_R -75 -25 -20; HAND_R 40 0 0; ARM_L -6 0 14")
                    .key(11, Ease.SNAP, "CHEST 18 34 0; HEAD -6 -12 0; ARM_R 12 34 0; HAND_R 112 0 0; ARM_L -4 0 22")
                    .key(15, Ease.SMOOTH, "CHEST 14 38 0; ARM_R 16 40 0")
                    .key(22, Ease.SMOOTH, RIDE)
                    .trail(7, 13).aim(3)
                    .build(),
            // He reins in and the horse rears; the blades cross over his head, where the hole opens that
            // drags them in; then horse and blades come down on whoever arrived.
            RIG.move("pull", "Event horizon", 64)
                    .key(0, Ease.SMOOTH, RIDE + STAND)
                    .key(12, Ease.SMOOTH, "MOUNT -12 0 0; HORSE -12 0 0; CHEST -10 0 0; HEAD 8 0 0; CAPE 10 0 0; ARM_R -35 0 -70; FOREARM_R -20 0 0; "
                            + "ARM_L -35 0 70; FOREARM_L -20 0 0; LEG_R -42 0 0; SHIN_R 70 0 0; LEG_L 12 0 0")
                    .key(44, Ease.SMOOTH, "MOUNT -14 0 0; CHEST -14 0 0; ARM_R -50 0 -60; ARM_L -50 0 60; LEG_R -48 0 0; SHIN_R 76 0 0; LEG_L 14 0 0")
                    .key(50, Ease.SNAP, "MOUNT 3 0 0; HORSE 12 0 0; CHEST 28 0 0; HEAD -14 0 0; ARM_R 0 40 0; FOREARM_R 0 0 0; HAND_R 84 0 0; "
                            + "ARM_L 0 -40 0; FOREARM_L 0 0 0; HAND_L 76 0 0; LEG_R 0 0 0; SHIN_R 0 0 0; LEG_L -3 0 0")
                    .key(64, Ease.SMOOTH, RIDE + STAND)
                    .key(0, Ease.LINEAR, "move 0 0 0").key(10, Ease.SMOOTH, "move 0 0 1.6").key(64, Ease.LINEAR, "move 0 0 1.6")
                    .cue(12, "pull").ring(50).aim(10)
                    .build(),
            archery("shot", "Great arrow", 40, AIM_LEVEL, LOOSE_LEVEL, 1),
            archery("volley", "Fan of arrows", 28, AIM_LEVEL, LOOSE_LEVEL, 2),
            archery("rain", "Arrow rain", 34, AIM_SKY, LOOSE_SKY, 2),
            // At half health: the horse rears, they leap for the sky and are gone (his boss hides them), and
            // they come down with the star, sixteen blocks in four ticks.
            RIG.move("cataclysm", "The falling star", 260)
                    .key(0, Ease.SMOOTH, RIDE + STAND)
                    .key(10, Ease.SMOOTH, "MOUNT -22 0 0; HORSE -18 0 0; CHEST -8 0 0; HEAD 14 0 0; CAPE 6 0 0; ARM_R -30 0 -30; ARM_L -30 0 30; "
                            + "LEG_R -50 0 0; SHIN_R 75 0 0; LEG_L 22 0 0")
                    .key(16, Ease.SNAP, "MOUNT -30 0 0; CAPE 60 0 0; ARM_R -80 0 -20; HAND_R 50 0 0; ARM_L -80 0 20; HAND_L 50 0 0; "
                            + "LEG_R -30 0 0; SHIN_R 40 0 0; LEG_L 30 0 0")
                    .key(228, Ease.SNAP, "MOUNT 3 0 0; HORSE 12 0 0; CHEST 26 0 0; HEAD -14 0 0; CAPE 20 0 0; ARM_R 0 40 0; HAND_R 84 0 0; "
                            + "ARM_L 0 -40 0; HAND_L 76 0 0; LEG_R 0 0 0; SHIN_R 0 0 0; LEG_L -3 0 0")
                    .key(260, Ease.SMOOTH, RIDE + STAND)
                    .key(0, Ease.LINEAR, "move 0 0 0").key(12, Ease.LINEAR, "move 0 0 0").key(20, Ease.LINEAR, "move 0 16 0")
                    .key(222, Ease.LINEAR, "move 0 16 0").key(226, Ease.LINEAR, "move 0 0 0")
                    .cue(12, "leap").cue(20, "vanish").cue(26, "meteor").cue(226, "impact").aim(0)
                    .build());

    private static final HeadSculpt HEADS = sculpt();
    private static final List<Piece> PIECES = List.copyOf(HEADS.pieces(uploaded()));

    /** The sword in his right hand is his weapon: from the foot of its blade to its point. */
    public static final RigForm FORM = RigForm.of("radahn", "Starscourge Radahn", RIG, PIECES, MOVES, "SWORD_R",
            at(grip(-1), axis(-1), .34f), at(bend(-1), upper(-1), .96f), Color.fromRGB(190, 110, 255));

    private RadahnModel() {
    }

    /**
     * A shot from the saddle. He throws his arms back over his shoulders, where the swords are put
     * away and the bow comes off his back (the dial {@code DRAW}), nocks ({@link #NOCK}), raises the bow
     * as he draws to {@code aim}, looses at {@code loose} into {@code release}, and changes back. He
     * takes {@code arrows} of the arrows in his back to the string (one, or two to fan) and spends them
     * as the real ones fly; they are back in his back as he puts the bow away. Its cues are
     * {@code nock}, when the arrow goes to the string, and the move's own name, when it flies.
     */
    private static Move archery(String id, String label, int loose, String aim, String release, int arrows) {
        int end = loose + 30;
        String take = arrows > 1 ? "TAKE %1$d 0 0; TAKE_2 %1$d 0 0" : "TAKE %1$d 0 0";
        String spend = arrows > 1 ? "LOOSE %1$d 0 0; LOOSE_2 %1$d 0 0" : "LOOSE %1$d 0 0";
        // He keeps the horse at a canter while he shoots.
        return horse(RIG.move(id, label, end), end, 16, 24, .12f)
                .key(0, Ease.SMOOTH, RIDE)
                .key(7, Ease.SMOOTH, "CHEST 2 0 0; HEAD -4 0 0; " + REACH)
                .key(8, Ease.LINEAR, "DRAW 0 0 0")
                .key(13, Ease.LINEAR, "DRAW 100 0 0")
                .key(13, Ease.LINEAR, take.formatted(0))
                .key(19, Ease.LINEAR, take.formatted(100))
                .key(20, Ease.SMOOTH, NOCK)
                .key(loose - 2, Ease.SMOOTH, aim)
                .key(loose - 1, Ease.LINEAR, spend.formatted(0))
                .key(loose, Ease.LINEAR, spend.formatted(100))
                .key(loose, Ease.SNAP, release)
                // Spent, they go back to his back unseen, and show there again as the bow goes away.
                .key(loose + 2, Ease.LINEAR, take.formatted(100))
                .key(loose + 4, Ease.LINEAR, take.formatted(0))
                // Held as he watches it fly: without this key his arms would start back the moment he loosed.
                .key(loose + 10, Ease.SMOOTH, release)
                .key(loose + 17, Ease.SMOOTH, "CHEST 2 0 0; HEAD -4 0 0; " + REACH)
                .key(loose + 18, Ease.LINEAR, "DRAW 100 0 0")
                .key(loose + 18, Ease.LINEAR, spend.formatted(100))
                .key(loose + 19, Ease.LINEAR, spend.formatted(0))
                .key(loose + 23, Ease.LINEAR, "DRAW 0 0 0")
                .key(end, Ease.SMOOTH, RIDE)
                .cue(20, "nock").cue(loose, id).aim(loose - 4)
                .build();
    }

    /** The gallop on the spot, while his stand runs: the horse ({@link #horse}), and him riding it steadier than it, his head level. */
    private static Move run() {
        MoveBuilder run = horse(RIG.move("run", "Gallop", GALLOP), GALLOP, GALLOP, 32, 0);
        for (int t = 0; t <= GALLOP; t++) {
            double at = Math.PI * 2 * t / GALLOP, arms = 3 * Math.sin(at - 1.2);
            run.key(t, Ease.LINEAR, String.format(Locale.ROOT, "CHEST %.1f 0 0; HEAD %.1f 0 0; CAPE %.1f 0 0; ARM_R %.1f 0 -4; ARM_L %.1f 0 4",
                    10 + 3 * rock(at), -6 + rock(at), 42 + 5 * rise(at), arms, arms));
        }
        return run.build();
    }

    /**
     * The horse at the gallop for the {@code ticks} of a move, a stride every {@code period}, riding
     * {@code pace} blocks a tick. Each pair of legs swings {@code reach} degrees either way, the forelegs
     * {@link #LAG} behind the hind legs, and folds as it leaves the ground. The horse pitches nose up as
     * its forelegs reach and down as they take the weight, its head lowest over them, and rises into the
     * air as they push off. Keyed every tick, evenly, so nothing stops between keys.
     */
    private static MoveBuilder horse(MoveBuilder move, int ticks, int period, float reach, float pace) {
        double size = reach / 32;
        for (int t = 0; t <= ticks; t++) {
            double hind = Math.PI * 2 * t / period, fore = hind - LAG;
            move.key(t, Ease.LINEAR, String.format(Locale.ROOT,
                    "LEG_R %.1f 0 0; SHIN_R %.1f 0 0; LEG_L %.1f 0 0; SHIN_L %.1f 0 0; MOUNT %.1f 0 0; HORSE %.1f 0 0; move 0 %.3f %.3f",
                    -reach * Math.cos(fore), 2.2 * reach * fold(fore), -.85 * reach * Math.cos(hind), 1.5 * reach * fold(hind),
                    -4 * size * rock(hind), 4 + 7 * size * Math.cos(hind - Math.PI * 13 / 12), size * (.05 + .05 * rise(hind)), t * pace));
        }
        return move;
    }

    /** At {@code at} radians into a stride: 1 as the horse pitches nose up, its forelegs reaching; -1 nose down over them. */
    private static double rock(double at) {
        return Math.cos(at - LAG / 2 - .6);
    }

    /** At {@code at} radians into a stride: 1 as horse and rider are highest, in the air after the forelegs push off. */
    private static double rise(double at) {
        return Math.cos(at + Math.PI / 6);
    }

    /** How far a leg is folded at {@code phase} of its stride (0 as it lands, pi as it lifts): from just before it lifts, most early in its swing. */
    private static double fold(double phase) {
        return Math.max(0, -Math.sin(phase + Math.PI / 4));
    }

    /** A rigid frame: {@code origin}, its second axis along {@code up}, its first as near {@code across} as that allows. */
    private static Matrix4f frame(Vector3f origin, Vector3f up, Vector3f across) {
        Vector3f y = new Vector3f(up).normalize();
        Vector3f x = new Vector3f(across).sub(new Vector3f(y).mul(across.dot(y))).normalize();
        Vector3f z = new Vector3f(x).cross(y);
        return new Matrix4f(x.x, x.y, x.z, 0, y.x, y.y, y.z, 0, z.x, z.y, z.z, 0, origin.x, origin.y, origin.z, 1);
    }

    /** The bow in his left hand: from where it hangs on his back to upright in his grip, its string towards his shoulder. */
    private static Matrix4f drawn() {
        Matrix4f rest = frame(BOW_HANDLE, new Vector3f(BOW_TOP).sub(BOW_LOW), mid(BOW_TOP, BOW_LOW).sub(BOW_HANDLE));
        return frame(grip(1), Y, new Vector3f(SHOULDER_L).sub(grip(1))).mul(rest.invert());
    }

    /**
     * A sword put away: from his hand to his back, its grip over the shoulder of the hand that holds
     * it, its blade down across his back and its point hanging by the horse's flank. The two cross, a
     * little apart, where the bow hung.
     */
    private static Matrix4f stowed(int side) {
        Matrix4f rest = frame(grip(side), axis(side), outward(side));
        return frame(v(side * .50, 2.74, side < 0 ? -.40 : -.47), v(-side * .62, -.78, 0), v(-side * .78, .62, 0)).mul(rest.invert());
    }

    /** Where an arrow in his back is nocked: the top of its shaft, over his shoulder. */
    private static Vector3f nock(int n) {
        Vector3f[] shaft = SHAFTS[n];
        return at(shaft[1], new Vector3f(shaft[1]).sub(shaft[0]).normalize(), .07f);
    }

    /**
     * An arrow from his back in his right hand: nocked at his fist, pointing along the draw
     * ({@link #SHOT}), its feathers hanging ({@link #FLETCH}). The second fans out beside the first.
     */
    private static Matrix4f nocked(int n) {
        Vector3f[] shaft = SHAFTS[n];
        Vector3f head = new Vector3f(shaft[0]).sub(shaft[1]).normalize();
        Matrix4f rest = frame(nock(n), head, v(0, -1, 0).add(new Vector3f(head).mul(head.y)));
        Vector3f up = new Vector3f(FLETCH).negate().normalize();
        Vector3f way = new Quaternionf().rotateAxis(n * FAN, up).transform(new Vector3f(SHOT));
        Vector3f at = at(at(grip(-1), SHOT, -.12f), new Vector3f(SHOT).cross(up).normalize(), n * .09f);
        return frame(at, way, FLETCH).mul(rest.invert());
    }

    /** The textures his skins were uploaded as, by skin name: a line of {@code name=texture} each in {@code radahn/skins.txt}. */
    private static Map<String, String> uploaded() {
        Map<String, String> textures = new HashMap<>();
        try (InputStream in = RadahnModel.class.getResourceAsStream("/radahn/skins.txt")) {
            if (in == null) return textures;
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                int cut = line.indexOf('=');
                if (cut > 0) textures.put(line.substring(0, cut), line.substring(cut + 1).trim());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return textures;
    }

    /** His skins by name, each the 64x64 ARGB pixels of a player skin: what is uploaded for his heads to show. */
    public static Map<String, int[]> skins() {
        return HEADS.skins();
    }

    private static HeadSculpt sculpt() {
        HeadSculpt s = new HeadSculpt();
        horse(s);
        legs(s);
        hips(s);
        trunk(s);
        collar(s);
        pauldrons(s);
        head(s);
        mane(s);
        arms(s);
        swords(s);
        cape(s);
        bow(s);
        arrows(s);
        return s;
    }

    // =====================================================================================
    // The horse
    // =====================================================================================

    /**
     * The horse that carries him: a thin dark barrel under his hips, a long tawny head hung low
     * between his boots with a striped forelock and a blaze, a strap and its buckle across its chest,
     * legs like sticks. The forelegs and the hind legs each stand a little apart, so a pair striding
     * together still reads as two legs.
     */
    private static void horse(HeadSculpt s) {
        s.part("MOUNT");
        Box barrel = s.box(v(0, .80, -.17), v(.40, .36, 1.08), LEVEL, 1, 1, 3, HIDE_DARK);
        for (Art a : barrel.faces()) coat(a, HIDE_DARK, HIDE_DEEP, HIDE, 3);
        barrel.front.rect(0, 3, 8, 2, WRAP).rect(4, 2, 3, 4, GOLD).rect(5, 3, 1, 2, LINE);
        for (Art a : s.box(v(0, .86, -.62), v(.36, .30, .26), new Quaternionf().rotateX(.35f), 1, 1, 1, HIDE_DARK).faces()) coat(a, HIDE_DARK, HIDE_DEEP, HIDE, 3);
        for (Art a : s.bar(v(0, .62, -.90), v(0, .92, -.72), .11f, .11f, X, 1, WRAP_DARK).faces()) streaks(a, WRAP_DARK, WRAP, PELT);
        for (Art a : s.bar(v(0, .26, -.96), v(0, .64, -.89), .08f, .08f, X, 1, WRAP_DARK).faces()) streaks(a, WRAP_DARK, WRAP, PELT);

        s.part("HORSE");
        for (Art a : s.bar(v(0, .86, .30), v(0, 1.10, .58), .28f, .30f, X, 1, HIDE).faces()) coat(a, HIDE, HIDE_DARK, HIDE_LIGHT, 5);
        // Drawn from the nose up, so the art on its face stands the right way.
        Vector3f nose = v(0, .10, .76), cheek = v(0, .60, .715), poll = v(0, 1.14, .62);
        Box muzzle = s.bar(nose, v(0, .62, .71), .23f, .22f, X, 2, HIDE), brow = s.bar(cheek, poll, .30f, .26f, X, 2, HIDE);
        for (Box b : List.of(muzzle, brow)) for (Art a : b.faces()) coat(a, HIDE, HIDE_DARK, HIDE_LIGHT, 5);
        brow.front.draw(0, 0, INK, BROW);
        muzzle.front.draw(0, 0, INK, MUZZLE);
        // The noseband goes round.
        for (Art a : List.of(muzzle.left, muzzle.right, muzzle.back)) a.rect(0, 0, 8, 3, GOLD).rect(0, 1, 8, 1, LINE);
        for (int side : SIDES) {
            for (Art a : put(s.box(v(-.11, 1.20, .60), v(.07, .15, .06), new Quaternionf().rotateZ(.25f), 1, 1, 1, HIDE), side).faces()) coat(a, HIDE, HIDE_DARK, HIDE_LIGHT, 5);
        }

        for (int side : SIDES) {
            float x = side * .14f, lead = side * .04f;
            s.part("LEG_R");
            // The legs stand a hair proud of the barrel, so the two share no face.
            leg(s.bar(v(x, .32, .27 + lead), v(x, .70, .26), .13f, .13f, X, 1, HIDE_DARK), false);
            s.part("SHIN_R");
            leg(s.bar(v(x, 0, .26 + lead), v(x, .36, .27 + lead), .085f, .085f, X, 1, HIDE_DARK), true);
            s.part("LEG_L");
            leg(s.bar(v(x, .32, -.57 - lead), v(x, .70, -.58), .13f, .15f, X, 1, HIDE_DARK), false);
            s.part("SHIN_L");
            leg(s.bar(v(x, 0, -.58 - lead), v(x, .36, -.57 - lead), .085f, .085f, X, 1, HIDE_DARK), true);
        }
    }

    /** A leg of the horse; a shin ends in a dark hoof. */
    private static void leg(Box leg, boolean hoof) {
        for (Art a : leg.faces()) coat(a, HIDE_DARK, HIDE_DEEP, HIDE, 3);
        if (!hoof) return;
        for (Art a : leg.sides()) a.rect(0, 6, 8, 2, WRAP_DARK);
        leg.bottom.fill(WRAP_DARK);
    }

    // =====================================================================================
    // The rider
    // =====================================================================================

    /**
     * His legs, astride: leather thighs with a gold stud, a greave like a strongbox with a lion on
     * its face and a fringe of fur under it, and a gold boot on a dark sole, banded with spirals.
     */
    private static void legs(HeadSculpt s) {
        s.part("PELVIS");
        for (int side : SIDES) {
            Box thigh = put(s.bar(v(-.64, .86, .26), v(-.28, 1.18, .02), .30f, .30f, v(0, 0, 1), 2, LEATHER), side);
            for (Art a : thigh.faces()) coat(a, LEATHER, LEATHER_DARK, LEATHER_LIGHT, 7);
            for (Art a : thigh.sides()) a.draw(2, 6, INK, ".GG.", "GPPG", "GPPG", ".GG.");

            Vector3f at = v(side * .76, .81, .30), size = v(.42, .50, .44);
            Box greave = gild(put(s.box(v(-.76, .81, .30), size, LEVEL, 2, 2, 2, GOLD), side));
            groove(greave.front.draw(0, 0, INK, LION).mirror());
            greave.back.copy(greave.front, false);
            groove(spiral(border(greave.right, 0, LINE), 2, 2, 12, 12, false));
            flute(greave.top);
            fringe(s, corner(at, size, LEVEL, -1, -1, 1), corner(at, size, LEVEL, 1, -1, 1), 5, .22f, v(0, -1, .25), .5f, 10 + side);
            fringe(s, corner(at, size, LEVEL, side, -1, 1), corner(at, size, LEVEL, side, -1, -1), 5, .20f, v(side * .3, -1, 0), .3f, 30 + side);

            Box boot = gild(put(s.box(v(-.72, .205, .32), v(.50, .41, .58), LEVEL, 2, 2, 2, GOLD), side));
            for (Art a : boot.sides()) {
                a.rect(0, 1, 16, 1, LINE).rect(0, 11, 16, 1, LINE);
                spiral(spiral(a, 0, 3, 7, 7, false), 9, 3, 7, 7, true);
                groove(a).rect(0, 14, 16, 2, SLATE).rect(0, 14, 16, 1, SLATE_LIGHT);
            }
            boot.bottom.fill(SLATE);
        }
    }

    /**
     * His hips: a gold belt cut with spirals over a toothed rim, the plate his loincloth hangs
     * from, gold and slate tabs, a slate flap with a gold cross at either side of them, and the
     * tassets: square plates sloping off the belt like roof tiles, a spiral on each and a fluted rim.
     */
    private static void hips(HeadSculpt s) {
        s.part("PELVIS");
        for (Art a : s.box(v(0, 1.17, -.02), v(.78, .36, .50), LEVEL, 2, 1, 1, LEATHER).faces()) coat(a, LEATHER, LEATHER_DARK, LEATHER_LIGHT, 7);
        Box belt = gild(s.box(v(0, 1.40, 0), v(.96, .22, .62), LEVEL, 4, 1, 2, GOLD));
        for (Art a : belt.sides()) {
            // The spirals of either half face the middle.
            for (int x = 0; x < a.w; x += 8) spiral(a, x, 0, 7, 5, x >= a.w / 2);
            groove(a.rect(0, 6, a.w, 1, LINE));
            for (int x = 0; x < a.w; x += 2) a.set(x, 7, PALE).set(x + 1, 7, LINE);
        }
        Box buckle = gild(s.box(v(0, 1.25, .30), v(.36, .12, .08), LEVEL, 1, 1, 1, GOLD));
        border(buckle.front, 0, LINE).rect(2, 3, 4, 2, PALE);
        for (Art a : s.box(v(0, 1.15, .27), v(.40, .10, .05), LEVEL, 1, 1, 1, CLOTH).faces()) drape(a, 0, a.h);
        for (int side : SIDES) {
            Box tab = gild(put(s.box(v(-.095, 1.03, .285), v(.16, .17, .05), LEVEL, 1, 1, 1, GOLD), side));
            for (Art a : List.of(tab.front, tab.back)) border(a, 0, LINE).rect(2, 2, 4, 4, PALE).rect(3, 3, 2, 2, LIGHT);
            for (Art a : put(s.box(v(-.095, .88, .28), v(.16, .13, .04), LEVEL, 1, 1, 1, SLATE), side).faces()) coat(a, SLATE, SLATE_DARK, SLATE_LIGHT, 23);
            Box flap = put(s.box(v(-.30, .97, .255), v(.17, .36, .04), LEVEL, 1, 2, 1, SLATE), side);
            flap.front.draw(0, 0, INK, FLAP);
            flap.back.copy(flap.front, true);

            Box tasset = gild(put(s.box(v(-.50, 1.22, .24), v(.46, .07, .46), new Quaternionf().rotateZ(.38f).rotateX(.62f), 2, 1, 2, GOLD), side));
            groove(spiral(border(tasset.top, 0, LINE), 2, 2, 12, 12, false));
            for (Art a : tasset.sides()) flute(a);
        }
    }

    /**
     * The cuirass: a waist crossed by a chevron over a toothed rim, and a breastplate of six
     * lobes fanned like a shell under the rust mantle with its two brooches and their chain
     * ({@link #BREAST}). The mantle goes on round his shoulders and across his back.
     */
    private static void trunk(HeadSculpt s) {
        s.part("CHEST");
        Box waist = gild(s.box(v(0, 1.62, 0), v(.86, .30, .54), LEVEL, 3, 1, 2, GOLD));
        groove(waist.front.draw(0, 0, INK, WAIST).mirror());
        for (Art a : List.of(waist.back, waist.left, waist.right)) {
            groove(a.rect(0, 6, a.w, 1, LINE));
            for (int x = 0; x < a.w; x += 2) a.set(x, 7, PALE).set(x + 1, 7, LINE);
        }

        Box chest = gild(s.box(v(0, 1.99, .02), v(1.08, .50, .66), LEVEL, 4, 2, 2, GOLD));
        groove(chest.front.draw(0, 0, INK, BREAST).mirror());
        for (Art a : List.of(chest.back, chest.left, chest.right)) drape(groove(border(a, 1, LINE)), 0, 5);
        drape(chest.top, 0, chest.top.h);

        // His neck under the pelt, and the mantle across his back and over either shoulder.
        for (Art a : s.box(v(0, 2.30, 0), v(.70, .14, .52), LEVEL, 1, 1, 1, PELT).faces()) coat(a, PELT, PELT, PELT_MID, 31);
        for (Art a : s.box(v(0, 2.24, -.30), v(.90, .22, .12), LEVEL, 2, 1, 1, CLOTH).faces()) drape(a, 1, a.h);
        for (int side : SIDES) {
            // A hair deeper than the breastplate, so the two share no face.
            for (Art a : put(s.box(v(-.50, 2.27, .02), v(.20, .16, .67), LEVEL, 1, 1, 1, CLOTH), side).faces()) drape(a, 0, a.h);
        }
    }

    /** A dark pelt round his neck: a ring of tufts turned this way and that, the mantle and its brooches showing under it. */
    private static void collar(HeadSculpt s) {
        s.part("CHEST");
        for (int k = 0; k < 10; k++) {
            double a = Math.PI * 2 * k / 10;
            pelt(s.box(v(Math.sin(a) * .44, 2.36 + .03 * (k % 3), Math.cos(a) * .33), v(.27, .17, .17),
                    new Quaternionf().rotateY((float) a).rotateZ(k % 2 == 0 ? .2f : -.2f), 1, 1, 1, PELT), k % 2);
        }
    }

    /** Pauldrons like strongboxes: a spiral cut into the front, the back, the side and the top of each, fur hanging under it. */
    private static void pauldrons(HeadSculpt s) {
        s.part("CHEST");
        for (int side : SIDES) {
            Quaternionf droop = new Quaternionf().rotateZ(side * -.14f);
            Vector3f at = v(side * .90, 2.64, 0), size = v(.52, .46, .64);
            Box plate = gild(put(s.box(v(-.90, 2.64, 0), size, new Quaternionf().rotateZ(.14f), 2, 2, 2, GOLD), side));
            for (Art a : List.of(plate.front, plate.back, plate.right, plate.top)) groove(spiral(border(a, 0, LINE), 2, 2, 12, 12, false));
            fringe(s, corner(at, size, droop, -1, -1, 1), corner(at, size, droop, 1, -1, 1), 6, .22f, v(0, -1, .2), .35f, 50 + side);
            fringe(s, corner(at, size, droop, side, -1, 1), corner(at, size, droop, side, -1, -1), 6, .22f, v(side * .35, -1, 0), .3f, 70 + side);
            fringe(s, corner(at, size, droop, 1, -1, -1), corner(at, size, droop, -1, -1, -1), 4, .20f, v(0, -1, -.2), .35f, 90 + side);
        }
    }

    /**
     * The helm: a tall box with a lion's face cut into its front ({@link #FACE}), the crest
     * carried back over its crown, a spiral on either side over the steel of the mouth guard, and a
     * tusk at either cheek that runs out from the jaw and turns up to a point.
     */
    private static void head(HeadSculpt s) {
        s.part("HEAD");
        Box helm = gild(s.box(v(0, 2.88, 0), v(.56, .92, .58), LEVEL, 2, 3, 2, GOLD));
        groove(helm.front.draw(0, 0, INK, FACE).mirror());
        groove(spiral(spiral(border(helm.right, 0, LINE), 2, 2, 12, 11, false), 4, 18, 8, 5, false).rect(0, 16, 16, 1, LINE));
        helm.right.rect(0, 14, 16, 1, STEEL).rect(0, 15, 16, 1, STEEL_DARK);
        helm.left.copy(helm.right, true);
        groove(border(helm.back, 0, LINE));
        groove(border(helm.top, 0, LINE).rect(5, 0, 1, 16, LINE).rect(10, 0, 1, 16, LINE)).rect(6, 0, 4, 16, PALE);
        for (int side : SIDES) {
            bone(put(s.bar(v(-.24, 2.60, .20), v(-.50, 2.54, .22), .19f, .19f, Y, 1, BONE), side));
            bone(put(s.bar(v(-.52, 2.47, .22), v(-.56, 2.80, .23), .17f, .17f, X, 1, BONE), side));
            bone(put(s.bar(v(-.56, 2.76, .23), v(-.58, 2.93, .235), .10f, .10f, X, 1, BONE), side));
        }
    }

    /**
     * The mane: its body over the back of the crown and down the back of the helm, a crest of tall
     * locks standing from it, locks swept out and up from either side, and more thrown back behind
     * the head.
     */
    private static void mane(HeadSculpt s) {
        s.part("HEAD");
        hair(s.box(v(0, 3.36, -.15), v(.62, .12, .38), LEVEL, 1, 1, 1, MANE_DEEP), 0);
        hair(s.box(v(0, 2.95, -.33), v(.62, .70, .12), LEVEL, 1, 1, 1, MANE_DEEP), 0);
        // The body of it, so no daylight shows between the locks where they grow.
        hair(s.box(v(0, 3.64, -.24), v(.50, .52, .40), new Quaternionf().rotateX(-.22f), 1, 1, 1, MANE_DEEP), 0);
        for (int side : SIDES) {
            hair(s.box(v(side * .36, 3.20, -.24), v(.24, .40, .36), new Quaternionf().rotateZ(side * -.5f), 1, 1, 1, MANE_DEEP), 0);
            hair(s.box(v(side * .27, 3.66, -.26), v(.30, .50, .34), new Quaternionf().rotateZ(side * -.3f), 1, 1, 1, MANE_DEEP), 0);
        }
        // The tall locks stand shoulder to shoulder where they grow and fan apart as they rise.
        int n = 0;
        for (int row = 0; row < 4; row++) {
            for (int col = -1; col <= 1; col++) {
                float x = col * .22f, z = .04f - .16f * row;
                float height = 1.05f - .16f * Math.abs(col) - (row == 0 ? .30f : row == 3 ? .12f : 0) + .1f * (noise(n + 40) - .5f);
                lock(s, v(x, 3.36, z), v(x * 1.5 + col * .12 + .08 * (noise(n) - .5), 3.36 + height, z - .10 - .16 * row), .22f, .16f, 3, n++);
            }
        }
        for (int side : SIDES) {
            for (int k = 0; k < 4; k++) {
                Vector3f root = v(side * .30, 3.28 - .13 * k, -.08 - .07 * k);
                Vector3f way = v(side * (.75 + .08 * k), .62 - .13 * k, -.25 - .07 * k).normalize();
                lock(s, root, at(root, way, .62f - .05f * k + .08f * noise(n)), .19f, .17f, 2, n++);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = -1; col <= 1; col++) {
                Vector3f root = v(col * .19, 3.22 - .22 * row, -.36);
                Vector3f way = v(col * .45, .55 - .40 * row, -.80).normalize();
                lock(s, root, at(root, way, .80f - .08f * row + .12f * noise(n)), .19f, .19f, 2, n++);
            }
        }
    }

    /**
     * A lock of the mane from {@code root} to {@code tip}: thick and dark where it grows, brightest
     * along its length, drawn to a thin point. The tall locks take three pieces, the rest two.
     */
    private static void lock(HeadSculpt s, Vector3f root, Vector3f tip, float width, float depth, int pieces, int seed) {
        float[] cuts = pieces == 3 ? new float[]{0, .44f, .36f, .78f, .72f, 1} : new float[]{0, .58f, .50f, 1};
        float[] wide = pieces == 3 ? new float[]{1, .7f, .4f} : new float[]{1, .5f};
        int[] tones = pieces == 3 ? new int[]{0, 2, 1} : new int[]{0, 1};
        for (int k = 0; k < pieces; k++) {
            Vector3f from = new Vector3f(root).lerp(tip, cuts[2 * k]), to = new Vector3f(root).lerp(tip, cuts[2 * k + 1]);
            hair(s.bar(from, to, width * wide[k], depth * wide[k], X, 1, MANE), seed % 2 + tones[k]);
        }
    }

    /** Hair streaked in one of the tones of {@link #HAIR}. */
    private static void hair(Box lock, int tone) {
        for (Art a : lock.sides()) streaks(a, HAIR[tone]);
        lock.top.fill(HAIR[tone][3]);
        lock.bottom.fill(HAIR[tone][0]);
    }

    /**
     * Arms held out from the body: wrapped dark to the elbow, then a gold vambrace as big as a
     * strongbox, a spiral on every side of it over a pale band, a ruff of fur at its elbow end.
     */
    private static void arms(HeadSculpt s) {
        for (int side : SIDES) {
            s.part(side < 0 ? "ARM_R" : "ARM_L");
            Box arm = put(s.bar(ELBOW_R, SHOULDER_R, .27f, .27f, v(0, 0, 1), 2, WRAP), side);
            for (Art a : arm.sides()) for (int y = 0; y < 16; y++) a.rect(0, y, 8, 1, y % 5 == 3 ? WRAP_DARK : y % 5 == 4 ? WRAP_LIGHT : WRAP);

            s.part(side < 0 ? "FOREARM_R" : "FOREARM_L");
            Vector3f along = new Vector3f(WRIST_R).sub(ELBOW_R).normalize();
            Vector3f lo = at(ELBOW_R, along, .04f), hi = at(WRIST_R, along, .08f);
            // Turned so its front face looks ahead; its pictures stand with the hand at their top.
            Box brace = gild(put(s.box(mid(lo, hi), v(.38f, lo.distance(hi), .38f), turn(lo, hi, Y), 2, 3, 2, GOLD), side));
            for (Art a : brace.sides()) {
                groove(spiral(border(a, 0, LINE), 2, 2, 12, 15, false));
                a.rect(0, 19, 16, 1, LINE).rect(0, 20, 16, 1, PALE).rect(0, 21, 16, 2, LIGHT);
            }

            Vector3f elbow = side < 0 ? ELBOW_R : ELBOW_L, wrist = side < 0 ? WRIST_R : WRIST_L;
            Vector3f run = new Vector3f(wrist).sub(elbow).normalize(), foot = at(elbow, run, .04f), back = new Vector3f(run).negate();
            Quaternionf q = turn(elbow, wrist, v(0, -side, 0));
            Vector3f up = q.transform(v(-side * .19f, 0, 0)), front = q.transform(v(0, 0, .19f));
            fringe(s, new Vector3f(foot).add(up).sub(front), new Vector3f(foot).add(up).add(front), 4, .18f, new Vector3f(back).add(0, .5f, 0), .4f, 110 + side);
            fringe(s, new Vector3f(foot).add(front).add(up), new Vector3f(foot).add(front).sub(up), 4, .18f, new Vector3f(back).add(0, -.3f, .4f), .4f, 130 + side);
            fringe(s, new Vector3f(foot).sub(up).add(front), new Vector3f(foot).sub(up).sub(front), 4, .20f, new Vector3f(back).mul(.5f).add(0, -1, 0), .4f, 150 + side);
        }
    }

    // =====================================================================================
    // The swords
    // =====================================================================================

    /** Where a hand grips its sword. */
    private static Vector3f grip(int side) {
        return v(side * 1.58, 2.01, .445);
    }

    /** The way a sword points from its grip: up, leaning out and a little forward. */
    private static Vector3f axis(int side) {
        return v(side * .37, .92, .10).normalize();
    }

    /** Across a sword, away from the body: the side its belly is on. */
    private static Vector3f outward(int side) {
        Vector3f a = axis(side), x = v(side, 0, 0);
        return x.sub(a.mul(x.dot(a))).normalize();
    }

    /** The way a blade runs past its bend: turned back towards the body, so it stands upright. */
    private static Vector3f upper(int side) {
        return axis(side).mul((float) Math.cos(TURN)).sub(outward(side).mul((float) Math.sin(TURN)));
    }

    /** Across a blade past its bend, away from the body. */
    private static Vector3f across(int side) {
        return outward(side).mul((float) Math.cos(TURN)).add(axis(side).mul((float) Math.sin(TURN)));
    }

    /** The middle of a blade where it bends: its inner edge runs on unbroken, and the upper blade is wider. */
    private static Vector3f bend(int side) {
        return at(at(at(grip(side), axis(side), BEND), outward(side), -.17f), across(side), .22f);
    }

    /**
     * A greatsword in each fist: a banded pommel and a wrapped grip, a guard of three keyed blocks,
     * and a rimmed black blade that runs out from the hand, bends upright, and is clipped to a
     * point on its inner edge. Its gold inlay goes through the blade, so it shows on both faces.
     *
     * <p>It is built for his right hand, x in towards his body, y up the blade and z ahead, and
     * flipped for his left.</p>
     */
    private static void swords(HeadSculpt s) {
        Vector3f h = grip(-1), a = axis(-1), b = outward(-1), in = new Vector3f(b).negate();
        Quaternionf q = turn(h, at(h, a, 1f), in);
        for (int side : SIDES) {
            s.part(side < 0 ? "HAND_R" : "HAND_L");
            Box fist = gild(put(s.box(h, v(.30, .30, .34), q, 1, 1, 1, GOLD), side));
            for (Art f : fist.sides()) groove(f.rect(0, 2, 8, 1, LINE).rect(0, 5, 8, 1, LINE));
            // The fist stays on the hand; the sword is a part of its own, which can be put away.
            s.part(side < 0 ? "SWORD_R" : "SWORD_L");
            Box hilt = put(s.bar(at(h, a, -.46f), at(h, a, .16f), .11f, .11f, in, 1, WRAP), side);
            for (Art f : hilt.sides()) for (int y = 0; y < 8; y++) f.rect(0, y, 8, 1, y % 3 == 0 ? WRAP_DARK : y % 3 == 1 ? WRAP : WRAP_LIGHT);
            Box pommel = gild(put(s.bar(at(h, a, -.64f), at(h, a, -.45f), .18f, .18f, in, 1, GOLD), side));
            for (Art f : pommel.sides()) groove(f.rect(0, 2, 8, 1, LINE).rect(0, 5, 8, 1, LINE));

            // The guard's blocks differ a hair in thickness, so no two of their faces lie in one plane.
            Box cross = gild(put(s.box(at(h, a, .25f), v(.36, .16, .20), q, 1, 1, 1, GOLD), side));
            Box inner = gild(put(s.box(at(at(h, a, .37f), b, -.25f), v(.17, .36, .21), q, 1, 2, 1, GOLD), side));
            Box outer = gild(put(s.box(at(at(h, a, .31f), b, .26f), v(.18, .26, .19), q, 1, 1, 1, GOLD), side));
            for (Art f : List.of(cross.front, cross.back)) groove(border(f, 0, LINE).rect(2, 3, 4, 2, LINE));
            for (Art f : List.of(inner.front, inner.back)) groove(spiral(f, 0, 0, 8, 16, false));
            for (Art f : List.of(outer.front, outer.back)) groove(spiral(f, 0, 0, 8, 8, false));

            // From the guard to the bend, the inner edge straight.
            Vector3f lo = at(at(h, a, .34f), b, .01f), hi = at(at(h, a, 1.50f), b, .01f);
            Box lower = put(s.box(mid(lo, hi), v(.36f, lo.distance(hi), .06f), q, 2, 4, 1, BLADE_RIM), side);
            blade(lower.front).draw(3, 6, INK, INLAY_ART);
            lower.back.copy(lower.front, true);

            // Past the bend it stands upright, a hair thicker so the two share no face, then steps in from its outer edge to a point.
            Vector3f c = bend(-1), d = upper(-1), e = across(-1);
            Quaternionf qu = turn(c, at(c, d, 1f), new Vector3f(e).negate());
            Box high = put(s.box(at(c, d, .23f), v(.44, .66, .068), qu, 2, 2, 1, BLADE_RIM), side);
            blade(high.front).rect(5, 3, 2, 1, BLADE_RIM).rect(4, 6, 1, 1, BLADE_RIM);
            high.back.copy(high.front, true);
            for (int k = 0; k < 3; k++) {
                float width = .33f - .11f * k;
                Box step = put(s.box(at(at(c, d, .6275f + .135f * k), e, -(.44f - width) / 2), v(width, .135, .068), qu, 1, 1, 1, BLADE_RIM), side);
                for (Art f : List.of(step.front, step.back)) f.rect(1, 1, 6, 7, BLADE);
            }
        }
    }

    // =====================================================================================
    // On his back
    // =====================================================================================

    /** The cape: a yoke across his shoulders, torn panels each to a hem of its own with a rag at its end, and a wing of it to either side. */
    private static void cape(HeadSculpt s) {
        s.part("CAPE");
        for (Art a : s.box(v(0, 2.32, -.44), v(1.34, .10, .24), LEVEL, 2, 1, 1, CLOTH).faces()) drape(a, 0, a.h);
        float[] hem = {1.22f, 1.05f, 1.12f, 1.02f, 1.15f, 1.10f};
        for (int k = 0; k < hem.length; k++) {
            // Each panel a hair behind the last, so no two share a face where they overlap.
            float x = -.62f + .248f * k, z = -.004f * k;
            Vector3f foot = v(x * 1.55, hem[k], -.74 + z);
            for (Art a : s.bar(foot, v(x, 2.34, -.52 + z), .33f, .04f, X, 2, CLOTH).faces()) drape(a, k % 2, a.h);
            for (Art a : s.box(at(foot, Y, -.02f), v(.17, .17, .03), new Quaternionf().rotateZ(.785f), 1, 1, 1, CLOTH).faces()) drape(a, k % 2, a.h);
        }
        for (Art a : s.bar(v(1.42, 1.44, -.66), v(.86, 2.28, -.53), .52f, .04f, X, 2, CLOTH).faces()) drape(a, 1, a.h);
        for (Art a : s.bar(v(1.70, 1.24, -.68), v(1.20, 1.80, -.62), .30f, .04f, X, 1, CLOTH).faces()) drape(a, 0, a.h);
        for (Art a : s.bar(v(-1.02, 1.50, -.64), v(-.82, 2.26, -.53), .36f, .04f, X, 2, CLOTH).faces()) drape(a, 0, a.h);
    }

    /**
     * The greatbow between his back and his cape: its upper limb stands behind his right shoulder,
     * its lower limb comes out level at his left hip, each banded in black and gold where the gold
     * of its tip begins, and tipped with a keyed hook turned away from the string.
     */
    private static void bow(HeadSculpt s) {
        s.part("BOW");
        float z = BOW_Z;
        Vector3f knee = v(-.84, 2.50, z), handle = BOW_HANDLE;
        for (Box limb : List.of(s.bar(knee, v(-.92, 3.14, z), .14f, .12f, X, 1, SLATE), s.bar(handle, knee, .15f, .13f, X, 1, SLATE),
                s.bar(handle, v(.72, 1.40, z), .14f, .12f, X, 1, SLATE))) {
            for (Art a : limb.sides()) streaks(a, SLATE_DARK, SLATE, SLATE, SLATE_LIGHT, SLATE, SLATE, SLATE, SLATE_DARK);
        }
        Box hold = s.bar(at(handle, v(.92, -.39, 0), .16f), at(handle, v(-.57, .82, 0), .16f), .17f, .15f, X, 1, LEATHER);
        for (Art a : hold.sides()) for (int y = 0; y < 8; y++) a.rect(0, y, 8, 1, y % 2 == 0 ? LEATHER : LEATHER_DARK);
        for (Box bands : List.of(s.bar(v(-.92, 3.14, z), v(-.92, 3.36, z), .17f, .15f, X, 1, GOLD), s.bar(v(.72, 1.40, z), v(.94, 1.376, z), .17f, .15f, Y, 1, GOLD))) {
            for (Art a : bands.sides()) for (int y = 0; y < 8; y++) a.rect(0, y, 8, 1, y % 4 == 0 ? LIGHT : y % 4 == 1 ? GOLD : SLATE_DARK);
        }
        for (Box tip : List.of(s.bar(v(-.92, 3.36, z), BOW_TOP, .19f, .15f, X, 2, GOLD), s.bar(v(.94, 1.375, z), BOW_LOW, .19f, .15f, Y, 2, GOLD))) {
            for (Art a : gild(tip).sides()) groove(border(border(a, 0, LINE), 2, LINE));
        }
        // The hooks are a hair thicker than the tips they cross, so the two share no face.
        for (Box hook : List.of(s.bar(v(-.84, 3.86, z), v(-1.22, 3.86, z), .15f, .16f, Y, 1, GOLD), s.bar(v(-1.17, 3.70, z), v(-1.17, 3.92, z), .10f, .14f, X, 1, GOLD),
                s.bar(v(1.36, 1.06, z), v(1.36, 1.40, z), .15f, .16f, X, 1, GOLD))) {
            for (Art a : gild(hook).sides()) groove(spiral(a, 0, 0, 8, 8, false));
        }
        Box string = s.bar(v(1.34, 1.14, -.36), v(-1.00, 3.84, -.36), .03f, .03f, X, 1, FUR);
        for (Art a : string.sides()) for (int y = 0; y < 8; y++) a.rect(0, y, 8, 1, y % 2 == 0 ? FUR : FUR_MID);
    }

    /**
     * Two giant arrows stood in his back over his left shoulder, a dark nock at the end of each and
     * three white feathers hung along it. Each is a part of its own, which he takes to shoot.
     */
    private static void arrows(HeadSculpt s) {
        for (int n = 0; n < SHAFTS.length; n++) {
            s.part(n == 0 ? "ARROW" : "ARROW_2");
            Vector3f[] shaft = SHAFTS[n];
            Vector3f d = new Vector3f(shaft[1]).sub(shaft[0]).normalize();
            // Straight down, as near as the shaft allows; the feathers are raked back towards the arrow's head.
            Vector3f hang = v(0, -1, 0).add(new Vector3f(d).mul(d.y)).normalize();
            Vector3f below = new Vector3f(hang).mul(.85f).sub(new Vector3f(d).mul(.5f)).normalize();
            Vector3f above = new Vector3f(hang).mul(-.85f).sub(new Vector3f(d).mul(.5f)).normalize();
            for (Art a : s.bar(shaft[0], at(shaft[1], d, .07f), .09f, .09f, X, 1, WOOD).sides()) {
                streaks(a, WOOD, WOOD_DARK, WOOD, WOOD, WOOD_DARK, WOOD, WOOD, WOOD_DARK).rect(0, 0, 8, 1, LEATHER_DARK);
            }
            for (int k = 0; k < 3; k++) {
                Vector3f c = new Vector3f(shaft[0]).lerp(shaft[1], .46f + .17f * k);
                tuft(s.bar(at(c, below, .30f), c, .12f, .03f, d, 1, FUR), k);
                tuft(s.bar(at(c, above, .13f), c, .10f, .03f, d, 1, FUR), k);
            }
        }
    }

    // =====================================================================================
    // Paint
    // =====================================================================================

    /** A box built for his right side: left there, or flipped to his left wearing the same heads. */
    private static Box put(Box box, int side) {
        return side < 0 ? box : box.flip();
    }

    /** Gold plate on every face of a box: a lit rim along its top and left, a shaded one along its bottom and right. */
    private static Box gild(Box box) {
        for (Art a : box.faces()) {
            a.fill(GOLD).rect(0, 0, a.w, 1, LIGHT).rect(0, 0, 1, a.h, LIGHT).rect(0, a.h - 1, a.w, 1, SHADE).rect(a.w - 1, 0, 1, a.h, SHADE);
        }
        return box;
    }

    /** Lights the lower lip of every line cut into a plate, the way a groove catches the sun. */
    private static Art groove(Art a) {
        for (int y = a.h - 2; y >= 0; y--) {
            for (int x = 0; x < a.w; x++) {
                int under = a.get(x, y + 1);
                if (a.get(x, y) == LINE && (under == GOLD || under == LIGHT || under == SHADE)) a.set(x, y + 1, PALE);
            }
        }
        return a;
    }

    /** A one-pixel frame, {@code inset} pixels in from the picture's edge. */
    private static Art border(Art a, int inset, int rgb) {
        int w = a.w - 2 * inset, h = a.h - 2 * inset;
        return a.rect(inset, inset, w, 1, rgb).rect(inset, a.h - 1 - inset, w, 1, rgb).rect(inset, inset, 1, h, rgb).rect(a.w - 1 - inset, inset, 1, h, rgb);
    }

    /**
     * A squared spiral, his plate's device: a line wound inwards from the top left of the {@code w}
     * by {@code h} pixels at {@code x}, {@code y}, a pixel of plate between its turns. {@code flipped}
     * winds it from the top right instead.
     */
    private static Art spiral(Art a, int x, int y, int w, int h, boolean flipped) {
        int left = 0, top = 0, right = w - 1, bottom = h - 1, cx = 0, cy = 0;
        for (int leg = 0; left <= right && top <= bottom; leg++) {
            int fromX = cx, fromY = cy;
            switch (leg % 4) {
                case 0 -> { cx = right; top += 2; }
                case 1 -> { cy = bottom; right -= 2; }
                case 2 -> { cx = left; bottom -= 2; }
                default -> { cy = top; left += 2; }
            }
            for (int i = Math.min(fromX, cx); i <= Math.max(fromX, cx); i++) {
                for (int j = Math.min(fromY, cy); j <= Math.max(fromY, cy); j++) a.set(x + (flipped ? w - 1 - i : i), y + j, LINE);
            }
        }
        return a;
    }

    /** The flutes of a rim: pale and dark lines down the picture. */
    private static Art flute(Art a) {
        for (int x = 0; x < a.w; x++) a.rect(x, 0, 1, a.h, x % 2 == 0 ? LIGHT : LINE);
        return a;
    }

    /** A speckled coat: {@code base} flecked with a darker and a lighter tone. */
    private static Art coat(Art a, int base, int dark, int light, int seed) {
        for (int y = 0; y < a.h; y++) {
            for (int x = 0; x < a.w; x++) {
                float fleck = noise(seed * 613 + y * 37 + x);
                a.set(x, y, fleck < .14f ? dark : fleck > .90f ? light : base);
            }
        }
        return a;
    }

    /** Streaks down the picture: its columns take {@code tones} in turn. */
    private static Art streaks(Art a, int... tones) {
        for (int x = 0; x < a.w; x++) a.rect(x, 0, 1, a.h, tones[x % tones.length]);
        return a;
    }

    /** Cloth hanging in folds over the top {@code rows} of a picture, a hole worn through it here and there. */
    private static Art drape(Art a, int variant, int rows) {
        int[] folds = variant == 0 ? new int[]{CLOTH_DARK, CLOTH, CLOTH, CLOTH_LIGHT, CLOTH, CLOTH_DARK, CLOTH, CLOTH}
                : new int[]{CLOTH, CLOTH_LIGHT, CLOTH, CLOTH_DARK, CLOTH, CLOTH, CLOTH_DARK, CLOTH};
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < a.w; x++) a.set(x, y, noise(variant * 53 + y * 29 + x) > .95f ? CLOTH_DEEP : folds[x % 8]);
        }
        return a;
    }

    /** A tuft of the pelt: dark fur, {@code tone} 0 flecked with paler hairs. */
    private static void pelt(Box tuft, int tone) {
        for (Art a : tuft.faces()) coat(a, PELT, tone == 0 ? PELT_MID : PELT, tone == 0 ? PELT_LIGHT : PELT_MID, 33 + tone);
    }

    /** A length of tusk: pale down its middle, shaded at its edges. */
    private static void bone(Box horn) {
        for (Art a : horn.sides()) streaks(a, BONE_SHADE, BONE_MID, BONE, BONE, BONE_MID, BONE, BONE_MID, BONE_SHADE);
        horn.top.fill(BONE);
        horn.bottom.fill(BONE_MID);
    }

    /** The face of a blade: a black field, pitted, in a rim with a bright line inside it at either edge. */
    private static Art blade(Art a) {
        coat(a, BLADE, BLADE_DEEP, BLADE, 71);
        return a.rect(0, 0, 1, a.h, BLADE_RIM).rect(1, 0, 1, a.h, BLADE_EDGE).rect(a.w - 3, 0, 1, a.h, BLADE_EDGE).rect(a.w - 2, 0, 2, a.h, BLADE_RIM);
    }

    /**
     * A fringe of fur along the edge from {@code a} to {@code b}: {@code count} tufts of uneven
     * length that hang along {@code down} and splay towards the edge's ends.
     */
    private static void fringe(HeadSculpt s, Vector3f a, Vector3f b, int count, float length, Vector3f down, float splay, int seed) {
        Vector3f edge = new Vector3f(b).sub(a).normalize();
        float width = a.distance(b) / count * .92f;
        for (int k = 0; k < count; k++) {
            float t = (k + .5f) / count;
            Vector3f root = new Vector3f(a).lerp(b, t);
            Vector3f way = new Vector3f(down).add(new Vector3f(edge).mul((t - .5f) * 2 * splay + .25f * (noise(seed + k) - .5f))).normalize();
            tuft(s.bar(at(root, way, length * (.65f + .6f * noise(seed + 31 + k))), root, width, .045f, edge, 1, FUR), k);
        }
    }

    /** A tuft of fur, or a feather, hanging from the top of its picture: pale strands, a darker one or two, a shaded tip. */
    private static void tuft(Box tuft, int tone) {
        int t = tone % 3;
        for (Art a : tuft.faces()) {
            a.fill(t == 2 ? FUR_MID : FUR).rect((2 + 3 * t) % 8, 0, 1, 8, t == 2 ? FUR_SHADE : FUR_MID).rect((5 + 3 * t) % 8, 0, 1, 8, FUR_MID).rect(0, 7, 8, 1, FUR_SHADE);
        }
    }

    // =====================================================================================
    // Tools
    // =====================================================================================

    /** {@code from} moved {@code by} blocks along {@code direction}. */
    private static Vector3f at(Vector3f from, Vector3f direction, float by) {
        return new Vector3f(from).add(new Vector3f(direction).mul(by));
    }

    private static Vector3f mid(Vector3f a, Vector3f b) {
        return new Vector3f(a).add(b).mul(.5f);
    }

    /** The turn of a bar laid from {@code a} to {@code b} with its width along {@code side}: the one {@link DisplaySculpt#bar} gives it. */
    private static Quaternionf turn(Vector3f a, Vector3f b, Vector3f side) {
        Vector3f along = new Vector3f(b).sub(a).normalize();
        Vector3f wide = new Vector3f(side).sub(new Vector3f(along).mul(side.dot(along))).normalize();
        return new Matrix3f(wide, along, new Vector3f(wide).cross(along)).getNormalizedRotation(new Quaternionf());
    }

    /** A corner of a turned box, or with a zero the middle of an edge or a face: {@code sx}, {@code sy}, {@code sz} of -1, 0 or 1 pick it. */
    private static Vector3f corner(Vector3f centre, Vector3f size, Quaternionf rotation, float sx, float sy, float sz) {
        return rotation.transform(new Vector3f(sx * size.x / 2, sy * size.y / 2, sz * size.z / 2)).add(centre);
    }
}
