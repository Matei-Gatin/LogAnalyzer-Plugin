package com.github.mateigatin.loganalyzerplugin.analyzer;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrafficByHourAnalyzer implements LogAnalyzer
{
    @Override
    public AnalysisResult analyze(List<AbstractLogEntry> entries) {
        Map<String, Long> hourlyTraffic = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getTimeStamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")),
                        Collectors.counting()
                ));

        String summary = String.format("Peak traffic hour: %s with %d requests",
                hourlyTraffic.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("N/A"),
                hourlyTraffic.values().stream()
                        .mapToLong(o -> o)
                        .max()
                        .orElse(0L));
        //
        Map<String, Object> hourlyTrafficConverter = new HashMap<>(hourlyTraffic);

        return new AnalysisResult(getAnalyzerName(), hourlyTrafficConverter, summary);
    }

    @Override
    public String getAnalyzerName() {
        return "Traffic";
    }
}
