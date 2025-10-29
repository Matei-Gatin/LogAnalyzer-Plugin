package com.github.mateigatin.loganalyzerplugin.analyzer;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatusCodeAnalyzer implements LogAnalyzer {

    @Override
    public AnalysisResult analyze(List<AbstractLogEntry> entries) {
        Map<Integer, Long> counts = entries.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getHttpStatusCode().getCode(),
                        Collectors.counting())
                );

        Map<String, Object> data = new HashMap<>();
        counts.forEach((code, count) -> data.put(String.valueOf(code), count));

        String summary = "Status code distribution: " + counts.size() + " unique codes.";

        return new AnalysisResult(getAnalyzerName(), data, summary);
    }

    @Override
    public String getAnalyzerName()
    {
        return "Status Code Analyzer";
    }
}
