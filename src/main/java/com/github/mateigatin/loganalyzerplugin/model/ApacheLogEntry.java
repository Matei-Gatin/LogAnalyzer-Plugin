package com.github.mateigatin.loganalyzerplugin.model;

import lombok.Getter;
import lombok.ToString;

import java.time.ZonedDateTime;

@Getter
public class ApacheLogEntry extends AbstractLogEntry
{

    // 127.0.0.1 - Scott [10/Dec/2019:13:55:36 -0700] "GET /server-status HTTP/1.1" 200 2326


    public ApacheLogEntry(String ipAddress, String userId, ZonedDateTime timeStamp,
                          String requestType, String endpoint, HttpStatus httpStatusCode, long responseSize) {
        super(ipAddress, userId, timeStamp, requestType, endpoint, httpStatusCode, responseSize);
    }

    @Override
    public String getLogType()
    {
        return "Apache";
    }

    @Override
    public String toString() {
        return "ApacheLogEntry{" +
                "endpoint='" + endpoint + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                ", ipAddress='" + ipAddress + '\'' +
                ", requestType='" + requestType + '\'' +
                ", responseSize=" + responseSize +
                ", timeStamp=" + timeStamp +
                ", userId='" + userId + '\'' +
                '}';
    }
}
