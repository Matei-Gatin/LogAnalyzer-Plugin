package com.github.mateigatin.loganalyzerplugin.parser;

import lombok.extern.slf4j.Slf4j;
import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ApacheLogParser implements LogParser
{
    // 127.0.0.1 - Scott [10/Dec/2019:13:55:36 -0700] "GET /server-status HTTP/1.1" 200 2326

    private final Path filePath;
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\S+) \\S+ (\\S+) \\[([^\\]]+)\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+|-)$"
    );

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    public ApacheLogParser(final String filePath)
    {
        this.filePath = Path.of(filePath);
    }

    @Override
    public List<AbstractLogEntry> parse()
    {
        List<AbstractLogEntry> entries = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath))
        {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null)
            {
                lineNumber++;

                Optional<AbstractLogEntry> entry = parseLogLine(line);
                entry.ifPresent(entries::add);
            }

            log.info("Successfully parsed {} log entries from {}", entries.size(), filePath);

        } catch (IOException e)
        {
            log.error("Error reading file: {}", filePath, e);
            throw new RuntimeException("Failed to read log file", e);
        }

        return entries;
    }

    @Override
    public boolean isValidFormat(String string)
    {
        return LOG_PATTERN.matcher(string).matches();
    }

    @Override
    public Optional<AbstractLogEntry> parseLogLine(String logLine)
    {
        if (logLine == null || logLine.trim().isEmpty())
        {
            return Optional.empty();
        }

        try {
            Matcher matcher = LOG_PATTERN.matcher(logLine);

            if (!matcher.find())
            {
                log.warn("Invalid log format: {}", logLine);
                return Optional.empty();
            }

            String ipAddress = matcher.group(1);
            String userId = matcher.group(2);
            ZonedDateTime dateTime = ZonedDateTime.parse(matcher.group(3),
                    DATE_FORMATTER);

            String httpMethod = matcher.group(4); // split requestType
            String endpoint = matcher.group(5); //
            String protocol = matcher.group(6); //

            String fullRequestType = httpMethod + " " + endpoint + " " + protocol;
            HttpStatus statusCode = HttpStatus.fromCode(Integer.parseInt(matcher.group(7)));
            long objectSize = matcher.group(8).equals("-") ? 0 : Long.parseLong(matcher.group(8));

            return Optional.of(
                    new ApacheLogEntry(
                            ipAddress,
                            userId,
                            dateTime,
                            fullRequestType,
                            endpoint,
                            statusCode,
                            objectSize)
            );

        } catch (DateTimeParseException | IllegalArgumentException e)
        {
            log.warn("Error parsing log line: {} - {}", logLine, e.getMessage());
            return Optional.empty();
        }
    }
}
