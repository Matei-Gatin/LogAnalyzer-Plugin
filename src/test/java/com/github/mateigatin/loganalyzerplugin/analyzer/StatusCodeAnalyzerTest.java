package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.StatusCodeAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusCodeAnalyzerTest {

    private StatusCodeAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new StatusCodeAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testStatusCodeDistribution() {
        entries.add(createEntry(HttpStatus.OK));
        entries.add(createEntry(HttpStatus.OK));
        entries.add(createEntry(HttpStatus.NOT_FOUND));
        entries.add(createEntry(HttpStatus.INTERNAL_SERVER_ERROR));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(2L, data.get("200"));
        assertEquals(1L, data.get("404"));
        assertEquals(1L, data.get("500"));
    }

    @Test
    void testMostCommonStatus() {
        for (int i = 0; i < 10; i++) {
            entries.add(createEntry(HttpStatus.OK));
        }
        entries.add(createEntry(HttpStatus.NOT_FOUND));

        AnalysisResult result = analyzer.analyze(entries);

        assertTrue(result.getSummary().contains("Status code distribution"));
    }

    @Test
    void testMultipleDifferentStatusCodes() {
        entries.add(createEntry(HttpStatus.OK));
        entries.add(createEntry(HttpStatus.CREATED));
        entries.add(createEntry(HttpStatus.BAD_REQUEST));
        entries.add(createEntry(HttpStatus.UNAUTHORIZED));
        entries.add(createEntry(HttpStatus.FORBIDDEN));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(5, data.size());
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);

        assertNotNull(result);
        assertNotNull(result.getSummary());
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Status Code Analyzer", analyzer.getAnalyzerName());
    }

    private AbstractLogEntry createEntry(HttpStatus status) {
        return new ApacheLogEntry(
            "127.0.0.1",
            "user",
            ZonedDateTime.now(),
            "GET",
            "/test",
            status,
            1024
        );
    }
}
