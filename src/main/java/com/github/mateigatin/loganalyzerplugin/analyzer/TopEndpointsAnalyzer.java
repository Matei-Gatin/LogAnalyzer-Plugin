package com.github.mateigatin.loganalyzerplugin.analyzer;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopEndpointsAnalyzer implements LogAnalyzer {

    @Override
    public AnalysisResult analyze(List<AbstractLogEntry> entries)
    {
        Map<String, Object> mapOfValues = entries.stream()
                .collect(Collectors.groupingBy(s -> {
                    String[] parts = s.getRequestType().split(" ");
                    return parts.length > 1 ? parts[1] : parts[0];
                }, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String summary = "Top 5 endpoints average requests is: " + mapOfValues.values().stream()
                .mapToLong(o -> (long) o)
                .average()
                .orElse(0.0) + " requests";

        return new AnalysisResult(getAnalyzerName(), mapOfValues, summary);
    }

    @Override
    public String getAnalyzerName()
    {
        return "Top 5 Endpoints Analyzer";
    }
}
