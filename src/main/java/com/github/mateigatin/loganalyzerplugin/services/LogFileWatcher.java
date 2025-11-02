package com.github.mateigatin.loganalyzerplugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class LogFileWatcher
{
    private static final Logger LOG = Logger.getInstance(LogFileWatcher.class);

    private final Project project;
    private WatchService watchService;
    private Thread watchThread;
    private final AtomicBoolean watching = new AtomicBoolean(false);
    private Path watchedFilePath;
    private Consumer<Path> onFileChanged;

    public LogFileWatcher(Project project)
    {
        this.project = project;
    }

    public void startWatching(String filePath, Consumer<Path> onFileChanged)
    {
        if (watching.get())
        {
            LOG.warn("Already watching a file. Stop current watch first.");
            return;
        }

        try
        {
            this.watchedFilePath = Paths.get(filePath);
            this.onFileChanged = onFileChanged;
            this.watchService = FileSystems.getDefault().newWatchService();

            // Watch the directory containing the file
            Path dir = watchedFilePath.getParent();
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            watching.set(true);

            // Start watch thread
            watchThread = new Thread(() ->
            {
               LOG.info("Started watching file: " + filePath);

               while (watching.get())
               {
                   try
                   {
                       WatchKey key = watchService.take();

                       for (WatchEvent<?> event : key.pollEvents())
                       {
                           WatchEvent.Kind<?> kind = event.kind();

                           if (kind == StandardWatchEventKinds.OVERFLOW)
                           {
                               continue;
                           }

                           @SuppressWarnings("unchecked")
                           WatchEvent<Path> ev = (WatchEvent<Path>) event;
                           Path filename = ev.context();
                           Path fullPath = dir.resolve(filename);

                           // Check if it's our watched file
                           if (fullPath.equals(watchedFilePath))
                           {
                               LOG.info("File changed: " + fullPath);

                               // Notify listener on UI thread
                               ApplicationManager.getApplication()
                                       .invokeLater(() -> onFileChanged.accept(fullPath));

                           }
                       }

                       boolean valid = key.reset();
                       if (!valid)
                       {
                           break;
                       }
                   } catch (InterruptedException e)
                   {
                       Thread.currentThread().interrupt();
                       break;
                   } catch (Exception e)
                   {
                       LOG.error("Error in watch thread", e);
                   }
               }

               LOG.info("Stopped watching file");
            }, "LogFileWatcher-" + project.getName());

            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e)
        {
            LOG.error("Failed to start watching file: " + filePath, e);
            watching.set(false);
        }
    }

    public void stopWatching() {
        if (!watching.get()) {
            return;
        }

        watching.set(false);

        if (watchThread != null) {
            watchThread.interrupt();
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOG.error("Error closing watch service", e);
            }
        }

        LOG.info("Stopped watching file");
    }

    public boolean isWatching() {
        return watching.get();
    }

    public String getWatchedFilePath() {
        return watchedFilePath != null ? watchedFilePath.toString() : null;
    }
}
