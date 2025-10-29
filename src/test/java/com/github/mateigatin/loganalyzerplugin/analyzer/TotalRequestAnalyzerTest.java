package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.TotalRequestAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotalRequestAnalyzerTest {

    private TotalRequestAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new TotalRequestAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testTotalRequestCount() {
        for (int i = 0; i < 10; i++) {
            entries.add(createEntry());
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(10, data.get("Total"));
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(0, data.get("Total"));
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Total Request Analyzer", analyzer.getAnalyzerName());
    }

    @Test
    void testSummaryContainsTotalCount() {
        entries.add(createEntry());
        entries.add(createEntry());
        entries.add(createEntry());

        AnalysisResult result = analyzer.analyze(entries);

        assertTrue(result.getSummary().contains("3"));
        assertTrue(result.getSummary().toLowerCase().contains("total"));
    }

    private AbstractLogEntry createEntry() {
        return new ApacheLogEntry(
            "127.0.0.1",
            "user",
            ZonedDateTime.now(),
            "GET",
            "/test",
            HttpStatus.OK,
            1024
        );
    }
}
