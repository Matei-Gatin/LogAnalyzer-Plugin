package com.github.mateigatin.loganalyzerplugin.toolWindow;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class LogAnalyzerToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LogAnalyzerWindow logAnalyzerWindow = project.getUserData(LogAnalyzerWindow.KEY);

        if (logAnalyzerWindow == null)
        {
            logAnalyzerWindow = new LogAnalyzerWindow(project);
            project.putUserData(LogAnalyzerWindow.KEY, logAnalyzerWindow);
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(logAnalyzerWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}