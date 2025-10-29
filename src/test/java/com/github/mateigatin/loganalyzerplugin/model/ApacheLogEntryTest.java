package com.github.mateigatin.loganalyzerplugin.model;

import com.github.mateigatin.loganalyzerplugin.model.ApacheLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ApacheLogEntryTest {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    @Test
    void testValidLogEntry() {
        ZonedDateTime timestamp = ZonedDateTime.parse("10/Dec/2019:13:55:36 -0700", DATE_FORMATTER);

        ApacheLogEntry entry = new ApacheLogEntry(
                "127.0.0.1",
                "Scott",
                timestamp,
                "GET",
                "/server-status",
                HttpStatus.OK,
                2326
        );

        assertEquals("127.0.0.1", entry.getIpAddress());
        assertEquals("Scott", entry.getUserId());
        assertEquals("GET", entry.getRequestType());
        assertEquals("/server-status", entry.getEndpoint());
        assertEquals(HttpStatus.OK, entry.getHttpStatusCode());
        assertEquals(2326, entry.getResponseSize());
        assertTrue(entry.isSuccessful());
        assertFalse(entry.isClientError());
        assertFalse(entry.isServerError());
    }

    @Test
    void testClientErrorStatus() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        ApacheLogEntry entry = new ApacheLogEntry(
                "10.0.0.1",
                "user",
                timestamp,
                "GET",
                "/admin",
                HttpStatus.FORBIDDEN,
                0
        );

        assertTrue(entry.isClientError());
        assertFalse(entry.isSuccessful());
        assertFalse(entry.isServerError());
    }

    @Test
    void testServerErrorStatus() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        ApacheLogEntry entry = new ApacheLogEntry(
                "10.0.0.1",
                "user",
                timestamp,
                "POST",
                "/api/data",
                HttpStatus.INTERNAL_SERVER_ERROR,
                0
        );

        assertTrue(entry.isServerError());
        assertFalse(entry.isSuccessful());
        assertFalse(entry.isClientError());
    }

    @Test
    void testNullIpAddressThrowsException() {
        ZonedDateTime timestamp = ZonedDateTime.now();

        assertThrows(NullPointerException.class, () -> {
            new ApacheLogEntry(
                    null,
                    "user",
                    timestamp,
                    "GET",
                    "/test",
                    HttpStatus.OK,
                    100
            );
        });
    }

    @Test
    void testNullTimestampThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new ApacheLogEntry(
                    "127.0.0.1",
                    "user",
                    null,
                    "GET",
                    "/test",
                    HttpStatus.OK,
                    100
            );
        });
    }

    @Test
    void testGetLogType() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        ApacheLogEntry entry = new ApacheLogEntry(
                "127.0.0.1",
                "user",
                timestamp,
                "GET",
                "/test",
                HttpStatus.OK,
                100
        );

        assertEquals("Apache", entry.getLogType());
    }
}