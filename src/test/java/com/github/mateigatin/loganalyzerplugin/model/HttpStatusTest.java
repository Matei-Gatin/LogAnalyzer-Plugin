package com.github.mateigatin.loganalyzerplugin.model;

import com.github.mateigatin.loganalyzerplugin.model.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HttpStatusTest {

    @ParameterizedTest
    @CsvSource({
        "200, OK",
        "201, CREATED",
        "204, NO_CONTENT",
        "304, NOT_MODIFIED",
        "400, BAD_REQUEST",
        "401, UNAUTHORIZED",
        "403, FORBIDDEN",
        "404, NOT_FOUND",
        "413, REQUEST_TOO_LARGE",
        "500, INTERNAL_SERVER_ERROR",
        "502, BAD_GATEWAY",
        "503, SERVICE_UNAVAILABLE"
    })
    void testHttpStatusFromCode(int code, String expectedName) {
        HttpStatus status = HttpStatus.fromCode(code);
        assertEquals(code, status.getCode());
        assertEquals(expectedName, status.name());
    }

    @Test
    void testSuccessfulStatuses() {
        assertTrue(HttpStatus.OK.isSuccess());
        assertTrue(HttpStatus.CREATED.isSuccess());
        assertTrue(HttpStatus.NO_CONTENT.isSuccess());
        assertFalse(HttpStatus.NOT_FOUND.isSuccess());
        assertFalse(HttpStatus.INTERNAL_SERVER_ERROR.isSuccess());
    }

    @Test
    void testClientErrorStatuses() {
        assertTrue(HttpStatus.BAD_REQUEST.isClientError());
        assertTrue(HttpStatus.UNAUTHORIZED.isClientError());
        assertTrue(HttpStatus.FORBIDDEN.isClientError());
        assertTrue(HttpStatus.NOT_FOUND.isClientError());
        assertFalse(HttpStatus.OK.isClientError());
        assertFalse(HttpStatus.INTERNAL_SERVER_ERROR.isClientError());
    }

    @Test
    void testServerErrorStatuses() {
        assertTrue(HttpStatus.INTERNAL_SERVER_ERROR.isServerError());
        assertTrue(HttpStatus.BAD_GATEWAY.isServerError());
        assertTrue(HttpStatus.SERVICE_UNAVAILABLE.isServerError());
        assertFalse(HttpStatus.OK.isServerError());
        assertFalse(HttpStatus.NOT_FOUND.isServerError());
    }

    @Test
    void testUnknownStatusCode() {
        HttpStatus status = HttpStatus.fromCode(999);
        assertEquals(HttpStatus.UNKNOWN, status);
        assertEquals(0, status.getCode());
    }

    @Test
    void testNotModifiedStatus() {
        assertFalse(HttpStatus.NOT_MODIFIED.isSuccess());
        assertFalse(HttpStatus.NOT_MODIFIED.isClientError());
        assertFalse(HttpStatus.NOT_MODIFIED.isServerError());
    }
}
