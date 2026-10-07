package com.Chagui68.utils;

import com.Chagui68.testsupport.ProjectPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rules read off the sources, each one a bug that compiled, passed every other test and only showed
 * up in game:
 * <ul>
 *   <li><b>FLASH needs a colour.</b> Since 1.21 the {@code FLASH} particle carries one, and spawning it
 *   bare throws from inside whatever event or tick called it; a JackStar reboot did, on every hit.</li>
 *   <li><b>No silent catch.</b> Twenty-one empty catch blocks had hidden a rejected attribute, a failed
 *   world purge and a song that never started. A catch reports through {@link MscLog}; a comment is not
 *   a body.</li>
 *   <li><b>Names are Components.</b> Item names, lore and mob names are Adventure Components; the
 *   deprecated String setters still work and would show up only as a subtly wrong tooltip. Chat,
 *   titles and bars are a different concern and are not flagged; matches in comments do not count.</li>
 * </ul>
 */
class SourceGuardsTest {

    // ------------------------------------------------------------------ FLASH particles

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

    // ------------------------------------------------------------------ silent catches

    /**
     * Catches that are allowed to stay silent. Keep it empty: a failure worth catching but not worth
     * reporting is a failure worth reporting at {@code MscLog.debug}.
     */
    private static final List<String> ALLOWED = List.of();

    /** Sanity floor: the scan must keep finding the plugin's catch blocks instead of passing empty. */
    private static final int MINIMUM_CATCHES = 40;

    @Test
    @DisplayName("No catch block swallows its exception silently")
    void everyCatchReportsOrExplains() {
        List<String> silent = new ArrayList<>();
        int total = 0;

        for (Path file : sources()) {
            String source = withoutCommentsAndStrings(ProjectPaths.read(file));
            int index = 0;
            while ((index = source.indexOf("catch", index)) >= 0) {
                if (!isKeyword(source, index)) {
                    index += "catch".length();
                    continue;
                }
                int paren = source.indexOf('(', index);
                int open = paren < 0 ? -1 : source.indexOf('{', paren);
                int close = open < 0 ? -1 : matchingBrace(source, open);
                if (close < 0) break;

                total++;
                String body = source.substring(open + 1, close);
                if (body.isBlank()) {
                    String where = ProjectPaths.relative(file) + ":" + lineOf(source, index);
                    if (!ALLOWED.contains(where)) silent.add(where);
                }
                index = close;
            }
        }

        assertTrue(total >= MINIMUM_CATCHES,
                "only " + total + " catch blocks were found; the scanner stopped matching the sources");
        assertTrue(silent.isEmpty(),
                "these catch blocks swallow their exception; report it with MscLog.debug/warn instead: " + silent);
    }

    // ------------------------------------------------------------------ helpers

    private static List<Path> sources() {
        return ProjectPaths.javaFiles(ProjectPaths.mainJava());
    }

    /** {@code catch} is only a catch clause when it stands alone in front of its parentheses. */
    private static boolean isKeyword(String source, int index) {
        boolean cleanBefore = index == 0 || !Character.isJavaIdentifierPart(source.charAt(index - 1));
        int after = index + "catch".length();
        boolean cleanAfter = after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after));
        return cleanBefore && cleanAfter;
    }

    private static int matchingBrace(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int lineOf(String source, int index) {
        int line = 1;
        for (int i = 0; i < index && i < source.length(); i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }

    /**
     * Blanks out comments, string literals and char literals, keeping the line count so the reported
     * line numbers still point at the real code. Without this the javadoc that documents this guard
     * would be scanned as if it were code.
     */
    private static String withoutCommentsAndStrings(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                while (i < source.length() && source.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                out.append("  ");
                i += 2;
                while (i < source.length()
                        && !(source.charAt(i) == '*' && i + 1 < source.length() && source.charAt(i + 1) == '/')) {
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < source.length()) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' || c == '\'') {
                out.append(' ');
                i++;
                while (i < source.length() && source.charAt(i) != c) {
                    if (source.charAt(i) == '\\') {
                        out.append(' ');
                        i++;
                    }
                    if (i < source.length()) {
                        out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
                if (i < source.length()) {
                    out.append(' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ legacy name API

    /** The deprecated String-based name and lore APIs, with the parenthesis so prose does not match. */
    private static final List<String> LEGACY_APIS = List.of(
            "setDisplayName(", "setLore(", "setItemName(",
            "setCustomName(", "getDisplayName(", "getCustomName(");

    @Test
    @DisplayName("No item or mob name is built through the deprecated String API, and the scan catches one that is")
    void noItemOrMobNameUsesTheStringApi() throws IOException {
        List<Path> javaFiles = ProjectPaths.javaFiles(ProjectPaths.mainJava());
        // Without this the guard could pass vacuously if the walk ever stopped finding the sources.
        assertTrue(javaFiles.size() > 100,
                "Expected to scan the whole main source set, found only " + javaFiles.size() + " files");

        List<String> offenders = javaFiles.stream()
                .flatMap(SourceGuardsTest::findLegacyUsage)
                .sorted()
                .toList();

        assertTrue(offenders.isEmpty(),
                "Item names, item lore and mob names must be built as Adventure Components. "
                        + "Use MscText (or displayName/lore/itemName/customName) instead of:\n  "
                        + String.join("\n  ", offenders));

        // And the scan really detects what it looks for, so an empty result means something.
        Path sample = Files.createTempFile("legacy-name-api", ".java");
        try {
            Files.writeString(sample, String.join("\n",
                    "class Sample {",
                    "    void names(ItemMeta meta, Entity entity) {",
                    "        meta.setDisplayName(\"x\");",
                    "        meta.setLore(java.util.List.of(\"y\"));",
                    "        meta.setItemName(\"z\");",
                    "        entity.setCustomName(\"n\");",
                    "        String a = meta.getDisplayName();",
                    "        String b = entity.getCustomName();",
                    "    }",
                    "}"));
            assertEquals(6, findLegacyUsage(sample).count(),
                    "Each of the six deprecated APIs must be reported");

            Files.writeString(sample, String.join("\n",
                    "class Clean {",
                    "    // meta.setLore(List.of(\"ignored inside a comment\"))",
                    "    void ok(ItemMeta meta) {",
                    "        meta.displayName(MscText.plain(\"x\"));",
                    "    }",
                    "}"));
            assertEquals(0, findLegacyUsage(sample).count(),
                    "Component-based calls and comments must not be reported");
        } finally {
            Files.deleteIfExists(sample);
        }
    }

    /** Every line of a file that calls a deprecated name API outside a comment. */
    private static Stream<String> findLegacyUsage(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }

        List<String> found = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) continue;
            for (String api : LEGACY_APIS) {
                if (line.contains(api)) {
                    found.add(file + ":" + (i + 1) + " -> " + line);
                }
            }
        }
        return found.stream();
    }
}
