package com.github.mateigatin.loganalyzerplugin.actions;

import com.github.mateigatin.loganalyzerplugin.analyzer.*;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.parser.ApacheLogParser;
import com.github.mateigatin.loganalyzerplugin.parser.LogParser;
import com.github.mateigatin.loganalyzerplugin.toolWindow.LogAnalyzerWindow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AnalyzeLogFileAction extends AnAction
{
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread()
    {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        // Show and activate the tool window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LogAnalyzer");
        if (toolWindow != null)
        {
            toolWindow.show();
        }

        // Run analysis in background with progress indicator
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analyzing Log File: " + file.getName(), true){
            @Override
            public void run(@NotNull ProgressIndicator indicator)
            {
                try
                {
                    analyzeLogFile(project, file.getPath(), indicator);
                } catch (Exception ex)
                {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project,
                                "Error analyzing log file: " + ex.getMessage(),
                                "Analysis Error");
                    });
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only show action for .log files
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
//        e.getPresentation().setEnabledAndVisible(file != null && !file.isDirectory());
//        boolean isLogFile = file != null &&
//                !file.isDirectory() &&
//                (file.getName().endsWith(".log") || file.getName().endsWith(".txt"));
//        e.getPresentation().setEnabledAndVisible(isLogFile);

        boolean enabled = file != null &&
                !file.isDirectory() &&
                (file.getName().endsWith(".log") || file.getName().endsWith(".txt"));
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private void analyzeLogFile(Project project, String filePath, ProgressIndicator indicator)
    {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);
        indicator.setText("Reading log file...");

        try
        {
            ApacheLogParser parser = new ApacheLogParser(filePath);
            List<AbstractLogEntry> logEntries = parser.parse();


            if (logEntries.isEmpty())
            {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showWarningDialog(project,
                            "No valid log entries found. Please check the log format.",
                            "No Data");
                });
                return;
            }

            indicator.setFraction(0.4);
            indicator.setText("Running analyzers...");

            // Run all analyzers
            TotalRequestAnalyzer totalAnalyzer = new TotalRequestAnalyzer();
            AnalysisResult totalResult = totalAnalyzer.analyze(logEntries);

            indicator.setFraction(0.5);
            StatusCodeAnalyzer statusAnalyzer = new StatusCodeAnalyzer();
            AnalysisResult statusResult = statusAnalyzer.analyze(logEntries);

            indicator.setFraction(0.6);
            TrafficByHourAnalyzer trafficAnalyzer = new TrafficByHourAnalyzer();
            AnalysisResult trafficResult = trafficAnalyzer.analyze(logEntries);

            indicator.setFraction(0.7);
            TopEndpointsAnalyzer endpointsAnalyzer = new TopEndpointsAnalyzer();
            AnalysisResult endpointsResult = endpointsAnalyzer.analyze(logEntries);

            indicator.setFraction(0.8);
            PerformanceAnalyzer performanceAnalyzer = new PerformanceAnalyzer();
            AnalysisResult performanceResult = performanceAnalyzer.analyze(logEntries);

            indicator.setFraction(0.9);
            SecurityAnalyzer securityAnalyzer = new SecurityAnalyzer();
            AnalysisResult securityResult = securityAnalyzer.analyze(logEntries);

            indicator.setFraction(1.0);
            indicator.setText("Displaying results...");

            // Update the tool window with results
            ApplicationManager.getApplication().invokeLater(() ->
            {
                LogAnalyzerWindow window = getLogAnalyzerWindow(project);

                if (window != null) {
                    // Put everything in map and pass to displayResults
                    Map<String, AnalysisResult> results = new HashMap<>();
                    results.put("total", totalResult);
                    results.put("traffic", trafficResult);
                    results.put("statusCodes", statusResult);
                    results.put("endpoints", endpointsResult);
                    results.put("performance", performanceResult);
                    results.put("security", securityResult);

                    // Pass the file path so Watch Mode knows what to monitor
                    window.displayResults(results, filePath);
                }

                Messages.showInfoMessage(project,
                        String.format("Analyzed %d log entries successfully!", logEntries.size()),
                        "Analysis Complete");
            });
        } catch (Exception ex)
        {
            ApplicationManager.getApplication().invokeLater(() ->
            {
                Messages.showErrorDialog(project,
                        "Error analyzing log file: " + ex.getMessage(),
                        "Analysis Error");
            });
            ex.printStackTrace();
        }
    }

    /**
     * Public static method to re-analyze a file (used by Watch Mode)
     */
    public static void reAnalyzerFile(Project project, String filePath, LogAnalyzerWindow window)
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Re-analyzing Log File", false)
        {
            @Override
            public void run(@NotNull ProgressIndicator indicator)
            {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                indicator.setText("Reading updated log file...");

                try
                {
                    ApacheLogParser parser = new ApacheLogParser(filePath);
                    List<AbstractLogEntry> logEntries = parser.parse();

                    if (logEntries.isEmpty())
                    {
                        return; // skip if empty
                    }

                    indicator.setFraction(0.3);
                    indicator.setText("Running analyzers...");

                    // Run all analyzers
                    Map<String, AnalysisResult> results = new HashMap<>();

                    TotalRequestAnalyzer totalAnalyzer = new TotalRequestAnalyzer();
                    results.put("total", totalAnalyzer.analyze(logEntries));

                    StatusCodeAnalyzer statusAnalyzer = new StatusCodeAnalyzer();
                    results.put("statusCodes", statusAnalyzer.analyze(logEntries));

                    TrafficByHourAnalyzer trafficAnalyzer = new TrafficByHourAnalyzer();
                    results.put("traffic", trafficAnalyzer.analyze(logEntries));

                    TopEndpointsAnalyzer endpointsAnalyzer = new TopEndpointsAnalyzer();
                    results.put("endpoints", endpointsAnalyzer.analyze(logEntries));

                    PerformanceAnalyzer performanceAnalyzer = new PerformanceAnalyzer();
                    results.put("performance", performanceAnalyzer.analyze(logEntries));

                    SecurityAnalyzer securityAnalyzer = new SecurityAnalyzer();
                    results.put("security", securityAnalyzer.analyze(logEntries));

                    indicator.setFraction(1.0);
                    indicator.setText("Updating display...");

                    // Update UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        window.displayResults(results, filePath);
                    });
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

        });
    }


    private LogAnalyzerWindow getLogAnalyzerWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("LogAnalyzer");

        if (toolWindow == null)
        {
            System.out.println("DEBUG: ToolWindow 'LogAnalyzer' not found!");
            return null;
        }

        toolWindow.show(null);

        Content content = toolWindow.getContentManager().getContent(0);
        if (content == null)
        {
            System.out.println("DEBUG: No content found in tool window!");
            return null;
        }

        // Get the component from content
//        JComponent component = content.getComponent();
//        if (component == null) {
//            System.out.println("DEBUG: No component in content!");
//            return null;
//        }

        // Try to get LogAnalyzerWindow from project user data
        LogAnalyzerWindow window = project.getUserData(LogAnalyzerWindow.KEY);

        if (window == null) {
            // This shouldn't happen, but create one if needed
            window = new LogAnalyzerWindow(project);
            project.putUserData(LogAnalyzerWindow.KEY, window);
        }

        return window;
    }

}
