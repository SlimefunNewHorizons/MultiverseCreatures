package com.Chagui68.entities.monster;

import com.Chagui68.entities.monster.MonsterRig.Place;
import com.Chagui68.entities.monster.MonsterRig.Pose;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * One visit of the Monster, tick by tick, planned against the blocks round it so that its body is
 * never inside one: the game drives it with the world and the player, the model viewer and the tests
 * with a few blocks and a player who stands still.
 *
 * <p>It waits in its pose until the player has looked at it for a moment. Then it plays one of six
 * behaviours, each checked against the space before it starts:</p>
 * <ul>
 *   <li><b>walk</b>: it gets up the way its place asks (drops from a ceiling, hops down off a block,
 *   straightens up) and walks up to the player along a path, standing where there is room, narrow in
 *   a corridor, crouched under a low ceiling, on all fours in a tunnel;</li>
 *   <li><b>coffin rise</b>: from a double chest it sits up slowly, swings its legs over the edge and
 *   steps down;</li>
 *   <li><b>crawl rush</b>: it drops to all fours and scuttles at the player, faster;</li>
 *   <li><b>ceiling scuttle</b>: it runs along the ceiling, upside down, to above the player and drops
 *   in front of them;</li>
 *   <li><b>blink</b>: it flickers out and is suddenly in the player's face;</li>
 *   <li><b>peek and hide</b>: it pulls back out of sight and waits somewhere else, closer.</li>
 * </ul>
 * <p>Every one of them ends with its palm over the player's eyes, in the tallest of three hands that
 * fits the room (standing, crouched, on all fours), and then it is gone. When a behaviour cannot be
 * played where it is, it blinks instead.</p>
 */
public final class MonsterScript {

    /** What it does once seen. */
    public enum Behavior {
        WALK, COFFIN_RISE, CRAWL_RUSH, CEILING_SCUTTLE, BLINK, PEEK_AND_HIDE;

        /** The name the viewer and the commands show: lower case, dashes. */
        public String id() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    /** What it is doing in a frame. */
    public enum Phase { LURK, RISE, MOVE, GRAB, BLINK, HIDE, GONE }

    /** What the game has to do with a frame besides drawing it: sounds and effects. */
    public enum Event {
        NONE,
        /** It has been seen. */
        NOTICED,
        /** Its hand is over the player's eyes. */
        COVERED,
        /** It vanished in front of the player. */
        VANISHED,
        /** It left without being seen. */
        LEFT,
        /** It pulled back out of sight. */
        HID,
        /** It is back somewhere else, waiting again, or in the player's face after a blink. */
        BACK
    }

    /** Whether the player can see a point, within an angle of where they look. */
    public interface Senses {
        boolean sees(Vector3d point, double angle);
    }

    /** The player as the script needs them: where they stand, look from and look at, and how fast they go. */
    public record Watcher(Vector3d feet, Vector3d eye, Vector3d look, boolean flying, double speed) {
    }

    /** The visit's numbers, from {@code entities.monster}. */
    public record Settings(double speed, int handTicks, int lurkTicks, double viewAngle) {
    }

    /**
     * One tick: what it does, how it is posed, where it stands and faces, whether it is shown, what
     * happened, and the blocks it rests on (which its body may touch).
     */
    public record Frame(Phase phase, Pose pose, Vector3d anchor, float yaw, boolean visible, Event event,
                        Set<Long> resting) {
    }

    /** Ticks in a row the player has to look at it before it notices. */
    public static final int SEEN_TICKS = 3;
    /** How close it gets before it reaches for the player's eyes. */
    static final double REACH = 2.4;
    /** How far in front of the player's eyes it holds its palm. */
    public static final double PALM_REACH = MonsterMotion.PALM_REACH;
    private static final double HIDDEN_ANGLE = 80;
    private static final int MAX_EXPANSIONS = 2500;
    private static final int PATH_RADIUS = 16;
    private static final int MAX_REPLANS = 6;

    /** How it walks, tallest first: standing, narrow, crouched, on all fours. */
    private static final List<String> WALKERS = List.of("idle", "corridor stalk", "corridor crouch", "corridor crawl");
    private static final List<String> CRAWLERS = List.of("corridor crawl", "crawling");
    private static final List<String> CEILING_WALKERS = List.of("ceiling cling", "corridor ceiling crawl");
    private static final List<String> HANDS = List.of("hand on camera", "hand on camera low", "hand on camera crawl");
    /** Straight in front, then from the side: where it can stand to reach across to the player's eyes. */
    private static final float[] SIDES = {0f, (float) Math.PI / 4, (float) -Math.PI / 4, (float) Math.PI / 2, (float) -Math.PI / 2};
    /** Ground poses it may wait in again after it has hidden, besides the one it had. */
    private static final List<String> HIDING = List.of("crouched", "right behind", "over the shoulder", "head tilt",
            "corridor crouch", "squeezed in a shaft", "folded in a hole");
    private static final float[] STRIDE_PHASES = {0f, (float) Math.PI / 2, (float) Math.PI, (float) Math.PI * 1.5f};

    private final MonsterRig rig;
    private final MonsterSpace space;
    private final Settings settings;
    private final Random random;
    private final Behavior forced;
    private final Map<String, float[]> still = new HashMap<>();
    private final Map<String, float[]> walking = new HashMap<>();

    private Phase phase = Phase.LURK;
    private Pose shown;
    /** The pose under the stride while it walks, so a stride never piles onto another. */
    private Pose base;
    private Vector3d anchor;
    private float yaw;
    private boolean visible = true;
    private Set<Long> resting;
    private int ticks;
    private int seen;
    private boolean peeked;
    private Behavior behavior;
    /** The behaviour chosen the first time it was seen: hiding comes back to wait, and is seen again. */
    private Behavior first;
    private boolean fellBack;
    private int replans;
    private float stride;
    private Event event = Event.NONE;
    private final Deque<Action> actions = new ArrayDeque<>();

