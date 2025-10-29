package com.github.mateigatin.loganalyzerplugin.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import com.github.mateigatin.loganalyzerplugin.parser.ApacheLogParser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApacheLogParserTest {

    @Test
    void testValidLogLineFormat() {
        String validLog = "127.0.0.1 - Scott [10/Dec/2019:13:55:36 -0700] \"GET /server-status HTTP/1.1\" 200 2326";
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        assertTrue(parser.isValidFormat(validLog));
    }

    @Test
    void testInvalidLogLineFormat() {
        String invalidLog = "This is not a valid log line";
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        assertFalse(parser.isValidFormat(invalidLog));
    }

    @Test
    void testParseValidLogLine() {
        String logLine = "127.0.0.1 - Scott [10/Dec/2019:13:55:36 -0700] \"GET /server-status HTTP/1.1\" 200 2326";
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        Optional<AbstractLogEntry> result = parser.parseLogLine(logLine);

        assertTrue(result.isPresent());
        AbstractLogEntry entry = result.get();
        assertEquals("127.0.0.1", entry.getIpAddress());
        assertEquals("Scott", entry.getUserId());
        assertEquals("GET /server-status HTTP/1.1", entry.getRequestType());
        assertEquals("/server-status", entry.getEndpoint());
        assertEquals(HttpStatus.OK, entry.getHttpStatusCode());
        assertEquals(2326, entry.getResponseSize());
    }

    @Test
    void testParseLogLineWithDashForSize() {
        String logLine = "192.168.1.1 - admin [10/Dec/2019:13:55:36 -0700] \"POST /api/login HTTP/1.1\" 401 -";
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        Optional<AbstractLogEntry> result = parser.parseLogLine(logLine);

        assertTrue(result.isPresent());
        AbstractLogEntry entry = result.get();
        assertEquals(0, entry.getResponseSize());
        assertEquals(HttpStatus.UNAUTHORIZED, entry.getHttpStatusCode());
    }

    @Test
    void testParseLogLineWithDifferentStatusCodes() {
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        String line404 = "10.0.0.1 - user [10/Dec/2019:13:55:36 -0700] \"GET /missing HTTP/1.1\" 404 0";
        Optional<AbstractLogEntry> result404 = parser.parseLogLine(line404);
        assertTrue(result404.isPresent());
        assertEquals(HttpStatus.NOT_FOUND, result404.get().getHttpStatusCode());

        String line500 = "10.0.0.1 - user [10/Dec/2019:13:55:36 -0700] \"GET /error HTTP/1.1\" 500 0";
        Optional<AbstractLogEntry> result500 = parser.parseLogLine(line500);
        assertTrue(result500.isPresent());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result500.get().getHttpStatusCode());
    }

    @Test
    void testParseInvalidLogLine() {
        String invalidLog = "This is not a valid log line";
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        Optional<AbstractLogEntry> result = parser.parseLogLine(invalidLog);

        assertFalse(result.isPresent());
    }

    @Test
    void testParseEmptyLogLine() {
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        Optional<AbstractLogEntry> result = parser.parseLogLine("");

        assertFalse(result.isPresent());
    }

    @Test
    void testParseNullLogLine() {
        ApacheLogParser parser = new ApacheLogParser("dummy.log");

        Optional<AbstractLogEntry> result = parser.parseLogLine(null);

        assertFalse(result.isPresent());
    }

    @Test
    void testParseMultipleLogLines(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = """
            127.0.0.1 - user1 [10/Dec/2019:13:55:36 -0700] "GET /page1 HTTP/1.1" 200 1234
            192.168.1.1 - user2 [10/Dec/2019:13:56:00 -0700] "POST /api/data HTTP/1.1" 201 512
            10.0.0.1 - user3 [10/Dec/2019:13:57:00 -0700] "GET /admin HTTP/1.1" 403 0
            """;

        Files.writeString(logFile, logContent);

        ApacheLogParser parser = new ApacheLogParser(logFile.toString());
        List<AbstractLogEntry> entries = parser.parse();

        assertEquals(3, entries.size());
        assertEquals("127.0.0.1", entries.get(0).getIpAddress());
        assertEquals("192.168.1.1", entries.get(1).getIpAddress());
        assertEquals("10.0.0.1", entries.get(2).getIpAddress());
    }

    @Test
    void testParseFileWithEmptyLines(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = """
            127.0.0.1 - user1 [10/Dec/2019:13:55:36 -0700] "GET /page1 HTTP/1.1" 200 1234

            192.168.1.1 - user2 [10/Dec/2019:13:56:00 -0700] "POST /api/data HTTP/1.1" 201 512
            """;

        Files.writeString(logFile, logContent);

        ApacheLogParser parser = new ApacheLogParser(logFile.toString());
        List<AbstractLogEntry> entries = parser.parse();

        assertEquals(2, entries.size());
    }

    @Test
    void testParseFileWithInvalidLines(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = """
            127.0.0.1 - user1 [10/Dec/2019:13:55:36 -0700] "GET /page1 HTTP/1.1" 200 1234
            This is an invalid line
            192.168.1.1 - user2 [10/Dec/2019:13:56:00 -0700] "POST /api/data HTTP/1.1" 201 512
            """;

        Files.writeString(logFile, logContent);

        ApacheLogParser parser = new ApacheLogParser(logFile.toString());
        List<AbstractLogEntry> entries = parser.parse();

        assertEquals(2, entries.size());
    }
}
