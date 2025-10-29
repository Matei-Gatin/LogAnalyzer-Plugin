package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mateigatin.loganalyzerplugin.analyzer.TrafficByHourAnalyzer;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrafficByHourAnalyzerTest {

    private TrafficByHourAnalyzer analyzer;
    private List<AbstractLogEntry> entries;

    @BeforeEach
    void setUp() {
        analyzer = new TrafficByHourAnalyzer();
        entries = new ArrayList<>();
    }

    @Test
    void testTrafficGroupedByHour() {
        ZonedDateTime hour1 = ZonedDateTime.now().withHour(10).withMinute(0);
        ZonedDateTime hour2 = ZonedDateTime.now().withHour(11).withMinute(0);

        // Add 5 entries for hour 10
        for (int i = 0; i < 5; i++) {
            entries.add(createEntry(hour1.plusMinutes(i * 10)));
        }

        // Add 3 entries for hour 11
        for (int i = 0; i < 3; i++) {
            entries.add(createEntry(hour2.plusMinutes(i * 10)));
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertNotNull(data);
        assertTrue(data.size() >= 2);
    }

    @Test
    void testPeakTrafficHour() {
        ZonedDateTime peakHour = ZonedDateTime.now().withHour(14).withMinute(0);
        ZonedDateTime normalHour = ZonedDateTime.now().withHour(15).withMinute(0);

        // Add 10 entries for peak hour
        for (int i = 0; i < 10; i++) {
            entries.add(createEntry(peakHour.plusMinutes(i * 5)));
        }

        // Add 2 entries for normal hour
        for (int i = 0; i < 2; i++) {
            entries.add(createEntry(normalHour.plusMinutes(i * 5)));
        }

        AnalysisResult result = analyzer.analyze(entries);
        String summary = result.getSummary();

        assertTrue(summary.contains("Peak traffic hour"));
        assertTrue(summary.contains("10"));
    }

    @Test
    void testEmptyEntries() {
        AnalysisResult result = analyzer.analyze(entries);

        assertNotNull(result);
        assertNotNull(result.getSummary());
    }

    @Test
    void testAnalyzerName() {
        assertEquals("Traffic", analyzer.getAnalyzerName());
    }

    @Test
    void testSingleHour() {
        ZonedDateTime singleHour = ZonedDateTime.now().withHour(12).withMinute(0);

        for (int i = 0; i < 5; i++) {
            entries.add(createEntry(singleHour.plusMinutes(i * 5)));
        }

        AnalysisResult result = analyzer.analyze(entries);
        Map<String, Object> data = result.getData();

        assertEquals(1, data.size());
    }

    private AbstractLogEntry createEntry(ZonedDateTime timestamp) {
        return new ApacheLogEntry(
            "127.0.0.1",
            "user",
            timestamp,
            "GET",
            "/test",
            HttpStatus.OK,
            1024
        );
    }
}
