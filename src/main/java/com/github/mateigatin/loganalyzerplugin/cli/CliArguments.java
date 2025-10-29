package com.github.mateigatin.loganalyzerplugin.cli;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Builder
public class CliArguments {
    private final Path inputFile;
    private final Path outputDirectory;
    private final boolean streamMode;
    private final boolean jsonExport;
    private final boolean verbose;

    public static CliArguments parse(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        CliArgumentsBuilder builder = CliArguments.builder();
        builder.inputFile(Paths.get(args[0]));
        builder.outputDirectory(Paths.get("."));
        builder.streamMode(false);
        builder.jsonExport(true);
        builder.verbose(false);

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-o", "--output" -> {
                    if (i + 1 < args.length) {
                        builder.outputDirectory(Paths.get(args[++i]));
                    }
                }
                case "-s", "--stream" -> builder.streamMode(true);
                case "--no-json" -> builder.jsonExport(false);
                case "-v", "--verbose" -> builder.verbose(true);
                case "-h", "--help" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> System.err.println("Unknown option: " + args[i]);
            }
        }

        return builder.build();
    }

    private static void printUsage() {
        System.out.println("""
            Usage: java -jar LogAnalyzer.jar <log-file> [OPTIONS]
            
            OPTIONS:
              -o, --output <dir>     Output directory for reports (default: current directory)
              -s, --stream           Enable real-time streaming mode
              --no-json              Disable JSON export
              -v, --verbose          Enable verbose logging
              -h, --help             Show this help message
            
            EXAMPLES:
              # Basic analysis
              java -jar LogAnalyzer.jar access.log
           
              # Real-time monitoring
              java -jar LogAnalyzer.jar access.log --stream
            
              # Custom output directory
              java -jar LogAnalyzer.jar access.log -o ./reports
            """);
    }

}
