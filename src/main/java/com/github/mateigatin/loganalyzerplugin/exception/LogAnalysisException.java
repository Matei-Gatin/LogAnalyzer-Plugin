package com.github.mateigatin.loganalyzerplugin.exception;

public class LogAnalysisException extends Exception
{
    public LogAnalysisException(String message)
    {
        super(message);
    }

    public LogAnalysisException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
