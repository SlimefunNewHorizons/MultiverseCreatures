package com.Chagui68.entities.boss;

import com.Chagui68.testsupport.ProjectPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the Sentinel's attack rotation: every attack it owns is reachable, and it does not throw the
 * same few moves back to back.
 */
class SentinelAttackPoolTest {

    /** Attacks driven by the AI loop itself rather than drawn from a pool. */
    private static final Set<String> DRIVEN_ELSEWHERE = Set.of(
            "groundslam", "trianglecall", "airslam", "hoverbarrage", "shieldseal");

    private static final SentinelAttackPool.Situation GROUND_HURT = new SentinelAttackPool.Situation(
            10, 0.5, false, false, true, true, true, true, true, true, true);

    @Test
    @DisplayName("Every registered attack is drawn from some pool or driven by the AI loop")
    void everyAttackIsReachable() {
        Set<String> pooled = new HashSet<>();
        Stream.of(SentinelAttackPool.GROUND_CLOSE, SentinelAttackPool.GROUND_MEDIUM,
                SentinelAttackPool.GROUND_FAR, SentinelAttackPool.RANGED, SentinelAttackPool.AERIAL_CLOSE,
                SentinelAttackPool.AERIAL_MEDIUM, SentinelAttackPool.AERIAL_FAR, SentinelAttackPool.DEFENSE_STATES,
                SentinelAttackPool.DEFENSE_HEALS, SentinelAttackPool.SUMMONINGS, SentinelAttackPool.DESTRUCTIVES,
                List.of(SentinelAttackPool.CHAMPION_CALL)).forEach(pooled::addAll);

        Set<String> registered = registeredAttackNames();
        assertFalse(registered.isEmpty(), "no attack names were found to check");
        for (String name : registered) {
            assertTrue(pooled.contains(name) || DRIVEN_ELSEWHERE.contains(name),
                    name + " is registered but no pool ever picks it, so the Sentinel never uses it");
        }
        for (String name : pooled) {
            assertTrue(registered.contains(name), name + " is pooled but no attack answers to that name");
        }
    }

    @Test
    @DisplayName("A flight can end early: every aerial pool holds enough attacks")
    void aerialPoolsCoverAFlight() {
        for (List<String> pool : List.of(SentinelAttackPool.AERIAL_CLOSE, SentinelAttackPool.AERIAL_MEDIUM,
                SentinelAttackPool.AERIAL_FAR)) {
            assertTrue(new HashSet<>(pool).size() >= SentinelAttackPool.AERIAL_ATTACKS_PER_FLIGHT, pool.toString());
        }
    }

    @Test
    @DisplayName("Recent attacks are skipped until the pool runs out")
    void recentAttacksAreSkipped() {
        Random random = new Random(1);
        Deque<String> recent = new ArrayDeque<>();
        List<String> pool = SentinelAttackPool.GROUND_MEDIUM;
        for (int i = 0; i < 500; i++) {
            String pick = SentinelAttackPool.pick(pool, recent, random);
            assertFalse(recent.contains(pick), pick + " was thrown again within " + SentinelAttackPool.HISTORY + " attacks");
            SentinelAttackPool.remember(recent, pick);
            assertTrue(recent.size() <= SentinelAttackPool.HISTORY);
        }
    }

    @Test
    @DisplayName("A pool smaller than the history still yields an attack")
    void smallPoolsNeverRunDry() {
        Deque<String> recent = new ArrayDeque<>(List.of("a", "b"));
        assertNotNull(SentinelAttackPool.pick(List.of("a", "b"), recent, new Random(2)));
        assertNull(SentinelAttackPool.pick(List.of(), recent, new Random(2)));
    }

    @Test
    @DisplayName("The attack tick never repeats one of the last three attacks, of any kind")
    void attackTickHonoursTheRepeatLock() {
        Random random = new Random(3);
        Deque<String> recent = new ArrayDeque<>();
        for (int i = 0; i < 2000; i++) {
            String pick = SentinelAttackPool.next(GROUND_HURT, SentinelAttackPool.Weights.DEFAULT, recent, Set.of(), random);
            assertNotNull(pick);
            assertFalse(recent.contains(pick), pick + " came back within " + SentinelAttackPool.HISTORY + " attacks");
            SentinelAttackPool.remember(recent, pick);
        }
    }

