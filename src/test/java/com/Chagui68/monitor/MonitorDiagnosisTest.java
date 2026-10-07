package com.Chagui68.monitor;

import com.Chagui68.monitor.MonitorDiagnosis.Finding;
import com.Chagui68.monitor.MonitorDiagnosis.Level;
import com.Chagui68.monitor.MonitorDiagnosis.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitorDiagnosisTest {

    private static Metrics healthy() {
        return new Metrics(20, 12, 40, 50, 0.01, 0, Map.of(), 800, 100, 50, 30, 60, 150, 0, 3, 0.3);
    }

    private static boolean has(List<Finding> findings, Level level, String text) {
        return findings.stream().anyMatch(f -> f.level() == level && f.title().contains(text));
    }

    @Test
    @DisplayName("A healthy server reports a single OK finding")
    void healthyServer() {
        List<Finding> findings = MonitorDiagnosis.analyze(healthy());
        assertEquals(1, findings.size());
        assertEquals(Level.OK, findings.get(0).level());
    }

    @Test
    @DisplayName("Low TPS and slow ticks are critical")
    void lowTps() {
        Metrics m = new Metrics(9, 80, 300, 50, 0.01, 0, Map.of(), 800, 100, 50, 30, 60, 150, 0, 3, 0.3);
        List<Finding> findings = MonitorDiagnosis.analyze(m);
        assertTrue(has(findings, Level.CRITICAL, "TPS"));
        assertTrue(has(findings, Level.CRITICAL, "Ticks take"));
    }

    @Test
    @DisplayName("A nearly full heap and heavy GC are called out")
    void memory() {
        Metrics m = new Metrics(20, 12, 40, 95, 0.2, 0, Map.of(), 800, 100, 50, 30, 60, 150, 0, 3, 0.3);
        List<Finding> findings = MonitorDiagnosis.analyze(m);
        assertTrue(has(findings, Level.CRITICAL, "Memory"));
        assertTrue(has(findings, Level.CRITICAL, "Garbage"));
    }

    @Test
    @DisplayName("Freezes name the plugin that caused most of them")
    void freezes() {
        Metrics m = new Metrics(20, 12, 40, 50, 0.01, 3, Map.of("MultiverseCreatures", 2, StallSource.SERVER, 1),
                800, 100, 50, 30, 60, 150, 0, 3, 0.3);
        Finding f = MonitorDiagnosis.analyze(m).get(0);
        assertTrue(f.title().startsWith("3 freezes"));
        assertTrue(f.detail().contains("MultiverseCreatures"));
    }

    @Test
    @DisplayName("Boss particles far over budget are reported when custom entities are alive")
    void particles() {
        Metrics m = new Metrics(20, 12, 40, 50, 0.01, 0, Map.of(), 800, 100, 50, 30, 900, 150, 4000, 3, 0.3);
        assertTrue(has(MonitorDiagnosis.analyze(m), Level.WARN, "particles"));
    }
}
