package com.github.mateigatin.loganalyzerplugin.model;

import lombok.Getter;
import lombok.ToString;
import com.github.mateigatin.loganalyzerplugin.analyzer.LogAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Objects;

@Getter
@ToString
public class AnalysisResult {
    private final String analyzerName;
    private final Map<String, Object> data;
    private final String summary;

    public AnalysisResult(String analyzerName, Map<String, Object> data, String summary) {
        this.analyzerName = analyzerName;
        this.data = data;
        this.summary = summary;
    }


    public void createAnalysis(String path)
    {
        Path createPath = Path.of(path);

        try {
            Files.createFile(createPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter br = Files.newBufferedWriter(createPath, StandardOpenOption.TRUNCATE_EXISTING))
        {
            br.write("=== " + analyzerName + " ===\n");
             for (Map.Entry<String, Object> entry : data.entrySet())
             {
                 String message = """
                         %s: %s requests
                         """.formatted(entry.getKey(), String.valueOf(entry.getValue()));
                 br.write(message);
             }
        } catch (IOException e)
        {
            System.err.println("Exception: " + e.getMessage());
        }
    }
}
