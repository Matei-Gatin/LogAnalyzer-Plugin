package com.github.mateigatin.loganalyzerplugin.config;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public class AnalyzerConfig
{
    @Builder.Default
    private final int topEndpointsLimit = 5;

    @Builder.Default
    private final String outputFormat = "txt";

    private final Path inputFilePath;
    private final Path outputFilePath;

    @Builder.Default
    private final boolean includeTimestamps = true;
}
