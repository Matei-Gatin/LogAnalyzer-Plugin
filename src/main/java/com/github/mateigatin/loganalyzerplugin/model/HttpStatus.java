package com.github.mateigatin.loganalyzerplugin.model;

import lombok.Getter;

@Getter
public enum HttpStatus {
    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    NOT_MODIFIED(304, "Not Modified"),

    // 4xx Client Errors
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    REQUEST_TOO_LARGE(413, "Request Entity Too Large"),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    UNKNOWN(0, "Unknown Status");


    private final int code;
    private final String description;

    HttpStatus(int code, String description)
    {
        this.code = code;
        this.description = description;
    }

    public static HttpStatus fromCode(int code)
    {
        for (HttpStatus status : values())
        {
            if (status.code == code)
            {
                return status;
            }
        }

        return UNKNOWN;
    }

    public boolean isSuccess()
    {
        return code >= 200 && code < 300;
    }

    public boolean isClientError()
    {
        return code >= 400 && code < 500;
    }

    public boolean isServerError()
    {
        return code >= 500 && code < 600;
    }
}