    /**
     * @param forced the behaviour to play once seen, or {@code null} to choose as the game does
     */
    public MonsterScript(MonsterRig rig, MonsterSpace space, Pose start, Vector3d anchor, float yaw,
                         Settings settings, Random random, Behavior forced) {
        this.rig = rig;
        this.space = space;
        this.settings = settings;
        this.random = random;
        this.forced = forced;
        this.shown = start;
        this.base = start;
        this.anchor = new Vector3d(anchor);
        this.yaw = yaw;
        this.resting = support(start, anchor, yaw);
    }

    public Phase phase() {
        return phase;
    }

    public Pose pose() {
        return shown;
    }

    public Vector3d anchor() {
        return new Vector3d(anchor);
    }

    public float yaw() {
        return yaw;
    }

    /** The behaviour it is playing, or {@code null} while it still waits. */
    public Behavior behavior() {
        return behavior;
    }

    /** The blocks a pose on blocks lies on: the one under its anchor, or the two of a double chest. */
    public static Set<Long> support(Pose pose, Vector3d anchor, float yaw) {
        Set<Long> cells = new HashSet<>();
        if (pose.place() != Place.BLOCK) return cells;
        int y = (int) Math.floor(anchor.y - 0.01);
        if (pose.wide()) {
            for (double side : new double[]{-0.5, 0.5}) {
                double x = anchor.x + side * Math.cos(yaw), z = anchor.z - side * Math.sin(yaw);
                cells.add(MonsterSpace.key((int) Math.floor(x), y, (int) Math.floor(z)));
            }
        } else {
            cells.add(MonsterSpace.key((int) Math.floor(anchor.x), y, (int) Math.floor(anchor.z)));
        }
        return cells;
    }

    // ------------------------------------------------------------------ the tick

    /**
     * One tick of the visit.
     *
     * @param seenNow whether the player is looking at it now (the game checks its line of sight)
     */
    public Frame tick(Watcher watcher, boolean seenNow, Senses senses) {
        event = Event.NONE;
        if (phase == Phase.GONE) return frame();
        ticks++;
        if (phase == Phase.LURK && actions.isEmpty()) {
            seen = seenNow ? seen + 1 : 0;
            if (seen >= SEEN_TICKS) {
                event = Event.NOTICED;
                notice(watcher, senses);
            } else if (ticks > settings.lurkTicks() && !senses.sees(chest(), HIDDEN_ANGLE)) {
                phase = Phase.GONE;
                visible = false;
                event = Event.LEFT;
                return frame();
            }
        }
        for (int guard = 0; guard < 32 && !actions.isEmpty(); guard++) {
            Action action = actions.peekFirst();
            if (action instanceof Decision decision) {
                actions.pollFirst();
                decision.run.accept(watcher, senses);
                continue;
            }
            if (action.step(watcher)) actions.pollFirst();
            break;
        }
        return frame();
    }

    private Frame frame() {
        return new Frame(phase, shown, new Vector3d(anchor), yaw, visible, event, Set.copyOf(resting));
    }

    // ------------------------------------------------------------------ choosing what to do

    private void notice(Watcher watcher, Senses senses) {
        Behavior wanted = forced != null && !(peeked && forced == Behavior.PEEK_AND_HIDE) ? forced : natural();
        if (build(wanted, watcher, senses)) {
            behavior = wanted;
        } else if (forced == null && wanted != Behavior.WALK && build(Behavior.WALK, watcher, senses)) {
            behavior = Behavior.WALK;
        } else {
            build(Behavior.BLINK, watcher, senses);
            behavior = Behavior.BLINK;
        }
        if (first == null) first = behavior;
    }

    /**
     * The behaviour it played when first seen, or a blink when that one could not be finished (no
     * path, no room to reach). Hiding counts as played however it comes back.
     */
    public Behavior played() {
        return fellBack && first != Behavior.PEEK_AND_HIDE ? Behavior.BLINK : first;
    }

    /** What the game picks: the coffin rise from a double chest, often the scuttle from a ceiling. */
    private Behavior natural() {
        if (shown.wide()) return Behavior.COFFIN_RISE;
        if (shown.place() == Place.CEILING && random.nextDouble() < 0.6) return Behavior.CEILING_SCUTTLE;
        if (!peeked && shown.place() != Place.AIR && random.nextDouble() < 0.3) return Behavior.PEEK_AND_HIDE;
        if (shown.place() == Place.GROUND && random.nextDouble() < 0.25) return Behavior.CRAWL_RUSH;
        return Behavior.WALK;
    }

    /** Queues a behaviour, all of it checked against the space first; false, with nothing queued, when it cannot be played. */
    private boolean build(Behavior b, Watcher watcher, Senses senses) {
        List<Action> plan = new ArrayList<>();
        State state = new State(shown, anchor, yaw, resting);
        boolean ok = switch (b) {
            case WALK -> rise(plan, state, WALKERS, watcher) && plan.add(new Decision((w, s) -> approach(w, WALKERS)));
            case CRAWL_RUSH -> shown.place() != Place.AIR && rise(plan, state, CRAWLERS, watcher)
                    && plan.add(new Decision((w, s) -> approach(w, CRAWLERS)));
            case COFFIN_RISE -> shown.wide() && coffin(plan, state, watcher)
                    && plan.add(new Decision((w, s) -> approach(w, WALKERS)));
            case CEILING_SCUTTLE -> shown.place() == Place.CEILING && scuttle(plan, state, watcher)
                    && plan.add(new Decision((w, s) -> approach(w, WALKERS)));
            case BLINK -> blink(plan);
            case PEEK_AND_HIDE -> !peeked && shown.place() != Place.AIR && peek(plan, state, watcher);
        };
        if (ok) actions.addAll(plan);
        return ok;
    }

    // ------------------------------------------------------------------ getting up

    /** Where it stands, posed and faced, while a plan is being checked. */
    private record State(Pose pose, Vector3d anchor, float yaw, Set<Long> resting) {
    }

