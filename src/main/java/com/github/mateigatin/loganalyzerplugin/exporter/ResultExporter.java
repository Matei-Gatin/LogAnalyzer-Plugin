package com.github.mateigatin.loganalyzerplugin.exporter;

import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ResultExporter
{
    void export(List<AnalysisResult> results, Path outputPath) throws IOException;
}
