package com.github.mateigatin.loganalyzerplugin.stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.parser.LogParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class LogStreamProcessor implements AutoCloseable
{
    private final Path logFile;
    private final LogParser parser;
    @Getter
    private final BlockingQueue<AbstractLogEntry> entryQueue;
    @Getter
    private volatile boolean running = false;
    private Thread processingThread;
    private long lastFilePosition = 0;

    public LogStreamProcessor(Path logFile, LogParser parser)
    {
        this.logFile = logFile;
        this.parser = parser;
        this.entryQueue = new LinkedBlockingQueue<>(1000);
    }

    public void startMonitoring(Consumer<AbstractLogEntry> entryConsumer)
    {
        running = true;

        try
        {
            lastFilePosition = Files.size(logFile);
        } catch (IOException e)
        {
            log.warn("Could not get initial file size, starting from beginning", e);
            lastFilePosition = 0;
        }

        processingThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService())
            {
                Path directory = logFile.getParent();
                if (directory == null)
                {
                    directory = Paths.get(".");
                }

                directory.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE
                );

                log.info("Started monitoring log file: {}", logFile);
                log.info("Monitoring directory: {}", directory);
                log.info("Waiting for new log entries...");

                while (running)
                {
                    WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key != null)
                    {
                        for (WatchEvent<?> event : key.pollEvents())
                        {
                            Path changed = (Path) event.context();
                            if (changed.toString().equals(logFile.getFileName().toString()))
                            {
                                log.debug("Detected change in log file");
                                processNewEntries(entryConsumer);
                            }
                        }
                        key.reset();
                    }
                }
            } catch (Exception e)
            {
                log.error("Error monitoring log file", e);
            }
        });
        processingThread.setName("LogStreamProcessor-" + logFile.getFileName());
        processingThread.start();
    }

    private void processNewEntries(Consumer<AbstractLogEntry> entryConsumer)
    {
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r"))
        {
            long fileLength = raf.length();

            // check if file was truncated (rotated)
            if (fileLength < lastFilePosition)
            {
                log.info("Log file appears to have been rotated, starting from beginning");
                lastFilePosition = 0;
            }

            // No new data
            if (fileLength == lastFilePosition)
            {
                return;
            }

            raf.seek(lastFilePosition);

            String line;
            int newEntriesCount = 0;

            // Read new lines
            while ((line = raf.readLine()) != null)
            {
                if (line.trim().isEmpty())
                {
                    continue;
                }

                // Parse the new log line
                Optional<AbstractLogEntry> entry = parser.parseLogLine(line);

                if (entry.isPresent())
                {
                    AbstractLogEntry logEntry = entry.get();

                    // add to queue for processing
                    try
                    {
                        entryQueue.put(logEntry);
                    } catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while queuing entry", e);
                        break;
                    }

                    // Consume the entry (real-time processing)
                    entryConsumer.accept(logEntry);
                    newEntriesCount++;

                    log.debug("Processed new entry: {} {} {}",
                            logEntry.getIpAddress(),
                            logEntry.getRequestType(),
                            logEntry.getEndpoint());
                } else
                {
                    log.warn("Failed to parse new log line: {}", line);
                }
            }

            // Update file position
            lastFilePosition = raf.getFilePointer();

            if (newEntriesCount > 0)
            {
                log.info("Processed {} new log entries", newEntriesCount);
            }
        } catch (IOException e)
        {
            log.error("Error reading new log entries", e);
        }
    }

    @Override
    public void close()
    {
        log.info("Stopping log stream processor...");
        running = false;
        if (processingThread != null)
        {
            processingThread.interrupt();
            try
            {
                processingThread.join(5000);
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for processor thread to stop");
            }
        }
        entryQueue.clear();
        log.info("Log stream processor stopped");
    }
}
