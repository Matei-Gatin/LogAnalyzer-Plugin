package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.TopEndpointsAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TopEndpointsAnalyzerTest {

    private TopEndpointsAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new TopEndpointsAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testTopEndpointsIdentification() {
        // Add multiple requests to different endpoints
        for (int i = 0; i < 10; i++) {
            entries.add(createEntry("GET /api/users HTTP/1.1"));
        }
        for (int i = 0; i < 5; i++) {
            entries.add(createEntry("GET /api/products HTTP/1.1"));
        }
        for (int i = 0; i < 3; i++) {
            entries.add(createEntry("GET /dashboard HTTP/1.1"));
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertNotNull(data);
        assertEquals(10L, data.get("/api/users"));
        assertEquals(5L, data.get("/api/products"));
        assertEquals(3L, data.get("/dashboard"));
    }

    @Test
    void testTopEndpointsLimit() {
        // Create more than 5 different endpoints
        for (int i = 1; i <= 10; i++) {
            for (int j = 0; j < i; j++) {
                entries.add(createEntry("GET /api/endpoint" + i + " HTTP/1.1"));
            }
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        // Should return top 5 by default
        assertTrue(data.size() <= 5);
    }

    @Test
    void testSingleEndpoint() {
        for (int i = 0; i < 5; i++) {
            entries.add(createEntry("GET /api/test HTTP/1.1"));
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(1, data.size());
        assertEquals(5L, data.get("/api/test"));
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("Top 5 endpoints"));
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Top 5 Endpoints Analyzer", analyzer.getAnalyzerName());
    }

    private AbstractLogEntry createEntry(String requestType) {
        return new ApacheLogEntry(
            "127.0.0.1",
            "user",
            ZonedDateTime.now(),
            requestType,
            "/test",
            HttpStatus.OK,
            1024
        );
    }
}
