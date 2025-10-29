package com.github.mateigatin.loganalyzerplugin.actions;

import com.github.mateigatin.loganalyzerplugin.analyzer.*;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.github.mateigatin.loganalyzerplugin.parser.ApacheLogParser;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
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
        e.getPresentation().setEnabledAndVisible(file != null && !file.isDirectory());
//        boolean isLogFile = file != null &&
//                !file.isDirectory() &&
//                (file.getName().endsWith(".log") || file.getName().endsWith(".txt"));
//        e.getPresentation().setEnabledAndVisible(isLogFile);
    }

    // TODO: FIX THIS METHOD:
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

            // Update the tool window with results - we'll use a service to access it
            ApplicationManager.getApplication().invokeLater(() -> {
                LogAnalyzerWindow window = getLogAnalyzerWindow(project);
                if (window != null) {
                    window.displayResults(
                            totalResult,
                            trafficResult,
                            statusResult,
                            endpointsResult,
                            performanceResult,
                            securityResult
                    );
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

    private LogAnalyzerWindow getLogAnalyzerWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LogAnalyzer");
        if (toolWindow != null && toolWindow.getContentManager().getContentCount() > 0) {
            // Get the window instance from the project's user data
            return project.getUserData(LogAnalyzerWindow.KEY);
        }
        return null;
    }
}