    /** Gets up from where it waits into the tallest of {@code walkers} that fits where it lands. */
    private boolean rise(List<Action> plan, State state, List<String> walkers, Watcher watcher) {
        Vector3d at = state.anchor();
        switch (state.pose().place()) {
            case CEILING -> {
                Double floor = MonsterSpace.floor(space, at.x, at.z, (int) Math.floor(at.y) - 1, (int) Math.floor(at.y) - 14);
                if (floor == null) return false;
                return drop(plan, state, new Vector3d(at.x, floor, at.z), walkers, watcher);
            }
            case BLOCK -> {
                State now = state;
                Move perch = new Move(Phase.RISE, rig.pose("perched"), at, state.yaw(), 10, Curve.SMOOTH, false, false);
                if (!state.pose().wide() && perch.clear(now, watcher)) {
                    plan.add(perch);
                    now = perch.end(now, watcher);
                }
                return hop(plan, now, walkers, watcher);
            }
            default -> {
                Stance stance = stance(at, state, walkers, watcher, state.resting());
                if (stance == null) return false;
                Move up = settle(stance.pose(), stance.at(), stance.yaw(), 12, state, watcher);
                if (up == null) return false;
                plan.add(up);
                return true;
            }
        }
    }

    /**
     * A change of pose where it stands, checked tick by tick: slow, or where the room is too tight
     * for the limbs to swing round slowly, quick, or at once, the way something folded too small
     * snaps open.
     */
    private Move settle(Pose to, Vector3d at, float facing, int ticks, State from, Watcher watcher) {
        for (int n : new int[]{ticks, 3, 1}) {
            Move move = new Move(Phase.RISE, to, at, facing, n, Curve.SMOOTH, false, false);
            if (move.clear(from, watcher)) return move;
        }
        return null;
    }

    /** How it lands from a ceiling: crouched, and lower where the room is lower. */
    private static final List<String> LANDINGS = List.of("crouched", "corridor crouch", "corridor crawl");

    /**
     * Lets go of the ceiling and tumbles down to {@code landing}, turning upright about the middle of
     * its body, lands low, then straightens into the tallest of {@code walkers} that fits.
     */
    private boolean drop(List<Action> plan, State state, Vector3d landing, List<String> walkers, Watcher watcher) {
        Stance low = stance(landing, LANDINGS, watcher, Set.of());
        if (low == null) return false;
        Move fall = new Move(Phase.RISE, low.pose(), landing, low.yaw(), 12, Curve.TUMBLE, false, true);
        if (!fall.clear(state, watcher)) return false;
        State down = fall.end(state, watcher);
        Stance up = stance(landing, walkers, watcher, Set.of());
        if (up == null) return false;
        Move rise = settle(up.pose(), up.at(), up.yaw(), 12, down, watcher);
        if (rise == null) return false;
        plan.add(fall);
        plan.add(rise);
        return true;
    }

    /** Steps down off the block (or the double chest) it lies on, to the side nearest the player that has room. */
    private boolean hop(List<Action> plan, State state, List<String> walkers, Watcher watcher) {
        List<Vector3d> sides = new ArrayList<>();
        Set<Long> seenCells = new HashSet<>();
        int y = (int) Math.floor(state.anchor().y - 0.01);
        for (long cell : state.resting()) {
            int[] c = MonsterSpace.unkey(cell);
            int cx = c[0], cz = c[2];
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long k = MonsterSpace.key(cx + d[0], y, cz + d[1]);
                if (state.resting().contains(k) || !seenCells.add(k)) continue;
                Double floor = MonsterSpace.floor(space, cx + d[0] + 0.5, cz + d[1] + 0.5, y + 1, y - 4);
                if (floor != null) sides.add(new Vector3d(cx + d[0] + 0.5, floor, cz + d[1] + 0.5));
            }
        }
        sides.sort(Comparator.comparingDouble(v -> v.distanceSquared(watcher.feet())));
        for (Vector3d landing : sides) {
            Stance stance = stance(landing, walkers, watcher, Set.of());
            if (stance == null) continue;
            Move down = new Move(Phase.RISE, stance.pose(), landing, stance.yaw(), 10, Curve.FALL, false, true);
            if (down.clear(state, watcher)) {
                plan.add(down);
                return true;
            }
        }
        return false;
    }

    /** From a double chest: sits up, swings its legs over the front, steps down. */
    private boolean coffin(List<Action> plan, State state, Watcher watcher) {
        State now = state;
        for (String name : List.of("sitting up on a double chest", "sitting on a double chest")) {
            if (now.pose().name().equals(name)) continue;
            Move move = new Move(Phase.RISE, rig.pose(name), now.anchor(), now.yaw(), name.startsWith("sitting up") ? 22 : 16,
                    Curve.SMOOTH, false, false);
            if (!move.clear(now, watcher)) return false;
            plan.add(move);
            now = move.end(now, watcher);
        }
        return hop(plan, now, WALKERS, watcher);
    }

    /** A pose of {@code walkers} that fits at {@code at}, facing the player if it can, square to the blocks otherwise. */
    private Stance stance(Vector3d at, List<String> walkers, Watcher watcher, Set<Long> ignore) {
        return stance(at, null, walkers, watcher, ignore);
    }

    /**
     * {@link #stance(Vector3d, List, Watcher, Set)}, also trying each posture with its box centred
     * where {@code over} (a pose standing on {@code at}, turned by its own yaw) has its box: in a
     * corridor or an alcove the room is round the body, not round the anchor.
     */
    private Stance stance(Vector3d at, State over, List<String> walkers, Watcher watcher, Set<Long> ignore) {
        float facing = MonsterMotion.toward(at, watcher.feet(), yaw);
        Vector3d middle = over == null ? null : middle(over.pose(), at, over.yaw());
        for (String name : walkers) {
            for (float y : yaws(facing)) {
                if (MonsterSpace.fits(space, at, y, walkingBox(name), ignore)) return new Stance(rig.pose(name), y, at);
                if (middle == null) continue;
                float[] box = walkingBox(name);
                double cx = (box[0] + box[3]) / 2, cz = (box[2] + box[5]) / 2;
                Vector3d own = new Vector3d(at).add(cx * Math.cos(y) + cz * Math.sin(y), 0, -cx * Math.sin(y) + cz * Math.cos(y));
                Vector3d centred = new Vector3d(at).add(middle.x - own.x, 0, middle.z - own.z);
                // Centred where it was, then out of its nook: half a block and a block the way it faces.
                for (double out : new double[]{0, 0.5, 1.0}) {
                    Vector3d moved = new Vector3d(centred).add(Math.sin(y) * out, 0, Math.cos(y) * out);
                    if (MonsterSpace.fits(space, moved, y, box, ignore)) return new Stance(rig.pose(name), y, moved);
                }
            }
        }
        return null;
    }

