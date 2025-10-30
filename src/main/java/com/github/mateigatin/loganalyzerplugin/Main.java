package com.github.mateigatin.loganalyzerplugin;


import lombok.extern.slf4j.Slf4j;
import com.github.mateigatin.loganalyzerplugin.analyzer.*;
import com.github.mateigatin.loganalyzerplugin.cli.CliArguments;
import com.github.mateigatin.loganalyzerplugin.exporter.JsonExporter;
import com.github.mateigatin.loganalyzerplugin.exporter.ResultExporter;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.parser.ApacheLogParser;
import com.github.mateigatin.loganalyzerplugin.parser.LogParser;
import com.github.mateigatin.loganalyzerplugin.stream.LogStreamProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Main {
    public static void main(String[] args) {
        CliArguments cliArgs = CliArguments.parse(args);

        if (cliArgs.isStreamMode()) {
            runStreamMode(cliArgs);
        } else {
            runBatchMode(cliArgs);
        }
    }

    private static void runBatchMode(CliArguments cliArgs) {
        try {
            log.info("=== Starting Log Analysis (Batch Mode) ===");
            log.info("Analyzing file: {}", cliArgs.getInputFile());

            // Validate input file
            if (!Files.exists(cliArgs.getInputFile())) {
                log.error("File not found: {}", cliArgs.getInputFile());
                System.exit(1);
            }

            // Create output directory if needed
            if (!Files.exists(cliArgs.getOutputDirectory())) {
                Files.createDirectories(cliArgs.getOutputDirectory());
            }

            // Step 1: Parse the log file
            LogParser parser = new ApacheLogParser(cliArgs.getInputFile().toString());
            List<AbstractLogEntry> entries = parser.parse();
            log.info("Successfully parsed {} log entries", entries.size());

            if (entries.isEmpty()) {
                log.warn("No log entries found. Exiting.");
                return;
            }

            // Step 2: Run all analyzers
            List<LogAnalyzer> analyzers = List.of(
                    new TotalRequestAnalyzer(),
                    new StatusCodeAnalyzer(),
                    new TopEndpointsAnalyzer(),
                    new TrafficByHourAnalyzer(),
                    new PerformanceAnalyzer(),
                    new SecurityAnalyzer()
            );

            List<AnalysisResult> allResults = new ArrayList<>();

            log.info("\n=== Running Analysis ===");
            for (LogAnalyzer analyzer : analyzers) {
                log.info("Running: {}", analyzer.getAnalyzerName());
                AnalysisResult result = analyzer.analyze(entries);
                allResults.add(result);

                // Print summary to console
                System.out.println("\n" + "=".repeat(60));
                System.out.println(result.getSummary());
                System.out.println("=".repeat(60));

                // Export individual text report
                String filename = "analysis_" +
                        analyzer.getAnalyzerName().toLowerCase().replace(" ", "_") + ".txt";
                Path outputFile = cliArgs.getOutputDirectory().resolve(filename);
                result.createAnalysis(outputFile.toString());
                log.info("Saved report to: {}", outputFile);
            }

            // Step 3: Export combined JSON report (if enabled)
            if (cliArgs.isJsonExport()) {
                log.info("\n=== Exporting Combined Report ===");
                ResultExporter jsonExporter = new JsonExporter();
                Path jsonOutputPath = cliArgs.getOutputDirectory().resolve("analysis_report.json");
                jsonExporter.export(allResults, jsonOutputPath);
                log.info("Combined JSON report saved to: {}", jsonOutputPath);
            }

            // Step 4: Print summary statistics
            printSummary(entries, allResults);

            log.info("\n=== Analysis Complete ===");
            log.info("Generated {} individual reports", analyzers.size());
            log.info("Check {} for output files", cliArgs.getOutputDirectory());

        } catch (Exception e) {
            log.error("Fatal error during log analysis", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runStreamMode(CliArguments cliArgs) {
        log.info("=== Starting Real-Time Log Stream Monitor ===");
        log.info("Monitoring: {}", cliArgs.getInputFile());
        log.info("Press Ctrl+C to stop");
        System.out.println("\n" + "=".repeat(60));

        LogParser parser = new ApacheLogParser(cliArgs.getInputFile().toString());

        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger suspiciousCount = new AtomicInteger(0);

        try (LogStreamProcessor processor = new LogStreamProcessor(cliArgs.getInputFile(), parser)) {

            processor.startMonitoring(entry -> {
                int count = requestCount.incrementAndGet();

                System.out.printf("[%s] %s %s %s - Status: %d - Size: %d bytes%n",
                        entry.getTimeStamp().toLocalTime(),
                        entry.getIpAddress(),
                        entry.getRequestType(),
                        entry.getEndpoint(),
                        entry.getHttpStatusCode().getCode(),
                        entry.getResponseSize()
                );

                if (entry.isClientError() || entry.isServerError()) {
                    errorCount.incrementAndGet();
                    System.out.println("  ⚠️  ERROR detected!");
                }

                String endpoint = entry.getEndpoint().toLowerCase();
                if (endpoint.contains("admin") || endpoint.contains("..") ||
                        endpoint.contains("wp-") || endpoint.contains("config")) {
                    suspiciousCount.incrementAndGet();
                    System.out.println("  🚨 SUSPICIOUS request detected!");
                }

                if (count % 10 == 0) {
                    printStreamStats(count, errorCount.get(), suspiciousCount.get());
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Shutting down...");
                printStreamStats(requestCount.get(), errorCount.get(), suspiciousCount.get());
            }));

            while (processor.isRunning()) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            log.info("Stream monitoring interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during stream processing", e);
        }
    }

    private static void printSummary(List<AbstractLogEntry> entries, List<AnalysisResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("OVERALL SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Total Log Entries Processed: " + entries.size());
        System.out.println("Total Analyzers Run: " + results.size());
        System.out.println("Reports Generated:");
        results.forEach(r -> System.out.println("  - " + r.getAnalyzerName()));
        System.out.println("=".repeat(60));
    }

    private static void printStreamStats(int total, int errors, int suspicious) {
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("📊 Stats - Total: %d | Errors: %d | Suspicious: %d%n",
                total, errors, suspicious);
        System.out.println("-".repeat(60) + "\n");
    }
}