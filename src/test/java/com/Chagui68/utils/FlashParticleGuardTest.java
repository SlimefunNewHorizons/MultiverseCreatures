package com.Chagui68.utils;

import com.Chagui68.testsupport.ProjectPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Since 1.21 the {@code FLASH} particle carries a colour, and spawning it without one throws
 * {@code IllegalArgumentException: missing required data class org.bukkit.Color} — from inside
 * whatever event or tick called it. A JackStar reboot spawned one bare, so every hit that triggered
 * the reboot failed with that exception. This guard reads every {@code spawnParticle} call on
 * {@code FLASH} and requires a colour in its arguments.
 */
class FlashParticleGuardTest {

    private static final Pattern FLASH_CALL = Pattern.compile("spawnParticle\\(\\s*(?:org\\.bukkit\\.)?Particle\\.FLASH\\b");

    @Test
    @DisplayName("Every FLASH particle is spawned with a colour")
    void everyFlashHasAColor() {
        List<String> offenders = new ArrayList<>();
        int calls = 0;
        for (Path file : ProjectPaths.javaFiles(ProjectPaths.mainJava())) {
            String source = ProjectPaths.read(file);
            Matcher matcher = FLASH_CALL.matcher(source);
            while (matcher.find()) {
                calls++;
                String arguments = argumentsFrom(source, matcher.start() + "spawnParticle".length());
                if (!arguments.contains("Color") && !arguments.contains("color")) {
                    int line = 1 + (int) source.substring(0, matcher.start()).chars().filter(c -> c == '\n').count();
                    offenders.add(ProjectPaths.relative(file) + ":" + line);
                }
            }
        }
        assertTrue(calls > 0, "the scan found no FLASH particles at all");
        assertTrue(offenders.isEmpty(), "FLASH spawned without a Color (throws at runtime):\n  " + String.join("\n  ", offenders));
    }

    /** The text between the parenthesis at {@code open} and the one that closes it. */
    private static String argumentsFrom(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return source.substring(open + 1, i);
        }
        return source.substring(open);
    }
}