    private record Stance(Pose pose, float yaw, Vector3d at) {
    }

    // ------------------------------------------------------------------ getting to the player

    /** Walks (or flies) up to the player along a path that fits, or blinks when there is none. */
    private void approach(Watcher watcher, List<String> walkers) {
        if (near(watcher)) {
            grab(watcher, walkers == CRAWLERS);
            return;
        }
        if (++replans > MAX_REPLANS) {
            fallBack(watcher);
            return;
        }
        double speed = speed(walkers, watcher);
        if (watcher.flying() || shown.place() == Place.AIR) {
            Vector3d target = MonsterMotion.grabAnchor(rig, rig.pose(HANDS.get(0)), watcher.eye(), watcher.look(),
                    MonsterMotion.facing(watcher.look(), yaw));
            Vector3d toward = new Vector3d(target).sub(anchor);
            double length = toward.length();
            if (length > REACH) toward.mul((length - REACH + 0.5) / length);
            Vector3d end = new Vector3d(anchor).add(toward);
            Move fly = new Move(Phase.MOVE, rig.pose("idle"), end, null, Math.max(1, (int) Math.ceil(toward.length() / speed)),
                    Curve.LINEAR, true, true);
            if (!fly.clear(new State(base, anchor, yaw, resting), watcher)) {
                fallBack(watcher);
                return;
            }
            actions.addFirst(new Decision((w, s) -> approach(w, walkers)));
            actions.addFirst(fly);
            return;
        }
        List<Step> path = path(watcher, walkers);
        if (path == null) {
            fallBack(watcher);
            return;
        }
        List<Action> moves = new ArrayList<>();
        String posture = base.name();
        State now = new State(base, anchor, yaw, resting);
        for (Step step : path) {
            if (!step.posture().equals(posture)) {
                Move change = new Move(Phase.MOVE, rig.pose(step.posture()), now.anchor(), step.yaw(), 6, Curve.SMOOTH, false, true);
                if (!change.clear(now, watcher)) {
                    // No room to change slowly: it snaps into the new posture.
                    change = new Move(Phase.MOVE, rig.pose(step.posture()), now.anchor(), step.yaw(), 1, Curve.SMOOTH, false, true);
                    if (!change.clear(now, watcher)) {
                        fallBack(watcher);
                        return;
                    }
                }
                moves.add(change);
                now = change.end(now, watcher);
                posture = step.posture();
            }
            int ticks = Math.max(1, (int) Math.ceil(now.anchor().distance(step.at()) / speed));
            Move walk = null;
            // A step with no room to be taken slowly is taken in a jerk, the way it squeezes out of a nook.
            for (int n : new int[]{ticks, Math.min(ticks, 3), 1}) {
                Move trial = new Move(Phase.MOVE, rig.pose(step.posture()), step.at(), step.yaw(), n, Curve.LINEAR, n == ticks, true);
                if (trial.clear(now, watcher)) {
                    walk = trial;
                    break;
                }
            }
            if (walk == null) {
                fallBack(watcher);
                return;
            }
            moves.add(walk);
            now = walk.end(now, watcher);
        }
        moves.add(new Decision((w, s) -> approach(w, walkers)));
        for (int i = moves.size() - 1; i >= 0; i--) actions.addFirst(moves.get(i));
    }

    private double speed(List<String> walkers, Watcher watcher) {
        double own = settings.speed() * (walkers == CRAWLERS ? 1.4 : 1.0);
        return Math.max(own, watcher.speed() + 0.25);
    }

    private boolean near(Watcher watcher) {
        double dx = anchor.x - watcher.feet().x, dz = anchor.z - watcher.feet().z;
        return Math.sqrt(dx * dx + dz * dz) <= REACH
                && (watcher.flying() || Math.abs(anchor.y - watcher.feet().y) <= 1.5);
    }

    /** A step of a path: where it goes, in which posture, facing which way. */
    private record Step(Vector3d at, String posture, float yaw) {
    }

    private record Node(int x, int z, double y, String posture, float yaw, Node from, double cost, double estimate) {
        long key() {
            return MonsterSpace.key(x, (int) Math.round(y * 16), z);
        }
    }

