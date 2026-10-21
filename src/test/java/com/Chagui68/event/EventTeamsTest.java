package com.Chagui68.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class EventTeamsTest {

    private final UUID ana = UUID.randomUUID(), ben = UUID.randomUUID(), cai = UUID.randomUUID();

    private EventTeams teams() {
        return new EventTeams(null, Logger.getAnonymousLogger());
    }

    @Test
    @DisplayName("A leader invites, the invited player joins, and the commands on offer follow who they are")
    void inviteAndJoin() {
        EventTeams t = teams();
        EventCommand command = new EventCommand(t);
        assertEquals(List.of("create", "join", "list", "info"), command.options(ana));
        assertEquals(EventTeams.Result.OK, t.create(ana, "Drakes"));
        assertEquals(EventTeams.Result.NAME_TAKEN, t.create(ben, "drakes"));
        assertEquals(EventTeams.Result.BAD_NAME, t.create(ben, "x"));
        assertEquals(EventTeams.Result.NOT_INVITED, t.join(ben, "Drakes"));
        assertEquals(EventTeams.Result.OK, t.invite(ana, ben));
        assertEquals(EventTeams.Result.OK, t.join(ben, "drakes"));
        assertEquals(EventTeams.Result.NOT_LEADER, t.invite(ben, cai));
        assertEquals(EventTeams.Result.TARGET_HAS_TEAM, t.invite(ana, ben));
        assertTrue(command.options(ana).contains("disband"));
        assertEquals(List.of("leave", "list", "info"), command.options(ben));
    }

    @Test
    @DisplayName("A leader who leaves hands the team on; the last one out disbands it")
    void leaving() {
        EventTeams t = teams();
        t.create(ana, "Drakes");
        t.invite(ana, ben);
        t.join(ben, "Drakes");
        assertEquals(EventTeams.Result.OK, t.leave(ana));
        assertEquals(ben, t.team("Drakes").leader());
        assertNull(t.teamOf(ana));
        assertEquals(EventTeams.Result.OK, t.leave(ben));
        assertNull(t.team("Drakes"));
        assertTrue(t.teams().isEmpty());
    }

    @Test
    @DisplayName("Teams are ranked by what their members dealt, players by their own, highest first")
    void ranking() {
        Map<UUID, Double> damage = Map.of(ana, 100.0, ben, 300.0, cai, 250.0);
        Map<String, Double> byTeam = EventRanking.byTeam(damage, p -> p.equals(cai) ? null : p.equals(ana) ? "A" : "B");
        assertEquals(Map.of("A", 100.0, "B", 300.0), byTeam);
        List<Map.Entry<UUID, Double>> top = EventRanking.top(damage, 2);
        assertEquals(List.of(ben, cai), top.stream().map(Map.Entry::getKey).toList());
    }
}
