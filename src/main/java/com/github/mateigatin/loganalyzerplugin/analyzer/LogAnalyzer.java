package com.github.mateigatin.loganalyzerplugin.analyzer;

import com.github.mateigatin.loganalyzerplugin.model.AbstractLogEntry;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.util.List;

public interface LogAnalyzer
{
    AnalysisResult analyze(List<AbstractLogEntry> entries);
    String getAnalyzerName();
}