    /**
     * The shortest walk, block by block, to within reach of the player, each step in the tallest
     * posture that fits where it leaves from and where it arrives (its stride included), up one block
     * or down three at most.
     */
    private List<Step> path(Watcher watcher, List<String> walkers) {
        Vector3d feet = watcher.feet();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost() + n.estimate()));
        Map<Long, Double> best = new HashMap<>();
        Node start = new Node((int) Math.floor(anchor.x), (int) Math.floor(anchor.z), anchor.y, base.name(), yaw, null, 0,
                Math.max(0, flat(anchor, feet) - REACH));
        open.add(start);
        best.put(start.key(), 0.0);
        int expansions = 0;
        while (!open.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            Node node = open.poll();
            Vector3d at = node == start ? new Vector3d(anchor) : centre(node);
            if (node != start && flat(at, feet) <= REACH - 0.3 && Math.abs(at.y - feet.y) <= 1.5) {
                List<Step> steps = new ArrayList<>();
                for (Node n = node; n != start; n = n.from()) steps.add(new Step(centre(n), n.posture(), n.yaw()));
                Collections.reverse(steps);
                return merge(steps);
            }
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = node.x() + d[0], nz = node.z() + d[1];
                if ((nx + 0.5 - feet.x) * (nx + 0.5 - feet.x) + (nz + 0.5 - feet.z) * (nz + 0.5 - feet.z) > PATH_RADIUS * PATH_RADIUS) continue;
                int level = (int) Math.floor(node.y() + 0.01);
                Double y = MonsterSpace.floor(space, nx + 0.5, nz + 0.5, level + 1, level - 3);
                if (y == null || y - node.y() > 1.05 || node.y() - y > 3.05) continue;
                Vector3d to = new Vector3d(nx + 0.5, y, nz + 0.5);
                float facing = (float) Math.atan2(d[0], d[1]);
                String posture = null;
                for (String name : walkers) {
                    float[] box = walkingBox(name);
                    // From where it stands it only needs room where it goes: the move itself is checked.
                    if ((node == start || MonsterSpace.fits(space, at, facing, box, Set.of()))
                            && MonsterSpace.fits(space, to, facing, box, Set.of())) {
                        posture = name;
                        break;
                    }
                }
                if (posture == null) continue;
                double cost = node.cost() + 1 + walkers.indexOf(posture) * 0.3;
                Node next = new Node(nx, nz, y, posture, facing, node, cost, Math.max(0, flat(to, feet) - REACH));
                Double known = best.get(next.key());
                if (known != null && known <= cost) continue;
                best.put(next.key(), cost);
                open.add(next);
            }
        }
        return null;
    }

    /** Joins steps that go on the same way in the same posture into one. */
    private static List<Step> merge(List<Step> steps) {
        List<Step> out = new ArrayList<>();
        for (Step step : steps) {
            if (!out.isEmpty()) {
                Step last = out.get(out.size() - 1);
                if (last.posture().equals(step.posture()) && Math.abs(last.yaw() - step.yaw()) < 1.0e-3 && Math.abs(last.at().y - step.at().y) < 1.0e-6) {
                    out.set(out.size() - 1, step);
                    continue;
                }
            }
            out.add(step);
        }
        return out;
    }

    private static Vector3d centre(Node node) {
        return new Vector3d(node.x() + 0.5, node.y(), node.z() + 0.5);
    }

    private static double flat(Vector3d a, Vector3d b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ------------------------------------------------------------------ the ceiling

    /** Runs along the ceiling to above the player, then drops in front of them. */
    private boolean scuttle(List<Action> plan, State state, Watcher watcher) {
        int ceiling = (int) Math.round(state.anchor().y);
        Vector3d feet = watcher.feet();
        Deque<Node> open = new ArrayDeque<>();
        Map<Long, Node> known = new HashMap<>();
        Node start = new Node((int) Math.floor(state.anchor().x), (int) Math.floor(state.anchor().z), ceiling,
                state.pose().name(), state.yaw(), null, 0, 0);
        open.add(start);
        known.put(start.key(), start);
        int expansions = 0;
        while (!open.isEmpty() && expansions++ < 800) {
            Node node = open.poll();
            Vector3d at = node == start ? new Vector3d(state.anchor()) : centre(node);
            if (node != start && flat(at, feet) <= 2.6) {
                List<Step> steps = new ArrayList<>();
                for (Node n = node; n != start; n = n.from()) steps.add(new Step(centre(n), n.posture(), n.yaw()));
                Collections.reverse(steps);
                List<Action> trial = new ArrayList<>();
                State now = state;
                for (Step step : merge(steps)) {
                    Move move = new Move(Phase.MOVE, rig.pose(step.posture()), step.at(), step.yaw(),
                            Math.max(1, (int) Math.ceil(now.anchor().distance(step.at()) / settings.speed())), Curve.LINEAR, true, true);
                    trial.add(move);
                    now = move.end(now, watcher);
                }
                Double floor = MonsterSpace.floor(space, at.x, at.z, ceiling - 1, ceiling - 14);
                if (floor != null && drop(trial, now, new Vector3d(at.x, floor, at.z), WALKERS, watcher)) {
                    plan.addAll(trial);
                    return true;
                }
            }
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = node.x() + d[0], nz = node.z() + d[1];
                if (!space.solid(nx, ceiling, nz)) continue;
                long key = MonsterSpace.key(nx, ceiling * 16, nz);
                if (known.containsKey(key) || flat(new Vector3d(nx + 0.5, 0, nz + 0.5), feet) > PATH_RADIUS) continue;
                Vector3d to = new Vector3d(nx + 0.5, ceiling, nz + 0.5);
                float facing = (float) Math.atan2(d[0], d[1]);
                for (String name : CEILING_WALKERS) {
                    float[] box = walkingBox(name);
                    if (MonsterSpace.fits(space, at, facing, box, Set.of()) && MonsterSpace.fits(space, to, facing, box, Set.of())) {
                        Node next = new Node(nx, nz, ceiling, name, facing, node, node.cost() + 1, 0);
                        known.put(key, next);
                        open.add(next);
                        break;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ the hand

    /** Reaches for the player's eyes with the tallest hand that fits, or vanishes in front of them. */
    private void grab(Watcher watcher, boolean crawling) {
        List<String> hands = new ArrayList<>(HANDS);
        if (crawling) Collections.reverse(hands);
        State now = new State(base, anchor, yaw, resting);
        for (String name : hands) for (float turn : SIDES) {
            Pose hand = rig.pose(name);
            float facing = MonsterMotion.facing(watcher.look(), yaw) + turn;
            Vector3d at = handAnchor(hand, watcher, facing);
            if (at == null || !MonsterSpace.fits(space, at, facing, still(hand), Set.of())) continue;
            Move reach = new Move(Phase.GRAB, hand, at, facing, MonsterMotion.REACH_TICKS, Curve.SMOOTH, false, true);
            List<Action> plan = new ArrayList<>();
            if (reach.clear(now, watcher)) {
                plan.add(reach);
            } else {
                // No room to reach across: it is gone for a blink and then there.
                plan.add(new Hidden(Phase.BLINK, 3));
                Vector3d there = at;
                plan.add(new Decision((w, s) -> {
                    Vector3d then = handAnchor(hand, w, facing);
                    place(hand, then != null && MonsterSpace.fits(space, then, facing, still(hand), Set.of()) ? then : there, facing, Event.BACK);
                }));
            }
            plan.add(new Decision((w, s) -> event = Event.COVERED));
            plan.add(new Hold(hand));
            plan.add(new Decision((w, s) -> vanish()));
            for (int i = plan.size() - 1; i >= 0; i--) actions.addFirst(plan.get(i));
            return;
        }
        actions.addFirst(new Decision((w, s) -> {
            event = Event.COVERED;
            vanish();
        }));
    }

    /**
     * Where its anchor goes for a hand's palm to sit just in front of the player's eyes, never under
     * the floor; {@code null} when standing on what is there would lift the palm off the eyes (on top
     * of a chest beside the player, say).
     */
    private Vector3d handAnchor(Pose hand, Watcher watcher, float facing) {
        Vector3d at = MonsterMotion.grabAnchor(rig, hand, watcher.eye(), watcher.look(), facing);
        if (!watcher.flying()) {
            Double floor = MonsterSpace.floor(space, at.x, at.z, (int) Math.floor(watcher.eye().y) + 1,
                    (int) Math.floor(watcher.feet().y) - 2);
            if (floor != null && at.y < floor) {
                if (floor - at.y > 0.3) return null;
                at.y = floor;
            }
        }
        return at;
    }

    private void place(Pose pose, Vector3d at, float facing, Event what) {
        shown = pose;
        base = pose;
        anchor = new Vector3d(at);
        yaw = facing;
        visible = true;
        resting = Set.of();
        event = what;
    }

    private void vanish() {
        visible = false;
        phase = Phase.GONE;
        event = Event.VANISHED;
        actions.clear();
    }

    /** A behaviour that did not work out midway: it blinks into the player's face instead. */
    private void fallBack(Watcher watcher) {
        behavior = Behavior.BLINK;
        fellBack = true;
        List<Action> plan = new ArrayList<>();
        blink(plan);
        for (int i = plan.size() - 1; i >= 0; i--) actions.addFirst(plan.get(i));
    }

    private boolean blink(List<Action> plan) {
        plan.add(new Flicker(10));
        plan.add(new Hidden(Phase.BLINK, 6));
        plan.add(new Decision((w, s) -> {
            for (String name : HANDS) for (float turn : SIDES) {
                Pose hand = rig.pose(name);
                float facing = MonsterMotion.facing(w.look(), yaw) + turn;
                Vector3d at = handAnchor(hand, w, facing);
                if (at != null && MonsterSpace.fits(space, at, facing, still(hand), Set.of())) {
                    place(hand, at, facing, Event.BACK);
                    actions.addFirst(new Decision((w2, s2) -> vanish()));
                    actions.addFirst(new Hold(hand));
                    return;
                }
            }
            vanish();
        }));
        return true;
    }

    // ------------------------------------------------------------------ hiding

    /** Pulls back out of sight, waits, and comes back to wait somewhere else, closer. */
    private boolean peek(List<Action> plan, State state, Watcher watcher) {
        Vector3d away = new Vector3d(state.anchor()).sub(watcher.feet()).setComponent(1, 0);
        if (away.lengthSquared() > 1.0e-6) away.normalize();
        Vector3d back = new Vector3d(away).mul(1.2).add(state.anchor());
        Move retreat = new Move(Phase.HIDE, state.pose(), back, state.yaw(), 8, Curve.SMOOTH, false, false);
        if (state.pose().place() == Place.GROUND && retreat.clear(state, watcher)) plan.add(retreat);
        plan.add(new Decision((w, s) -> {
            visible = false;
            event = Event.HID;
        }));
        plan.add(new Hidden(Phase.HIDE, 30));
        plan.add(new Decision(this::relocate));
        return true;
    }

    /** Waits again, out of the player's sight, two to five blocks from them. */
    private void relocate(Watcher watcher, Senses senses) {
        Vector3d feet = watcher.feet();
        List<Vector3d> cells = new ArrayList<>();
        int fy = (int) Math.floor(feet.y + 0.01);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 2 || d > 5) continue;
                double x = Math.floor(feet.x) + dx + 0.5, z = Math.floor(feet.z) + dz + 0.5;
                Double y = MonsterSpace.floor(space, x, z, fy + 2, fy - 3);
                if (y != null) cells.add(new Vector3d(x, y, z));
            }
        }
        cells.sort(Comparator.comparingDouble(v -> v.distanceSquared(feet)));
        List<String> poses = new ArrayList<>();
        if (shown.place() == Place.GROUND) poses.add(shown.name());
        poses.addAll(HIDING);
        for (Vector3d at : cells) {
            float facing = MonsterMotion.toward(at, feet, yaw);
            for (String name : poses) {
                Pose pose = rig.pose(name);
                for (float y : pose.space() == null ? new float[]{facing} : yaws(facing)) {
                    if (!MonsterSpace.fits(space, at, y, still(pose), Set.of())) continue;
                    Matrix4f root = new Matrix4f().rotateY(y).mul(MonsterRig.body(pose));
                    if (senses.sees(world(at, rig.eyes(pose, root)), HIDDEN_ANGLE)
                            || senses.sees(world(at, rig.chest(pose, root)), HIDDEN_ANGLE)) continue;
                    place(pose, at, y, Event.BACK);
                    phase = Phase.LURK;
                    ticks = 0;
                    seen = 0;
                    peeked = true;
                    behavior = null;
                    replans = 0;
                    return;
                }
            }
        }
        phase = Phase.GONE;
        visible = false;
        event = Event.LEFT;
    }

    // ------------------------------------------------------------------ boxes and helpers

    private float[] still(Pose pose) {
        return still.computeIfAbsent(pose.name(), n -> rig.bounds(pose, MonsterRig.body(pose)));
    }

    /** The box a posture fills while it walks: its stride at four points of the step, together. */
    private float[] walkingBox(String name) {
        return walking.computeIfAbsent(name, n -> {
            Pose pose = rig.pose(n);
            float[] box = rig.bounds(pose, MonsterRig.body(pose)).clone();
            for (float phase : STRIDE_PHASES) {
                Pose step = strode(pose, phase);
                float[] b = rig.bounds(step, MonsterRig.body(step));
                for (int i = 0; i < 3; i++) box[i] = Math.min(box[i], b[i]);
                for (int i = 3; i < 6; i++) box[i] = Math.max(box[i], b[i]);
            }
            return box;
        });
    }

    /**
     * A pose mid stride that keeps to what holds it: on the ground its lowest point never sinks below
     * the still pose's, under a ceiling its highest never rises above it, so a swinging leg lifts the
     * body instead of going into the floor.
     */
    private Pose strode(Pose under, float phase) {
        Pose step = MonsterMotion.stride(under, phase);
        float[] still = rig.bounds(under, MonsterRig.body(under));
        float[] moving = rig.bounds(step, MonsterRig.body(step));
        float dy = under.place() == Place.CEILING ? Math.min(0, still[4] - moving[4]) : Math.max(0, still[1] - moving[1]);
        return dy == 0 ? step : step.lifted(dy);
    }

    /** The middle of a pose's box, in the world, standing on {@code anchor} and turned by {@code yaw}. */
    private Vector3d middle(Pose pose, Vector3d anchor, float yaw) {
        float[] b = rig.bounds(pose, new Matrix4f().rotateY(yaw).mul(MonsterRig.body(pose)));
        return new Vector3d(anchor).add((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2);
    }

    private static boolean grounded(Pose pose) {
        return pose.place() == Place.GROUND || pose.place() == Place.ACTION;
    }

    /** The lowest point of a still pose. */
    private float low(Pose pose) {
        return still(pose)[1];
    }

    /** The angle facing the player, then the four square to the blocks, nearest first. */
    private static float[] yaws(float facing) {
        float[] out = new float[5];
        out[0] = facing;
        int k = Math.round(facing / (float) (Math.PI / 2));
        int[] order = {0, 1, -1, 2};
        for (int i = 0; i < 4; i++) out[i + 1] = (float) ((k + order[i]) * Math.PI / 2);
        return out;
    }

    private Vector3d chest() {
        Matrix4f root = new Matrix4f().rotateY(yaw).mul(MonsterRig.body(shown));
        return world(anchor, rig.chest(shown, root));
    }

    private static Vector3d world(Vector3d anchor, Vector3f local) {
        return new Vector3d(anchor).add(local.x, local.y, local.z);
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float turnTowards(float from, float to, float share) {
        double d = Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return (float) (from + d * share);
    }

    // ------------------------------------------------------------------ actions

    private interface Action {
        /** One tick; true once it is done. */
        boolean step(Watcher watcher);
    }

    /** A choice made when its turn comes, with the player as they are then; takes no tick of its own. */
    private record Decision(BiConsumer<Watcher, Senses> run) implements Action {
        @Override
        public boolean step(Watcher watcher) {
            return true;
        }
    }

    /**
     * How a move travels: evenly, eased, falling (a drop that speeds up, or a hop that arcs over an
     * edge), or tumbling: falling while it turns over about the middle of its body, not its anchor, so
     * letting go of a ceiling never swings it up into the ceiling.
     */
    private enum Curve { LINEAR, SMOOTH, FALL, TUMBLE }

    /**
     * A stretch towards a pose and an anchor over some ticks. While it walks, the pose under the
     * stride eases over quickly and the stride swings on top. {@code yaw} null keeps it facing the
     * player. {@code leave} lets go of the blocks it rested on once it is done.
     */
    private final class Move implements Action {
        private final Phase phase;
        private final Pose to;
        private final Vector3d target;
        private final Float yawTo;
        private final int ticks;
        private final Curve curve;
        private final boolean walk;
        private final boolean leave;
        private State from;
        private int k;

        Move(Phase phase, Pose to, Vector3d target, Float yawTo, int ticks, Curve curve, boolean walk, boolean leave) {
            this.phase = phase;
            this.to = to;
            this.target = new Vector3d(target);
            this.yawTo = yawTo;
            this.ticks = ticks;
            this.curve = curve;
            this.walk = walk;
            this.leave = leave;
        }

        @Override
        public boolean step(Watcher watcher) {
            if (k == 0) from = new State(base, anchor, yaw, resting);
            k++;
            Frameless f = at(from, k, watcher, stride + (walk ? strideStep(watcher) : 0));
            if (walk) stride += strideStep(watcher);
            MonsterScript.this.phase = phase;
            base = f.base();
            shown = f.pose();
            anchor = f.anchor();
            yaw = f.yaw();
            visible = true;
            if (k >= ticks) {
                if (leave) resting = Set.of();
                return true;
            }
            return false;
        }

        private float strideStep(Watcher watcher) {
            return (float) (from == null ? settings.speed() : from.anchor().distance(target) / ticks) * 2.2f;
        }

        /** Where it is {@code k} ticks in, from {@code start}. */
        private Frameless at(State start, int k, Watcher watcher, float strideNow) {
            float t = Math.min(1f, k / (float) ticks);
            Pose under = walk ? start.pose().blend(to, Math.min(1f, t * 3)) : start.pose().blend(to, smooth(t));
            if (!walk && curve != Curve.TUMBLE && grounded(start.pose()) && grounded(to)) {
                // Between two poses on the floor, the body never dips below the lower of their lowest points.
                float floor = Math.min(low(start.pose()), low(to));
                float[] b = rig.bounds(under, MonsterRig.body(under));
                if (b[1] < floor) under = under.lifted(floor - b[1]);
            }
            Pose pose = walk ? strode(under, strideNow) : under;
            Vector3d a = new Vector3d(target).sub(start.anchor()).mul(curve == Curve.SMOOTH ? smooth(t) : t).add(start.anchor());
            double drop = target.y - start.anchor().y;
            if (curve == Curve.FALL) {
                a.y = start.anchor().y + (drop < -1.5 ? drop * t * t : drop * t + Math.sin(Math.PI * t) * 0.45);
            }
            float facing = yawTo != null ? yawTo : MonsterMotion.toward(a, watcher.feet(), start.yaw());
            // A step of a path turns at once, square to the blocks, so it never sweeps a corner;
            // anything else turns over its first half, and is checked tick by tick.
            float y = walk && yawTo != null ? facing : turnTowards(start.yaw(), facing, ticks == 1 ? 1f : Math.min(1f, t * 2));
            if (curve == Curve.TUMBLE) {
                // The middle of the body falls (faster and faster) from where it was to where it lands,
                // and the anchor is put wherever keeps that middle there while the body turns over.
                Vector3d from = middle(start.pose(), start.anchor(), start.yaw());
                Vector3d to = middle(this.to, target, yawTo != null ? yawTo : y);
                Vector3d mid = new Vector3d(to).sub(from).mul(t).add(from);
                mid.y = from.y + (to.y - from.y) * t * t;
                Vector3d now = middle(under, new Vector3d(), y);
                a = mid.sub(now);
                if (k >= ticks) a = new Vector3d(target);
            }
            return new Frameless(under, pose, a, y);
        }

        /** True when every tick of it, from {@code start}, keeps the body out of the blocks. */
        boolean clear(State start, Watcher watcher) {
            float s = 0;
            for (int i = 1; i <= ticks; i++) {
                if (walk) s += (float) (start.anchor().distance(target) / ticks) * 2.2f;
                Frameless f = at(start, i, watcher, s);
                Set<Long> ignore = leave && i == ticks ? Set.of() : start.resting();
                if (!MonsterSpace.fits(space, f.anchor(), f.yaw(), rig.bounds(f.pose(), MonsterRig.body(f.pose())), ignore)) {
                    return false;
                }
            }
            return true;
        }

        /** Where it ends up. */
        State end(State start, Watcher watcher) {
            Frameless f = at(start, ticks, watcher, 0);
            return new State(f.base(), f.anchor(), f.yaw(), leave ? Set.of() : start.resting());
        }
    }

    private record Frameless(Pose base, Pose pose, Vector3d anchor, float yaw) {
    }

    /** Out of sight for some ticks. */
    private final class Hidden implements Action {
        private final Phase phase;
        private int left;

        Hidden(Phase phase, int ticks) {
            this.phase = phase;
            this.left = ticks;
        }

        @Override
        public boolean step(Watcher watcher) {
            MonsterScript.this.phase = phase;
            visible = false;
            return --left <= 0;
        }
    }

    /** Flickers in and out where it stands. */
    private final class Flicker implements Action {
        private final int ticks;
        private int k;

        Flicker(int ticks) {
            this.ticks = ticks;
        }

        @Override
        public boolean step(Watcher watcher) {
            phase = Phase.BLINK;
            visible = (k / 2) % 2 == 0;
            return ++k >= ticks;
        }
    }

    /** Holds its palm over the player's eyes, following them while it still fits. */
    private final class Hold implements Action {
        private final Pose hand;
        /** How far round from straight in front it holds its hand, kept while it follows the player. */
        private Float turn;
        private int k;

        Hold(Pose hand) {
            this.hand = hand;
        }

        @Override
        public boolean step(Watcher watcher) {
            phase = Phase.GRAB;
            if (turn == null) turn = (float) Math.atan2(Math.sin(yaw - MonsterMotion.facing(watcher.look(), yaw)),
                    Math.cos(yaw - MonsterMotion.facing(watcher.look(), yaw)));
            float facing = MonsterMotion.facing(watcher.look(), yaw) + turn;
            Vector3d at = handAnchor(hand, watcher, facing);
            if (at != null && MonsterSpace.fits(space, at, facing, still(hand), Set.of())) {
                anchor = at;
                yaw = facing;
            }
            shown = hand;
            base = hand;
            visible = true;
            return ++k >= settings.handTicks();
        }
    }

    // ------------------------------------------------------------------ rehearsal

    /** A rehearsed visit: the frames, where the player looked in each (yaw in degrees), and what it played. */
    public record Take(List<Frame> frames, List<Float> looks, Behavior played) {
    }

    /**
     * Plays a visit in the pose's own blocks (or on open ground) with its player standing still: it
     * waits a second, the player turns to it, and it plays {@code behavior} (or what the game would
     * choose, when {@code null}). After hiding, the player turns to it again when it is back.
     */
    public static Take rehearse(MonsterRig rig, Pose pose, Behavior behavior, Settings settings, long seed) {
        MonsterSpace space = pose.world() != null ? pose.world().space() : new MonsterSpace.Blocks(true);
        float[] p = pose.player();
        Vector3d feet = new Vector3d(p[0], p[1], p[2]);
        Vector3d eye = new Vector3d(feet).add(0, 1.62, 0);
        double playerYaw = Math.toRadians(p[3]);
        Vector3d look = new Vector3d(Math.sin(playerYaw), 0, Math.cos(playerYaw));
        boolean flying = pose.place() == Place.AIR;
        MonsterScript script = new MonsterScript(rig, space, pose, new Vector3d(pose.at()), (float) Math.toRadians(pose.yaw()),
                settings, new Random(seed), behavior);
        List<Frame> frames = new ArrayList<>();
        List<Float> looks = new ArrayList<>();
        int waited = 0;
        Vector3d turnFrom = null;
        int turning = 0;
        for (int tick = 0; tick < 1200; tick++) {
            boolean seenNow = false;
            if (script.phase() == Phase.LURK && script.visible) {
                waited++;
                if (waited > 20) {
                    Matrix4f root = new Matrix4f().rotateY(script.yaw()).mul(MonsterRig.body(script.pose()));
                    Vector3d eyes = world(script.anchor(), rig.eyes(script.pose(), root));
                    Vector3d wanted = new Vector3d(eyes).sub(eye).normalize();
                    if (turnFrom == null) {
                        turnFrom = new Vector3d(look);
                        turning = 0;
                    }
                    turning = Math.min(6, turning + 1);
                    look = new Vector3d(turnFrom).lerp(wanted, smooth(turning / 6f)).normalize();
                    seenNow = look.angle(wanted) < Math.toRadians(settings.viewAngle());
                }
            } else {
                waited = 0;
                turnFrom = null;
                // Once it moves the player follows it with their eyes, until its hand is over them.
                if (script.visible && script.phase() != Phase.GRAB && script.phase() != Phase.HIDE) {
                    Matrix4f root = new Matrix4f().rotateY(script.yaw()).mul(MonsterRig.body(script.pose()));
                    Vector3d eyes = world(script.anchor(), rig.eyes(script.pose(), root));
                    look = new Vector3d(look).lerp(new Vector3d(eyes).sub(eye).normalize(), 0.3).normalize();
                }
            }
            Vector3d sight = new Vector3d(look);
            Senses senses = (point, angle) -> new Vector3d(point).sub(eye).angle(sight) < Math.toRadians(angle);
            Frame frame = script.tick(new Watcher(feet, eye, new Vector3d(look), flying, 0), seenNow, senses);
            frames.add(frame);
            looks.add((float) Math.toDegrees(Math.atan2(look.x, look.z)));
            if (frame.phase() == Phase.GONE) break;
        }
        return new Take(frames, looks, script.played());
    }
}
