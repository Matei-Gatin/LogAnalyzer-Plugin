package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.PerformanceAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceAnalyzerTest {

    private PerformanceAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new PerformanceAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testPerformanceMetrics() {
        entries.add(createEntry("/api/users", 1024));
        entries.add(createEntry("/api/products", 2048));
        entries.add(createEntry("/api/orders", 512));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertNotNull(data.get("averageResponseSize"));
        assertNotNull(data.get("maxResponseSize"));
        assertNotNull(data.get("minResponseSize"));
        assertNotNull(data.get("totalDataTransferred"));

        assertEquals(2048.0, data.get("maxResponseSize"));
        assertEquals(512.0, data.get("minResponseSize"));
    }

    @Test
    void testAverageResponseSize() {
        entries.add(createEntry("/api/test", 1000));
        entries.add(createEntry("/api/test", 2000));
        entries.add(createEntry("/api/test", 3000));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        double average = (Double) data.get("averageResponseSize");
        assertEquals(2000.0, average, 0.01);
    }

    @Test
    void testLargestEndpoints() {
        entries.add(createEntry("/api/large", 10000));
        entries.add(createEntry("/api/large", 10000));
        entries.add(createEntry("/api/small", 100));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        @SuppressWarnings("unchecked")
        Map<String, Long> largestEndpoints = (Map<String, Long>) data.get("largestEndpoints");

        assertNotNull(largestEndpoints);
        assertEquals(20000L, largestEndpoints.get("/api/large"));
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);

        assertNotNull(result);
        assertNotNull(result.getSummary());
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Performance Analyzer", analyzer.getAnalyzerName());
    }

    @Test
    void testSingleEntry() {
        entries.add(createEntry("/api/test", 5000));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(5000.0, data.get("averageResponseSize"));
        assertEquals(5000.0, data.get("maxResponseSize"));
        assertEquals(5000.0, data.get("minResponseSize"));
    }

    @Test
    void testZeroResponseSize() {
        entries.add(createEntry("/api/test", 0));
        entries.add(createEntry("/api/test2", 0));

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(0.0, data.get("averageResponseSize"));
        assertEquals(0.0, data.get("maxResponseSize"));
        assertEquals(0.0, data.get("minResponseSize"));
    }

    private AbstractLogEntry createEntry(String endpoint, long responseSize) {
        return new ApacheLogEntry(
            "127.0.0.1",
            "user",
            ZonedDateTime.now(),
            "GET",
            endpoint,
            HttpStatus.OK,
            responseSize
        );
    }
}
