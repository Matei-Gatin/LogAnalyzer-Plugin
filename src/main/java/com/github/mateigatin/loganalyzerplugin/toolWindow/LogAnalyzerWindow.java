package com.github.mateigatin.loganalyzerplugin.toolWindow;

import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;


import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogAnalyzerWindow
{

    // Key to store this window in the project's user data
    public static final Key<LogAnalyzerWindow> KEY = Key.create("LogAnalyzerWindow");

    private final Project project;
    private final JPanel mainPanel;
    private final JBTabbedPane tabbedPane;
    private final JLabel statusLabel;

    // Store text areas for each tab so we can update them
    private JBTextArea overviewTextArea;
    private JBTextArea trafficTextArea;
    private JBTextArea statusCodesTextArea;
    private JBTextArea endpointsTextArea;
    private JBTextArea performanceTextArea;
    private JBTextArea securityTextArea;

    public LogAnalyzerWindow(Project project)
    {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.tabbedPane = new JBTabbedPane();
        this.statusLabel = new JLabel("Ready to analyze logs");

        // Store this window instance in the project
        project.putUserData(KEY, this);

        setupUI();
    }

    private void setupUI()
    {
        // Create text areas
        overviewTextArea = new JBTextArea();
        trafficTextArea = new JBTextArea();
        statusCodesTextArea = new JBTextArea();
        endpointsTextArea = new JBTextArea();
        performanceTextArea = new JBTextArea();
        securityTextArea = new JBTextArea();

        // Make them all non-editable
        overviewTextArea.setEditable(false);
        trafficTextArea.setEditable(false);
        statusCodesTextArea.setEditable(false);
        endpointsTextArea.setEditable(false);
        performanceTextArea.setEditable(false);
        securityTextArea.setEditable(false);

        // Set initial text
        overviewTextArea.setText("📊 Log Analyzer\n\nRight-click on a .log file and select 'Analyze Log File' to begin.\n\nSupported formats:\n- Apache Combined Log Format\n- Nginx Access Logs");
        trafficTextArea.setText("No data yet. Analyze a log file to see traffic patterns.");
        statusCodesTextArea.setText("No data yet. Analyze a log file to see status code distribution.");
        endpointsTextArea.setText("No data yet. Analyze a log file to see top endpoints.");
        performanceTextArea.setText("No data yet. Analyze a log file to see performance metrics.");
        securityTextArea.setText("No data yet. Analyze a log file to see security analysis.");

        // Add tabs with scrollable text areas
        tabbedPane.addTab("📊 Overview", createScrollablePanel(overviewTextArea));
        tabbedPane.addTab("📈 Traffic by Hour", createScrollablePanel(trafficTextArea));
        tabbedPane.addTab("🔢 Status Codes", createScrollablePanel(statusCodesTextArea));
        tabbedPane.addTab("🎯 Top Endpoints", createScrollablePanel(endpointsTextArea));
        tabbedPane.addTab("⚡ Performance", createScrollablePanel(performanceTextArea));
        tabbedPane.addTab("🔒 Security", createScrollablePanel(securityTextArea));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createScrollablePanel(JBTextArea textArea)
    {
        JPanel panel = new JPanel(new BorderLayout());
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JBScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    public JPanel getContent()
    {
        return mainPanel;
    }

    public void updateStatus(String message)
    {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    // Method to update all results at once
    public void displayResults(
            AnalysisResult totalResult,
            AnalysisResult trafficResult,
            AnalysisResult statusResult,
            AnalysisResult endpointsResult,
            AnalysisResult performanceResult,
            AnalysisResult securityResult
    )
    {
        SwingUtilities.invokeLater(() -> {
            // Update Overview
            updateOverview(totalResult);

            // Update Traffic by Hour
            updateTraffic(trafficResult);

            // Update Status Codes
            updateStatusCodes(statusResult);

            // Update Top Endpoints
            updateEndpoints(endpointsResult);

            // Update Performance
            updatePerformance(performanceResult);

            // Update Security
            updateSecurity(securityResult);

            updateStatus("✅ Analysis complete!");
        });
    }

    private void updateOverview(AnalysisResult totalResult)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 LOG ANALYSIS OVERVIEW\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = totalResult.getData();

        if (data.containsKey("Total"))
        {
            sb.append("Total Requests: ").append(data.get("Total")).append("\n");
        } else
        {
            sb.append("Total Requests: 0\n");
        }

        sb.append("Analysis Time: ").append(java.time.LocalDateTime.now()).append("\n\n");

        sb.append("🔍 Quick Summary:\n");
        sb.append("├─ Check 'Traffic by Hour' for request patterns\n");
        sb.append("├─ Check 'Status Codes' for error rates\n");
        sb.append("├─ Check 'Top Endpoints' for most accessed URLs\n");
        sb.append("├─ Check 'Performance' for response times\n");
        sb.append("└─ Check 'Security' for suspicious activity\n");

        overviewTextArea.setText(sb.toString());
    }

    private void updateTraffic(AnalysisResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("📈 TRAFFIC BY HOUR\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        Map<Integer, Long> hourlyTraffic = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet())
        {
            String dateTime = entry.getKey();
            Long count = (Long) entry.getValue();

            // Extract hour from "yyyy-MM-dd HH:00" format
            try
            {
                String hourStr = dateTime.split(" ")[1].split(":")[0];
                int hour = Integer.parseInt(hourStr);
                hourlyTraffic.merge(hour, count, Long::sum);
            } catch (Exception e)
            {
                // Skip invalid entries
            }
        }

        if (!hourlyTraffic.isEmpty())
        {
            // Find max for scaling
            long max = hourlyTraffic.values().stream().max(Long::compare).orElse(1L);

            hourlyTraffic.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int hour = entry.getKey();
                        long count = entry.getValue();

                        // Create bar chart with chars
                        int barLength = (int) ((count * 50.0) / max);
                        String bar = "█".repeat(Math.max(0, barLength));

                        sb.append(String.format("%02d:00 | %-50s %d\n", hour, bar, count));
                    });
        } else
        {
            sb.append("No traffic data available.\n");
        }

        trafficTextArea.setText(sb.toString());
    }

    private void updateStatusCodes(AnalysisResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("🔢 HTTP STATUS CODE DISTRIBUTION\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        // Convert string keys to integers
        Map<Integer, Long> statusCodes = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                int code = Integer.parseInt(entry.getKey());
                long count = (Long) entry.getValue();
                statusCodes.put(code, count);
            } catch (NumberFormatException e) {
                // Skip non-numeric keys
            }
        }

        if (!statusCodes.isEmpty())
        {
            long total = statusCodes.values().stream().mapToLong(Long::longValue).sum();

            // Group by category
            long success = statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 200 && e.getKey() < 300)
                    .mapToLong(Map.Entry::getValue).sum();

            long redirect = statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 300 && e.getKey() < 400)
                    .mapToLong(Map.Entry::getValue).sum();

            long clientError = statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 400 && e.getKey() < 500)
                    .mapToLong(Map.Entry::getValue).sum();

            long serverError = statusCodes.entrySet().stream()
                    .filter(e -> e.getKey() >= 500 && e.getKey() < 600)
                    .mapToLong(Map.Entry::getValue).sum();

            sb.append("SUMMARY:\n");
            sb.append(String.format("✓ 2xx Success:      %6d (%.1f%%)\n", success, 100.0 * success / total));
            sb.append(String.format("↪ 3xx Redirect:     %6d (%.1f%%)\n", redirect, 100.0 * redirect / total));
            sb.append(String.format("✗ 4xx Client Error: %6d (%.1f%%)\n", clientError, 100.0 * clientError / total));
            sb.append(String.format("⚠ 5xx Server Error: %6d (%.1f%%)\n", serverError, 100.0 * serverError / total));

            sb.append("\n\nDETAILED BREAKDOWN:\n");
            statusCodes.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String emoji = getStatusEmoji(entry.getKey());
                        double percentage = 100.0 * entry.getValue() / total;
                        sb.append(String.format("%s %d: %6d requests (%.1f%%)\n",
                                emoji, entry.getKey(), entry.getValue(), percentage));
                    });
        } else {
            sb.append("No status code data available.\n");
        }

        statusCodesTextArea.setText(sb.toString());
    }

    private void updateEndpoints(AnalysisResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 TOP ENDPOINTS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        // The analyzer returns endpoint URLs directly as keys
        if (!data.isEmpty())
        {
            long total = data.values().stream()
                    .mapToLong(v -> (Long) v)
                    .sum();

            int rank = 1;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Long count = (Long) entry.getValue();
                double percentage = 100.0 * count / total;
                sb.append(String.format("#%d  %s\n", rank++, entry.getKey()));
                sb.append(String.format("    └─ %d requests (%.1f%%)\n\n", count, percentage));
            }
        } else
        {
            sb.append("No endpoint data available.\n");
        }

        endpointsTextArea.setText(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private void updatePerformance(AnalysisResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("⚡ PERFORMANCE METRICS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        sb.append("Response Size Statistics:\n\n");

        if (data.containsKey("averageResponseSize"))
        {
            double avgBytes = (Double) data.get("averageResponseSize");
            sb.append(String.format("Average: %.2f KB\n", avgBytes / 1024.0));
        }

        if (data.containsKey("minResponseSize"))
        {
            double minBytes = (Double) data.get("minResponseSize");
            sb.append(String.format("Minimum: %.2f KB\n", minBytes / 1024.0));
        }

        if (data.containsKey("maxResponseSize"))
        {
            double maxBytes = (Double) data.get("minResponseSize");
            sb.append(String.format("Minimum: %.2f KB\n", maxBytes / 1024.0));
        }

        if (data.containsKey("totalDataTransferred"))
        {
            double totalBytes = (Double) data.get("totalDataTransferred");
            sb.append(String.format("Total Transferred: %.2f MB\n", totalBytes / (1024.0 * 1024.0)));
        }

        if (data.containsKey("largestEndpoints"))
        {

            Map<String, Long> largest = (Map<String, Long>) data.get("largestEndpoints");

            if (!largest.isEmpty()) {
                sb.append("\n\n📦 LARGEST ENDPOINTS (by data transferred):\n");

                int rank = 1;
                for (Map.Entry<String, Long> entry : largest.entrySet()) {
                    sb.append(String.format("#%d  %s\n", rank++, entry.getKey()));
                    sb.append(String.format("    └─ %.2f KB transferred\n\n", entry.getValue() / 1024.0));
                }
            }
        }

        performanceTextArea.setText(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private void updateSecurity(AnalysisResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("🔒 SECURITY ANALYSIS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        if (data.containsKey("suspiciousRequestCount"))
        {
            int suspiciousCount = (Integer) data.get("suspiciousRequestCount");
            sb.append(String.format("⚠️  Suspicious Requests Detected: %d\n\n", suspiciousCount));
        }

        if (data.containsKey("potentialAttackers"))
        {
            List<String> attackers = (List<String>) data.get("potentialAttackers");

            if (!attackers.isEmpty())
            {
                sb.append("🚨 POTENTIAL ATTACKERS (>10 failed auth attempts):\n\n");
                for (String ip : attackers) {
                    sb.append(String.format("   • %s\n", ip));
                }
                sb.append("\n");
            } else {
                sb.append("✅ No potential attackers detected (brute force threshold: 10 attempts)\n\n");
            }
        }

        if (data.containsKey("unauthorizedAttempts"))
        {
            int unauthorizedCount = (Integer) data.get("unauthorizedAttempts");
            sb.append(String.format("Failed Authentication Attempts: %d unique IPs\n\n", unauthorizedCount));
        }

        if (data.containsKey("topOffendingIPs"))
        {
            Map<String, Long> topOffenders = (Map<String, Long>) data.get("topOffendingIPs");

            if (!topOffenders.isEmpty())
            {
                sb.append("🎯 TOP OFFENDING IPs (401/403 errors):\n\n");

                int rank = 1;
                for (Map.Entry<String, Long> entry : topOffenders.entrySet()) {
                    sb.append(String.format("#%d  %s\n", rank++, entry.getKey()));
                    sb.append(String.format("    └─ %d failed attempts\n\n", entry.getValue()));
                }
            }
        }

        securityTextArea.setText(sb.toString());
    }

    private String getStatusEmoji(int statusCode)
    {
        if (statusCode >= 200 && statusCode < 300) return "✓";
        if (statusCode >= 300 && statusCode < 400) return "↪";
        if (statusCode >= 400 && statusCode < 500) return "✗";
        if (statusCode >= 500) return "⚠";
        return "?";
    }
}
