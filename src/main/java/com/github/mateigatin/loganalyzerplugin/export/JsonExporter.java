package com.github.mateigatin.loganalyzerplugin.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class JsonExporter implements ResultExporter
{
    private final ObjectMapper mapper;

    public JsonExporter()
    {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void export(List<AnalysisResult> results, Path outputPath) throws IOException
    {
        String json = mapper.writeValueAsString(results);
        Files.writeString(outputPath, json);
        log.info("Exported analysis to JSON: {}", outputPath);
    }
}
