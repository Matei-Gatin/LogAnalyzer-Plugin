package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.SecurityAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityAnalyzerTest {

    private SecurityAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new SecurityAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testDetectSuspiciousAdminRequests() {
        entries.add(createEntry("10.0.0.1", "GET", "/admin", HttpStatus.FORBIDDEN));
        entries.add(createEntry("10.0.0.1", "GET", "/wp-admin", HttpStatus.FORBIDDEN));
        entries.add(createEntry("192.168.1.1", "GET", "/api/users", HttpStatus.OK));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertTrue((Integer) data.get("suspiciousRequestCount") >= 2);
    }

    @Test
    void testDetectBruteForceAttempts() {
        String attackerIP = "10.0.0.50";

        // Create 12 failed authentication attempts from same IP
        for (int i = 0; i < 12; i++) {
            entries.add(createEntry(attackerIP, "POST", "/login", HttpStatus.UNAUTHORIZED));
        }

        entries.add(createEntry("192.168.1.1", "GET", "/api/data", HttpStatus.OK));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        @SuppressWarnings("unchecked")
        List<String> potentialAttackers = (List<String>) data.get("potentialAttackers");

        assertTrue(potentialAttackers.contains(attackerIP));
    }

    @Test
    void testDetectPathTraversalAttempts() {
        entries.add(createEntry("10.0.0.1", "GET", "/../etc/passwd", HttpStatus.NOT_FOUND));
        entries.add(createEntry("10.0.0.1", "GET", "/../../config", HttpStatus.NOT_FOUND));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertTrue((Integer) data.get("suspiciousRequestCount") >= 2);
    }

    @Test
    void testNoSuspiciousActivity() {
        entries.add(createEntry("192.168.1.1", "GET", "/api/users", HttpStatus.OK));
        entries.add(createEntry("192.168.1.2", "POST", "/api/data", HttpStatus.CREATED));
        entries.add(createEntry("192.168.1.3", "GET", "/dashboard", HttpStatus.OK));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(0, data.get("suspiciousRequestCount"));

        @SuppressWarnings("unchecked")
        List<String> potentialAttackers = (List<String>) data.get("potentialAttackers");
        assertTrue(potentialAttackers.isEmpty());
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Security Analyzer", analyzer.getAnalyzerName());
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);

        assertNotNull(result);
        assertEquals(0, result.getData().get("suspiciousRequestCount"));
    }

    @Test
    void testMultipleForbiddenRequests() {
        for (int i = 0; i < 5; i++) {
            entries.add(createEntry("10.0.0.1", "GET", "/admin", HttpStatus.FORBIDDEN));
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        @SuppressWarnings("unchecked")
        Map<String, Long> unauthorizedAttempts = (Map<String, Long>) data.get("topOffendingIPs");

        assertNotNull(unauthorizedAttempts);
        assertTrue(unauthorizedAttempts.containsKey("10.0.0.1"));
    }

    private AbstractLogEntry createEntry(String ip, String method, String endpoint, HttpStatus status) {
        return new ApacheLogEntry(
            ip,
            "user",
            ZonedDateTime.now(),
            method,
            endpoint,
            status,
            0
        );
    }
}
