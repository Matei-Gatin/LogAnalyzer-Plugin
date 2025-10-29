package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

public class PerformanceAnalyzer implements LogAnalyzer {

    @Override
    public AnalysisResult analyze(List<AbstractLogEntry> entries) {
        DoubleSummaryStatistics sizeStats = entries.stream()
                .mapToDouble(AbstractLogEntry::getResponseSize)
                .summaryStatistics();

        Map<String, Long> endpointSizes = entries.stream()
                .collect(Collectors.groupingBy(
                        AbstractLogEntry::getEndpoint,
                        Collectors.summingLong(AbstractLogEntry::getResponseSize)
                ));

        Map<String, Object> data = Map.of(
                "averageResponseSize", sizeStats.getAverage(),
                "maxResponseSize", sizeStats.getMax(),
                "minResponseSize", sizeStats.getMin(),
                "totalDataTransferred", sizeStats.getSum(),
                "largestEndpoints", endpointSizes.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        String summary = String.format(
                "Performance Summary:\n" +
                "\tAverage Response Size: %.2f bytes\n" +
                "\tTotal Data Transferred: %.2f MB\n" +
                "\tLargest Response: %.2f KB",
                sizeStats.getAverage(),
                sizeStats.getSum() / (1024.0 * 1024.0),
                sizeStats.getMax() / 1024.0
        );

        return new AnalysisResult(getAnalyzerName(), data, summary);
    }

    @Override
    public String getAnalyzerName() {
        return "Performance Analyzer";
    }
}
