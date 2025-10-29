package com.github.mateigatin.loganalyzerplugin.analyzer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

public class SecurityAnalyzer implements LogAnalyzer {

    private static final List<String> SUSPICIOUS_PATTERNS = List.of(
            "/admin", "/wp-admin", "/.env", "/config", "SELECT",
            "UNION", "../", "script>", "<?php", "CASCADE"
    );

    @Override
    public AnalysisResult analyze(List<AbstractLogEntry> entries)
    {
        // Find suspicious requests
        List<AbstractLogEntry> suspiciousRequests = entries.stream()
                .filter(this::isSuspicious)
                .toList();

        // Find IP addresses with most 401/403 errors
        Map<String, Long> unauthorizedIPs = entries.stream()
                .filter(e -> e.getHttpStatusCode().getCode() == 401 ||
                        e.getHttpStatusCode().getCode() == 403)
                .collect(Collectors.groupingBy(
                        AbstractLogEntry::getIpAddress,
                        Collectors.counting()
                ));
        // Find potential brute force attempts (same IP, multiple failures)
        List<String> potentialAttackers = unauthorizedIPs.entrySet().stream()
                .filter(e -> e.getValue() > 10)
                .map(Map.Entry::getKey)
                .toList();
        // final map
        Map<String, Object> data = Map.of(
                "suspiciousRequestCount", suspiciousRequests.size(),
                "potentialAttackers", potentialAttackers,
                "unauthorizedAttempts", unauthorizedIPs.size(),
                "topOffendingIPs", unauthorizedIPs.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        String summary = String.format(
                "Security Analysis:\n" +
                "\tSuspicious Requests: %d\n" +
                "\tPotential Attackers: %d\n" +
                "\tFailed Auth Attempts: %d",
                suspiciousRequests.size(),
                potentialAttackers.size(),
                unauthorizedIPs.values().stream().mapToLong(Long::longValue).sum()
        );

        return new AnalysisResult(getAnalyzerName(), data, summary);

    }

    private boolean isSuspicious(AbstractLogEntry entry)
    {
        String request = entry.getRequestType() + " " + entry.getEndpoint();
        return SUSPICIOUS_PATTERNS.stream()
                .anyMatch(pattern -> request.toLowerCase().contains(pattern.toLowerCase()));
    }

    @Override
    public String getAnalyzerName() {
        return "Security Analyzer";
    }
}
