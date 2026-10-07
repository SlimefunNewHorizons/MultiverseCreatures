package com.Chagui68.listener.bossdimension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BossFightGuardTest {

    @Test
    @DisplayName("Mid-fight only the documented commands run, in any case and namespace; lookalikes and nothing are blocked")
    void onlyTheDocumentedCommandsRun() {
        for (String allowed : new String[]{"/say hi", "/me waves", "/help", "/?", "/dimtp", "/HELP 2", "/minecraft:say hi", "  /me  "}) {
            assertTrue(BossFightGuard.isAllowedCommand(allowed), allowed + " must be allowed");
        }
        for (String blocked : new String[]{"/menu", "/meteor", "/sayhi", "/helpop", "/home", "/spawn", "/tp Steve", "/msg a b", "", null}) {
            assertFalse(BossFightGuard.isAllowedCommand(blocked), blocked + " must be blocked");
        }
    }
}
