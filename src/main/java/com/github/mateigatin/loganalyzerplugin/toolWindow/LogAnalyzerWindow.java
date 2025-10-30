package com.github.mateigatin.loganalyzerplugin.toolWindow;

import com.github.mateigatin.loganalyzerplugin.export.ExportService;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LogAnalyzerWindow
{

    // Key to store this window in the project's user data
    public static final Key<LogAnalyzerWindow> KEY = Key.create("LogAnalyzerWindow");

    // Store text areas for each tab so we can update them
    private final JPanel mainPanel;
    private JTextArea overviewTextArea;
    private JTextArea trafficTextArea;
    private JTextArea statusCodesTextArea;
    private JTextArea endpointsTextArea;
    private JTextArea performanceTextArea;
    private JTextArea securityTextArea;
    private final Project project;

    private Map<String, AnalysisResult> currentResults = new HashMap<>();
    private final ExportService exportService = new ExportService();

    public LogAnalyzerWindow(Project project)
    {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());

        // toolbar with export button
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Tabbed pane
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // Text areas for each tab
        overviewTextArea = createTextArea();
        trafficTextArea = createTextArea();
        statusCodesTextArea = createTextArea();
        endpointsTextArea = createTextArea();
        performanceTextArea = createTextArea();
        securityTextArea = createTextArea();

        // Add tabs with icons
        tabbedPane.addTab("Overview", new JBScrollPane(overviewTextArea));
        tabbedPane.addTab("Traffic by Hour", new JBScrollPane(trafficTextArea));
        tabbedPane.addTab("Status Codes", new JBScrollPane(statusCodesTextArea));
        tabbedPane.addTab("Top Endpoints", new JBScrollPane(endpointsTextArea));
        tabbedPane.addTab("Performance", new JBScrollPane(performanceTextArea));
        tabbedPane.addTab("Security", new JBScrollPane(securityTextArea));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Show welcome message
        showWelcomeMessage();
    }

    private JPanel createToolbar()
    {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // Export button
        JButton exportButton = new JButton("📤 Export Report");
        exportButton.setToolTipText("Export analysis results to HTML or JSON");
        exportButton.addActionListener(e -> showExportDialog());

        // Clear button
        JButton clearButton = new JButton("🗑️ Clear");
        clearButton.setToolTipText("Clear current analysis");
        clearButton.addActionListener(e -> clearResults());

        toolbar.add(exportButton);
        toolbar.add(clearButton);

        return toolbar;
    }

    private void showExportDialog() {
        if (currentResults.isEmpty()) {
            Messages.showWarningDialog(
                    project,
                    "No analysis results to export. Please analyze a log file first.",
                    "No Data Available"
            );
            return;
        }

        // Create dialog with format selection
        String[] options = {"HTML Report", "JSON Data", "Cancel"};
        int choice = Messages.showDialog(
                project,
                "Choose export format:",
                "Export Analysis Results",
                options,
                0,
                Messages.getQuestionIcon()
        );

        if (choice == 2) return; // User cancelled

        boolean exportAsHtml = (choice == 0);
        String extension = exportAsHtml ? "html" : "json";
        String defaultName = "log-analysis-report." + extension;

        // Show file chooser
        FileSaverDescriptor descriptor =
                new FileSaverDescriptor(
                        "Export Analysis Report",
                        "Choose location to save the report",
                        extension
                );

        FileSaverDialog dialog = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, project);
        // --> Check for method
        VirtualFileWrapper fileWrapper = dialog.save((VirtualFile) null, defaultName);

        if (fileWrapper ==  null) return;

        Path outputPath = fileWrapper.getFile().toPath();

        // Export in background
        try {
            if (exportAsHtml) {
                exportService.exportToHtml(currentResults, outputPath);
            } else {
                exportService.exportToJson(currentResults, outputPath);
            }

            // Show success notification
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("LogAnalyzer.Notifications")
                    .createNotification(
                            "Export Successful",
                            "Report exported to: " + outputPath,
                            NotificationType.INFORMATION
                    )
                    .notify(project);

            // Ask if user wants to open the file
            int openChoice = Messages.showYesNoDialog(
                    project,
                    "Export successful! Would you like to open the file?",
                    "Export Complete",
                    Messages.getQuestionIcon()
            );

            if (openChoice == Messages.YES) {
                openFileInBrowser(outputPath);
            }

        } catch (IOException ex) {
            Messages.showErrorDialog(
                    project,
                    "Failed to export: " + ex.getMessage(),
                    "Export Error"
            );
            ex.printStackTrace();
        }
    }

    private void openFileInBrowser(Path filePath) {
        try {
            Desktop.getDesktop().browse(filePath.toUri());
        } catch (Exception ex) {
            // Fallback: show the path
            Messages.showInfoMessage(
                    project,
                    "File saved at: " + filePath,
                    "Export Complete"
            );
        }
    }

    private void clearResults() {
        int choice = Messages.showYesNoDialog(
                project,
                "Are you sure you want to clear the current analysis?",
                "Clear Analysis",
                Messages.getQuestionIcon()
        );

        if (choice == Messages.YES) {
            currentResults.clear();
            showWelcomeMessage();

            NotificationGroupManager.getInstance()
                    .getNotificationGroup("LogAnalyzer.Notifications")
                    .createNotification(
                            "Analysis Cleared",
                            "Ready to analyze a new log file.",
                            NotificationType.INFORMATION
                    )
                    .notify(project);
        }
    }

    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(JBUI.insets(10));
        return textArea;
    }


    private void showWelcomeMessage()
    {
        String welcome = """
            ╔════════════════════════════════════════════════════════════╗
            ║                  📊 LogAnalyzer Plugin                     ║
            ╚════════════════════════════════════════════════════════════╝
            
            Welcome! Ready to analyze your log files.
            
            🚀 Quick Start:
            1. Right-click on a .log file in your project
            2. Select 'Analyze Log File' from the context menu
            3. View detailed analysis in the tabs above
            4. Export results using the 📤 Export button
            
            📋 Features:
            • Traffic patterns by hour
            • HTTP status code distribution
            • Top accessed endpoints
            • Performance metrics
            • Security threat detection
            
            💡 Tip: You can export results as HTML (visual report)
                    or JSON (for further processing).
            
            Ready to analyze logs! 🔍
            """;

        overviewTextArea.setText(welcome);
        trafficTextArea.setText("No data yet. Analyze a log file to see traffic patterns.");
        statusCodesTextArea.setText("No data yet. Analyze a log file to see status code distribution.");
        endpointsTextArea.setText("No data yet. Analyze a log file to see top endpoints.");
        performanceTextArea.setText("No data yet. Analyze a log file to see performance metrics.");
        securityTextArea.setText("No data yet. Analyze a log file to see security analysis.");
    }


    public JPanel getContent()
    {
        return mainPanel;
    }

    // ---------------------

    // Method to update all results at once
    public void displayResults(Map<String, AnalysisResult> results) {
        // Store results for export
        this.currentResults = new HashMap<>(results);

        // Display in each tab
        if (results.containsKey("total")) {
            updateOverview(results.get("total"));
        }

        if (results.containsKey("traffic")) {
            updateTraffic(results.get("traffic"));
        }

        if (results.containsKey("statusCodes")) {
            updateStatusCodes(results.get("statusCodes"));
        }

        if (results.containsKey("endpoints")) {
            updateEndpoints(results.get("endpoints"));
        }

        if (results.containsKey("performance")) {
            updatePerformance(results.get("performance"));
        }

        if (results.containsKey("security")) {
            updateSecurity(results.get("security"));
        }
    }

    private void updateOverview(AnalysisResult totalResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 LOG ANALYSIS OVERVIEW\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = totalResult.getData();

        if (data.containsKey("Total")) {
            sb.append("Total Requests: ").append(data.get("Total")).append("\n");
        } else {
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

    private void updateTraffic(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("📈 TRAFFIC BY HOUR\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        Map<Integer, Long> hourlyTraffic = new java.util.HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String dateTime = entry.getKey();
            Long count = (Long) entry.getValue();

            try {
                String hourStr = dateTime.split(" ")[1].split(":")[0];
                int hour = Integer.parseInt(hourStr);
                hourlyTraffic.merge(hour, count, Long::sum);
            } catch (Exception e) {
                // Skip invalid entries
            }
        }

        if (!hourlyTraffic.isEmpty()) {
            long max = hourlyTraffic.values().stream().max(Long::compare).orElse(1L);

            hourlyTraffic.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int hour = entry.getKey();
                        long count = entry.getValue();

                        int barLength = (int) ((count * 50.0) / max);
                        String bar = "█".repeat(Math.max(0, barLength));

                        sb.append(String.format("%02d:00 | %-50s %d\n", hour, bar, count));
                    });
        } else {
            sb.append("No traffic data available.\n");
        }

        trafficTextArea.setText(sb.toString());
    }

    private void updateStatusCodes(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔢 HTTP STATUS CODE DISTRIBUTION\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        Map<Integer, Long> statusCodes = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                int code = Integer.parseInt(entry.getKey());
                long count = (Long) entry.getValue();
                statusCodes.put(code, count);
            } catch (NumberFormatException e) {
                // Skip non-numeric keys
            }
        }

        if (!statusCodes.isEmpty()) {
            long total = statusCodes.values().stream().mapToLong(Long::longValue).sum();

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

    private void updateEndpoints(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 TOP ENDPOINTS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        if (!data.isEmpty()) {
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
        } else {
            sb.append("No endpoint data available.\n");
        }

        endpointsTextArea.setText(sb.toString());
    }

    private void updatePerformance(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚡ PERFORMANCE METRICS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        sb.append("Response Size Statistics:\n\n");

        if (data.containsKey("averageResponseSize")) {
            double avgBytes = (Double) data.get("averageResponseSize");
            sb.append(String.format("Average: %.2f KB\n", avgBytes / 1024.0));
        }

        if (data.containsKey("minResponseSize")) {
            double minBytes = (Double) data.get("minResponseSize");
            sb.append(String.format("Minimum: %.2f KB\n", minBytes / 1024.0));
        }

        if (data.containsKey("maxResponseSize")) {
            double maxBytes = (Double) data.get("maxResponseSize");
            sb.append(String.format("Maximum: %.2f KB\n", maxBytes / 1024.0));
        }

        if (data.containsKey("totalDataTransferred")) {
            double totalBytes = (Double) data.get("totalDataTransferred");
            sb.append(String.format("Total Transferred: %.2f MB\n", totalBytes / (1024.0 * 1024.0)));
        }

        if (data.containsKey("largestEndpoints")) {
            @SuppressWarnings("unchecked")
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

    private void updateSecurity(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔒 SECURITY ANALYSIS\n");
        sb.append("═══════════════════════════════════════\n\n");

        Map<String, Object> data = result.getData();

        if (data.containsKey("suspiciousRequestCount")) {
            int suspiciousCount = (Integer) data.get("suspiciousRequestCount");
            sb.append(String.format("⚠️  Suspicious Requests Detected: %d\n\n", suspiciousCount));
        }

        if (data.containsKey("potentialAttackers")) {
            @SuppressWarnings("unchecked")
            java.util.List<String> attackers = (java.util.List<String>) data.get("potentialAttackers");

            if (!attackers.isEmpty()) {
                sb.append("🚨 POTENTIAL ATTACKERS (>10 failed auth attempts):\n\n");
                for (String ip : attackers) {
                    sb.append(String.format("   • %s\n", ip));
                }
                sb.append("\n");
            } else {
                sb.append("✅ No potential attackers detected (brute force threshold: 10 attempts)\n\n");
            }
        }

        if (data.containsKey("unauthorizedAttempts")) {
            int unauthorizedCount = (Integer) data.get("unauthorizedAttempts");
            sb.append(String.format("Failed Authentication Attempts: %d unique IPs\n\n", unauthorizedCount));
        }

        if (data.containsKey("topOffendingIPs")) {
            @SuppressWarnings("unchecked")
            Map<String, Long> topOffenders = (Map<String, Long>) data.get("topOffendingIPs");

            if (!topOffenders.isEmpty()) {
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

    private String getStatusEmoji(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "✓";
        if (statusCode >= 300 && statusCode < 400) return "↪";
        if (statusCode >= 400 && statusCode < 500) return "✗";
        if (statusCode >= 500 && statusCode < 600) return "⚠";
        return "?";
    }
}
