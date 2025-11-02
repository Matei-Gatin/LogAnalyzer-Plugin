package com.github.mateigatin.loganalyzerplugin.toolWindow;

import com.github.mateigatin.loganalyzerplugin.actions.AnalyzeLogFileAction;
import com.github.mateigatin.loganalyzerplugin.analyzer.*;
import com.github.mateigatin.loganalyzerplugin.export.ExportService;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.model.LogFilter;
import com.github.mateigatin.loganalyzerplugin.services.LogFileWatcher;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Watch Mode components
    private final LogFileWatcher fileWatcher;
    private String currentLogFilePath;
    private JButton watchButton;
    private JLabel watchStatusLabel;

    // filter UI fields
    private LogFilter currentFilter = new LogFilter();
    private java.util.List<AbstractLogEntry> allLogEntries = new java.util.ArrayList<>();
    private JBTextField searchField;
    private JBTextField startDateField;
    private JBTextField endDateField;
    private JBTextField statusCodeField;
    private JLabel filterStatusLabel;

    public LogAnalyzerWindow(Project project)
    {
        this.project = project;
        this.fileWatcher = new LogFileWatcher(project);
        this.mainPanel = new JPanel(new BorderLayout());

        // toolbar with export button
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Filter panel
        JPanel filterPanel = createFilterPanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);

        // Toolbar above filter panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

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

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();
    }

    // keyboard Shortcuts
    private void setupKeyboardShortcuts() {
        // Get the root component's input/action maps
        JComponent rootPane = mainPanel;

        // Ctrl+F - Focus search field
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
                "focusSearch"
        );
        rootPane.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        // Ctrl+Shift+G - Export
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "export"
        );
        rootPane.getActionMap().put("export", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showExportDialog();
            }
        });

        // Ctrl+W - Toggle watch mode
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "toggleWatch"
        );
        rootPane.getActionMap().put("toggleWatch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (watchButton.isEnabled()) {
                    toggleWatchMode();
                }
            }
        });

        // Escape - Clear filters
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "clearFilters"
        );
        rootPane.getActionMap().put("clearFilters", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!allLogEntries.isEmpty()) {
                    clearFilters();
                }
            }
        });

        // Enter in search field - Apply filters
        searchField.addActionListener(e -> applyFilters());
        statusCodeField.addActionListener(e -> applyFilters());
        startDateField.addActionListener(e -> applyFilters());
        endDateField.addActionListener(e -> applyFilters());
    }

    // Creating the filter panel
    private JPanel createFilterPanel()
    {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.GRAY),
                "🔍 Filters",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        // Row 1: Search and Status Code
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        row1.add(new JLabel("Search:"));
        searchField = new JBTextField(20);
        searchField.setToolTipText("Search by IP, endpoint, or any keyword (Ctrl+F to focus, Enter to apply)");
        row1.add(searchField);

        row1.add(Box.createHorizontalStrut(10));

        row1.add(new JLabel("Status Code:"));
        statusCodeField = new JBTextField(5);
        statusCodeField.setToolTipText("Filter by HTTP status code (e.g., 200, 404)");
        row1.add(statusCodeField);

        // Row 2: Date Range
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        row2.add(new JLabel("From:"));
        startDateField = new JBTextField(15);
        startDateField.setToolTipText("Start date (format: 2025-10-30 10:00)");
        row2.add(startDateField);

        row2.add(new JLabel("To:"));
        endDateField = new JBTextField(15);
        endDateField.setToolTipText("End date (format: 2025-10-30 15:00)");
        row2.add(endDateField);

        // Buttons
        JButton applyButton = new JButton("🔍 Apply Filter");
        applyButton.addActionListener(e -> applyFilters());
        row2.add(applyButton);

        JButton clearButton = new JButton("❌ Clear");
        clearButton.addActionListener(e -> clearFilters());
        row2.add(clearButton);

        // Filter status label
        filterStatusLabel = new JLabel("No filters active");
        filterStatusLabel.setForeground(JBColor.GRAY);
        filterStatusLabel.setFont(filterStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.add(filterStatusLabel);

        filterPanel.add(row1);
        filterPanel.add(row2);
        filterPanel.add(row3);

        return filterPanel;
    }

    // Apply filters
    private void applyFilters()
    {
        if (allLogEntries.isEmpty())
        {
            Messages.showWarningDialog(
                    project,
                    "No log entries loaded. Please analyze a file first.",
                    "No Data"
            );
            return;
        }

        try
        {
            // Update filter from UI fields
            currentFilter.setSearchQuery(searchField.getText().trim());

            // Parse status code
            String statusText = statusCodeField.getText().trim();
            if (!statusText.isEmpty()) {
                try
                {
                    currentFilter.setStatusCodeFilter(Integer.parseInt(statusText));
                } catch (NumberFormatException e)
                {
                    Messages.showErrorDialog(
                            project,
                            "Invalid status code. Please enter a number (e.g., 200, 404).",
                            "Invalid Input"
                    );
                    return;
                }
            } else
            {
                currentFilter.setStatusCodeFilter(null);
            }

            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            String startText = startDateField.getText().trim();
            if (!startText.isEmpty())
            {
                try
                {
                    currentFilter.setStartDate(LocalDateTime.parse(startText, formatter));
                } catch (DateTimeParseException e)
                {
                    Messages.showErrorDialog(
                            project,
                            "Invalid start date format. Use: yyyy-MM-dd HH:mm (e.g., 2025-10-30 10:00)",
                            "Invalid Date"
                    );
                    return;
                }
            } else {
                currentFilter.setStartDate(null);
            }

            String endText = endDateField.getText().trim();
            if (!endText.isEmpty())
            {
                try
                {
                    currentFilter.setEndDate(LocalDateTime.parse(endText, formatter));
                } catch (DateTimeParseException e)
                {
                    Messages.showErrorDialog(
                            project,
                            "Invalid end date format. Use: yyyy-MM-dd HH:mm (e.g., 2025-10-30 15:00)",
                            "Invalid Date"
                    );
                    return;
                }
            } else
            {
                currentFilter.setEndDate(null);
            }

            // Apply filter
            java.util.List<AbstractLogEntry> filteredEntries = allLogEntries.stream()
                    .filter(currentFilter::matches)
                    .collect(Collectors.toList());

            if (filteredEntries.isEmpty())
            {
                Messages.showInfoMessage(
                        project,
                        "No log entries match the current filters.",
                        "No Results"
                );
                return;
            }

            // Re-analyze with filtered data
            reAnalyzeWithEntries(filteredEntries);

            // Update status
            filterStatusLabel.setText(String.format(
                    "📊 Showing %d of %d entries - %s",
                    filteredEntries.size(),
                    allLogEntries.size(),
                    currentFilter.toString()
            ));
            filterStatusLabel.setForeground(new Color(0, 120, 215));

            showNotification("Filters Applied",
                    String.format("Showing %d matching entries", filteredEntries.size()));

        } catch (Exception ex) {
            Messages.showErrorDialog(
                    project,
                    "Error applying filters: " + ex.getMessage(),
                    "Filter Error"
            );
            ex.printStackTrace();
        }
    }

    // Clear filters
    private void clearFilters()
    {
        if (allLogEntries.isEmpty())
        {
            return;
        }

        // Clear UI fields
        searchField.setText("");
        statusCodeField.setText("");
        startDateField.setText("");
        endDateField.setText("");

        // Reset filter
        currentFilter = new LogFilter();

        // Re-analyze with all data
        reAnalyzeWithEntries(allLogEntries);

        // Update status
        filterStatusLabel.setText("No filters active");
        filterStatusLabel.setForeground(JBColor.GRAY);

        showNotification("Filters Cleared", "Showing all entries");
    }

    // Re-analyze with specific entries
    private void reAnalyzeWithEntries(java.util.List<AbstractLogEntry> entries)
    {
        // Run all analyzers
        Map<String, AnalysisResult> results = new HashMap<>();

        TotalRequestAnalyzer totalAnalyzer = new TotalRequestAnalyzer();
        results.put("total", totalAnalyzer.analyze(entries));

        StatusCodeAnalyzer statusAnalyzer = new StatusCodeAnalyzer();
        results.put("statusCodes", statusAnalyzer.analyze(entries));

        TrafficByHourAnalyzer trafficAnalyzer = new TrafficByHourAnalyzer();
        results.put("traffic", trafficAnalyzer.analyze(entries));

        TopEndpointsAnalyzer endpointsAnalyzer = new TopEndpointsAnalyzer();
        results.put("endpoints", endpointsAnalyzer.analyze(entries));

        PerformanceAnalyzer performanceAnalyzer = new PerformanceAnalyzer();
        results.put("performance", performanceAnalyzer.analyze(entries));

        SecurityAnalyzer securityAnalyzer = new SecurityAnalyzer();
        results.put("security", securityAnalyzer.analyze(entries));

        // Update display (but don't overwrite allLogEntries)
        displayResults(results, currentLogFilePath);
    }

    private JPanel createToolbar()
    {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // Watch Mode button
        watchButton = new JButton("▶ Start Watch Mode");
        watchButton.setToolTipText("Monitor log file for real-time updates (Ctrl+W)");
        watchButton.setEnabled(false); // Disabled until a file is analyzed
        watchButton.addActionListener(e -> toggleWatchMode());

        // Watch status label
        watchStatusLabel = new JLabel("No file being watched");
        watchStatusLabel.setForeground(JBColor.GRAY);

        // Export button
        JButton exportButton = new JButton("📤 Export Report");
        exportButton.setToolTipText("Export analysis results to HTML or JSON (Ctrl+Shift+G)");
        exportButton.addActionListener(e -> showExportDialog());

        // Clear button
        JButton clearButton = new JButton("🗑️ Clear");
        clearButton.setToolTipText("Clear current analysis");
        clearButton.addActionListener(e -> clearResults());

        // Add separator
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));

        toolbar.add(watchButton);
        toolbar.add(watchStatusLabel);
        toolbar.add(separator);
        toolbar.add(exportButton);
        toolbar.add(clearButton);

        return toolbar;
    }

    private void toggleWatchMode()
    {
        if (fileWatcher.isWatching())
        {
            stopWatchMode();
        } else
        {
            startWatchMode();
        }
    }

    private void startWatchMode() {
        if (currentLogFilePath == null) {
            Messages.showWarningDialog(
                    project,
                    "No log file to watch. Please analyze a file first.",
                    "Watch Mode"
            );
            return;
        }

        fileWatcher.startWatching(currentLogFilePath, this::onFileChanged);

        watchButton.setText("⏸ Pause Watch");
        watchButton.setForeground(new Color(220, 118, 51));

        String fileName = new java.io.File(currentLogFilePath).getName();
        watchStatusLabel.setText("🟢 Watching: " + fileName);
        watchStatusLabel.setForeground(new Color(0, 150, 0));

        showNotification("Watch Mode Started", "Monitoring: " + fileName);
    }

    private void stopWatchMode() {
        fileWatcher.stopWatching();

        watchButton.setText("▶ Start Watch Mode");
        watchButton.setForeground(null); // Reset to default color
        watchStatusLabel.setText("⏸ Watch paused");
        watchStatusLabel.setForeground(JBColor.GRAY);

        showNotification("Watch Mode Paused", "File monitoring stopped");
    }

    private void onFileChanged(Path filePath) {
        // Update status label with timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        watchStatusLabel.setText("🔄 Updated at " + sdf.format(new Date()));
        watchStatusLabel.setForeground(new Color(0, 120, 215));

        // Re-analyze the file - we'll implement this method in AnalyzeLogFileAction
        // For now, show a notification
//        showNotification("Log File Updated", "Re-analyzing " + filePath.getFileName());

         AnalyzeLogFileAction.reAnalyzerFile(project, filePath.toString(), this);
    }

    private void showNotification(String title, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("LogAnalyzer.Notifications")
                .createNotification(title, message, NotificationType.INFORMATION)
                .notify(project);
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
            // Stop watching if active
            if (fileWatcher.isWatching())
            {
                stopWatchMode();
            }

            currentResults.clear();
            currentLogFilePath = null;
            watchButton.setEnabled(false);
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
            4. Use ▶ Start Watch Mode for real-time monitoring
            5. Export results using the 📤 Export button
            
            📋 Features:
            • Real-time log monitoring
            • Traffic patterns by hour
            • HTTP status code distribution
            • Top accessed endpoints
            • Performance metrics
            • Security threat detection
    
            💡 Tip: Watch Mode automatically re-analyzes when the log file changes.

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

    // Overload to accept log entries
    public void displayResults(Map<String, AnalysisResult> results, String filePath, java.util.List<AbstractLogEntry> logEntries) {
        // Store all entries for filtering
        this.allLogEntries = new java.util.ArrayList<>(logEntries);

        // Reset filter when new file is loaded
        clearFilters();

        displayResults(results, filePath);
    }

    public void displayResults(Map<String, AnalysisResult> results)
    {
        displayResults(results, null);
    }

    // Method to update all results at once
    public void displayResults(Map<String, AnalysisResult> results, String filePath) {
        // Store results for export
        this.currentResults = new HashMap<>(results);

        if (filePath != null)
        {
            this.currentLogFilePath = filePath;
            watchButton.setEnabled(true);
        }

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
        sb.append("================================================================\n");
        sb.append("                   LOG ANALYSIS OVERVIEW                        \n");
        sb.append("================================================================\n\n");

        Map<String, Object> data = totalResult.getData();

        long totalRequests = 0;
        if (data.containsKey("Total")) {
            totalRequests = ((Number) data.get("Total")).longValue();
        }

        sb.append("+------------------------------------------------------------+\n");
        sb.append(String.format("| Total Requests Analyzed: %-30d |\n", totalRequests));
        sb.append(String.format("| Analysis Time: %-40s |\n",
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )));

        if (currentLogFilePath != null) {
            String fileName = new java.io.File(currentLogFilePath).getName();
            String displayName = fileName.length() > 40 ? fileName.substring(0, 37) + "..." : fileName;
            sb.append(String.format("| Log File: %-48s |\n", displayName));
        }

        sb.append("+------------------------------------------------------------+\n\n");

        sb.append("ANALYSIS SECTIONS:\n");
        sb.append("================================================================\n");
        sb.append("  Traffic by Hour    -> View request patterns over time\n");
        sb.append("  Status Codes       -> HTTP response code distribution\n");
        sb.append("  Top Endpoints      -> Most frequently accessed URLs\n");
        sb.append("  Performance        -> Response sizes and data transfer\n");
        sb.append("  Security           -> Threat detection and suspicious activity\n");
        sb.append("\n");

        sb.append("KEYBOARD SHORTCUTS:\n");
        sb.append("================================================================\n");
        sb.append("  Ctrl+F         -> Focus search field\n");
        sb.append("  Ctrl+Shift+G   -> Export report\n");
        sb.append("  Ctrl+W         -> Toggle watch mode\n");
        sb.append("  Escape         -> Clear filters\n");
        sb.append("  Enter          -> Apply current filters\n");
        sb.append("\n");

        if (currentFilter.hasFilters()) {
            sb.append("ACTIVE FILTERS:\n");
            sb.append("================================================================\n");
            sb.append("  ").append(currentFilter.toString()).append("\n\n");
        }

        sb.append("TIP: Use filters to drill down into specific time ranges,\n");
        sb.append("     status codes, or search for specific IPs/endpoints.\n");

        overviewTextArea.setText(sb.toString());
    }

    private void updateTraffic(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append("                      TRAFFIC BY HOUR                           \n");
        sb.append("================================================================\n\n");

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
            long total = hourlyTraffic.values().stream().mapToLong(Long::longValue).sum();
            long max = hourlyTraffic.values().stream().max(Long::compare).orElse(1L);

            sb.append(String.format("Total Requests: %d\n", total));
            sb.append(String.format("Peak Hour: %02d:00 (%d requests)\n\n",
                    hourlyTraffic.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse(0),
                    max));

            sb.append("Hour  | Traffic Distribution\n");
            sb.append("------+-------------------------------------------------------\n");

            hourlyTraffic.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int hour = entry.getKey();
                        long count = entry.getValue();
                        double percentage = 100.0 * count / total;

                        // Calculate bar length (max 40 chars)
                        int barLength = (int) ((count * 40.0) / max);

                        String bar = "#".repeat(Math.max(0, barLength));
                        String space = ".".repeat(Math.max(0, 40 - barLength));

                        sb.append(String.format("%02d:00 | %s%s %4d (%.1f%%)\n",
                                hour, bar, space, count, percentage));
                    });

            sb.append("\nTip: Use date filters to zoom into specific time ranges.\n");
        } else {
            sb.append("No traffic data available.\n");
            sb.append("\nTip: Make sure your log file contains timestamp information.\n");
        }

        trafficTextArea.setText(sb.toString());
    }

    private void updateStatusCodes(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append("              HTTP STATUS CODE DISTRIBUTION                     \n");
        sb.append("================================================================\n\n");

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

            // SUMMARY SECTION
            sb.append("SUMMARY:\n");
            sb.append(String.format("[OK] 2xx Success:      %6d (%.1f%%)\n", success, 100.0 * success / total));
            sb.append(String.format("[->] 3xx Redirect:     %6d (%.1f%%)\n", redirect, 100.0 * redirect / total));
            sb.append(String.format("[!!] 4xx Client Error: %6d (%.1f%%)\n", clientError, 100.0 * clientError / total));
            sb.append(String.format("[XX] 5xx Server Error: %6d (%.1f%%)\n\n", serverError, 100.0 * serverError / total));

            // DETAILED BREAKDOWN
            sb.append("DETAILED BREAKDOWN:\n");
            sb.append("----------------------------------------------------------------\n");

            statusCodes.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String symbol = getStatusSymbol(entry.getKey());
                        double percentage = 100.0 * entry.getValue() / total;

                        // Visual bar (40 chars max)
                        int barLength = (int) ((entry.getValue() * 40.0) / total);
                        String bar = "#".repeat(Math.max(0, barLength));

                        sb.append(String.format("%s %3d: %6d requests (%.1f%%) %s\n",
                                symbol, entry.getKey(), entry.getValue(), percentage, bar));
                    });

            sb.append("\nTip: Filter by status code to see specific error patterns.\n");
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

        if (!data.isEmpty()) {
            long total = data.values().stream()
                    .mapToLong(v -> (Long) v)
                    .sum();

            int rank = 1;
            for (Map.Entry<String, Object> entry : data.entrySet())
            {
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
            double maxBytes = (Double) data.get("maxResponseSize");
            sb.append(String.format("Maximum: %.2f KB\n", maxBytes / 1024.0));
        }

        if (data.containsKey("totalDataTransferred"))
        {
            double totalBytes = (Double) data.get("totalDataTransferred");
            sb.append(String.format("Total Transferred: %.2f MB\n", totalBytes / (1024.0 * 1024.0)));
        }

        if (data.containsKey("largestEndpoints"))
        {
            @SuppressWarnings("unchecked")
            Map<String, Long> largest = (Map<String, Long>) data.get("largestEndpoints");

            if (!largest.isEmpty())
            {
                sb.append("\n\n📦 LARGEST ENDPOINTS (by data transferred):\n");

                int rank = 1;
                for (Map.Entry<String, Long> entry : largest.entrySet())
                {
                    sb.append(String.format("#%d  %s\n", rank++, entry.getKey()));
                    sb.append(String.format("    └─ %.2f KB transferred\n\n", entry.getValue() / 1024.0));
                }
            }
        }

        performanceTextArea.setText(sb.toString());
    }

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

        if (data.containsKey("unauthorizedAttempts"))
        {
            int unauthorizedCount = (Integer) data.get("unauthorizedAttempts");
            sb.append(String.format("Failed Authentication Attempts: %d unique IPs\n\n", unauthorizedCount));
        }

        if (data.containsKey("topOffendingIPs"))
        {
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

    // helper for status symbols
    private String getStatusSymbol(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "[OK]";
        if (statusCode >= 300 && statusCode < 400) return "[->]";
        if (statusCode >= 400 && statusCode < 500) return "[!!]";
        if (statusCode >= 500 && statusCode < 600) return "[XX]";
        return "[??]";
    }

    public void dispose()
    {
        if (fileWatcher.isWatching())
        {
            fileWatcher.stopWatching();
        }
    }
}