    @Test
    @DisplayName("Over a fight every kind of attack comes up, and no single attack dominates")
    void attackTickVaries() {
        Random random = new Random(5);
        Deque<String> recent = new ArrayDeque<>();
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int rolls = 5000;
        for (int i = 0; i < rolls; i++) {
            String pick = SentinelAttackPool.next(GROUND_HURT, SentinelAttackPool.Weights.DEFAULT, recent, Set.of(), random);
            SentinelAttackPool.remember(recent, pick);
            counts.merge(pick, 1, Integer::sum);
        }
        for (String kindAttack : List.of("flyup", "hoverbarrage", "shieldseal", "groundslam", "regeneration",
                "bulwark", "afterimage", "lancestorm", "lancesnipe")) {
            assertTrue(counts.containsKey(kindAttack), kindAttack + " never came up");
        }
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            assertTrue(e.getValue() < rolls * 0.12, e.getKey() + " came up " + e.getValue() + " times of " + rolls);
        }
    }

    @Test
    @DisplayName("Defences wait for damage, heals for real damage, and only one defensive state holds")
    void defencesFollowTheBossState() {
        Random random = new Random(9);
        SentinelAttackPool.Situation fresh = new SentinelAttackPool.Situation(10, 1.0, false, false, true, true, true, true, true, true, true);
        SentinelAttackPool.Situation scratched = new SentinelAttackPool.Situation(10, 0.8, false, false, true, true, true, true, true, true, true);
        SentinelAttackPool.Situation guarded = new SentinelAttackPool.Situation(10, 0.5, false, false, true, false, false, true, true, true, true);
        for (int i = 0; i < 2000; i++) {
            String a = SentinelAttackPool.next(fresh, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertFalse(SentinelAttackPool.DEFENSE_STATES.contains(a) || SentinelAttackPool.DEFENSE_HEALS.contains(a), a);
            String b = SentinelAttackPool.next(scratched, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertFalse(SentinelAttackPool.DEFENSE_HEALS.contains(b), b + " healed above 70% health");
            String c = SentinelAttackPool.next(guarded, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertFalse(SentinelAttackPool.DEFENSE_STATES.contains(c) || SentinelAttackPool.DEFENSE_HEALS.contains(c),
                    c + " stacked on a running defence");
        }
    }

    @Test
    @DisplayName("Behind the seal and in the air only the attacks that work there are rolled")
    void sealAndFlightLimitTheKinds() {
        Random random = new Random(13);
        SentinelAttackPool.Situation sealed = new SentinelAttackPool.Situation(10, 0.5, false, true, true, true, true, true, true, true, true);
        SentinelAttackPool.Situation flying = new SentinelAttackPool.Situation(20, 0.5, true, false, true, true, true, true, true, true, true);
        for (int i = 0; i < 1000; i++) {
            String s = SentinelAttackPool.next(sealed, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertTrue(SentinelAttackPool.RANGED.contains(s) || s.equals("groundslam") || s.equals("trianglecall"), s);
            String f = SentinelAttackPool.next(flying, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertTrue(SentinelAttackPool.AERIAL_MEDIUM.contains(f) || SentinelAttackPool.RANGED.contains(f), f);
        }
    }

    @Test
    @DisplayName("Summons respect the cap, the champion its phase, and destructives their gap")
    void newKindsFollowTheirGates() {
        Random random = new Random(21);
        SentinelAttackPool.Situation closed = new SentinelAttackPool.Situation(10, 0.5, false, false, true, true, true, true,
                false, false, false);
        SentinelAttackPool.Situation open = SentinelAttackPool.Situation.open(10, 0.5);
        java.util.Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            String c = SentinelAttackPool.next(closed, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random);
            assertFalse(SentinelAttackPool.SUMMONINGS.contains(c) || SentinelAttackPool.DESTRUCTIVES.contains(c)
                    || c.equals(SentinelAttackPool.CHAMPION_CALL), c + " was thrown through a closed gate");
            seen.add(SentinelAttackPool.next(open, SentinelAttackPool.Weights.DEFAULT, List.of(), Set.of(), random));
        }
        assertTrue(seen.contains(SentinelAttackPool.CHAMPION_CALL), "the champion is never called");
        for (String name : SentinelAttackPool.DESTRUCTIVES) assertTrue(seen.contains(name), name + " never came up");
        for (String name : SentinelAttackPool.SUMMONINGS) assertTrue(seen.contains(name), name + " never came up");
    }

    @Test
    @DisplayName("Each phase keeps the passives of the ones before and adds its own")
    void passivesStack() {
        SentinelPassives none = SentinelPassives.forPhase(0);
        assertEquals(SentinelPassives.NONE, none);
        double previousInterval = 1;
        for (int phase = 1; phase <= 4; phase++) {
            SentinelPassives p = SentinelPassives.forPhase(phase);
            assertTrue(p.attackInterval() <= previousInterval, "phase " + phase + " attacks slower");
            previousInterval = p.attackInterval();
            assertNotNull(SentinelPassives.announcement(phase));
        }
        assertTrue(SentinelPassives.forPhase(2).damageTaken() < 1);
        assertTrue(SentinelPassives.forPhase(3).lightningEvery() > 0);
        SentinelPassives last = SentinelPassives.forPhase(4);
        assertTrue(last.regenPerSecond() > 0 && last.damageDealt() > 1 && last.destructiveFactor() > 1);
    }

    /** Every {@code getName()} return value under the attack package. */
    private static Set<String> registeredAttackNames() {
        Set<String> names = new HashSet<>();
        Pattern pattern = Pattern.compile("getName\\(\\)\\s*\\{\\s*return\\s+\"([a-z]+)\"");
        for (Path file : ProjectPaths.javaFiles(ProjectPaths.source("com", "Chagui68", "entities", "boss", "attack"))) {
            Matcher matcher = pattern.matcher(ProjectPaths.read(file));
            while (matcher.find()) names.add(matcher.group(1));
        }
        return names;
    }
}
