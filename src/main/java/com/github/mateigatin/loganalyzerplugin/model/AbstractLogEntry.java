package com.github.mateigatin.loganalyzerplugin.model;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

@Getter
public abstract class AbstractLogEntry {
    protected String ipAddress;
    protected String userId;
    protected ZonedDateTime timeStamp;
    protected String requestType;
    protected String endpoint;
    protected HttpStatus httpStatusCode;
    protected long responseSize;

    public AbstractLogEntry(String ipAddress, String userId, ZonedDateTime timeStamp,
                            String requestType, String endpoint, HttpStatus httpStatusCode,
                            long responseSize)
    {
        this.ipAddress = Objects.requireNonNull(ipAddress, "IP address cannot be null");
        this.userId = userId;
        this.timeStamp = Objects.requireNonNull(timeStamp, "Timestamp cannot be null");
        this.requestType = requestType;
        this.endpoint = endpoint; //
        this.httpStatusCode = httpStatusCode;
        this.responseSize = responseSize;
    }

    protected abstract String getLogType();

    public boolean isSuccessful()
    {
        return httpStatusCode.isSuccess();
    }

    public boolean isClientError()
    {
        return httpStatusCode.isClientError();
    }

    public boolean isServerError()
    {
        return httpStatusCode.isServerError();
    }
}
