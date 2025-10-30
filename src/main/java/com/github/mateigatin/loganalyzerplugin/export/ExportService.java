package com.github.mateigatin.loganalyzerplugin.export;

import com.github.mateigatin.loganalyzerplugin.model.AnalysisResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ExportService
{
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Export all analysis results to JSON format
     */
    public void exportToJson(Map<String, AnalysisResult> results, Path outputPath) throws IOException
    {
        Map<String, Object> exportData = new HashMap<>();

        // Add metadata
        exportData.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        exportData.put("version", "1.0");
        exportData.put("tool", "LogAnalyzer Plugin");

        // Add all analysis results
        Map<String, Map<String, Object>> analysisData = new HashMap<>();
        results.forEach((key, result) -> {
            analysisData.put(key, result.getData());
        });
        exportData.put("analysis", analysisData);

        // Write to file
        try (FileWriter writer = new FileWriter(outputPath.toFile()))
        {
            GSON.toJson(exportData, writer);
        }
    }

    /**
     * Export all analysis results to HTML format
     */
    public void exportToHtml(Map<String, AnalysisResult> results, Path outputPath) throws IOException
    {
        StringBuilder html = new StringBuilder();

        // HTML Header with CSS
        html.append(getHtmlHeader());

        //Add each analysis section
        html.append("<div class='container'>");
        html.append("<h1>📊 Log Analysis Report</h1>");
        html.append("<p class='metadata'>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");

        //Overview Section
        if (results.containsKey("total"))
        {
            html.append(buildOverviewSection(results.get("total")));
        }

        // Traffic Section
        if (results.containsKey("traffic"))
        {
            html.append(buildTrafficSection(results.get("traffic")));
        }

        // Status Codes Section
        if (results.containsKey("statusCodes"))
        {
            html.append(buildStatusCodesSection(results.get("statusCodes")));
        }

        // Endpoints Section
        if (results.containsKey("endpoints"))
        {
            html.append(buildEndpointsSection(results.get("endpoints")));
        }

        // Performance Section
        if (results.containsKey("performance"))
        {
            html.append(buildPerformanceSection(results.get("performance")));
        }

        // Security Section
        if (results.containsKey("security")) {
            html.append(buildSecuritySection(results.get("security")));
        }

        html.append("</div>");
        html.append(getHtmlFooter());

        // Write to file
        try (FileWriter writer = new FileWriter(outputPath.toFile()))
        {
            writer.write(html.toString());
        }
    }

    private String getHtmlHeader() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Log Analysis Report</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 20px;
                        color: #333;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 10px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        padding: 40px;
                    }
                    h1 {
                        color: #667eea;
                        margin-bottom: 10px;
                        font-size: 2.5em;
                    }
                    h2 {
                        color: #764ba2;
                        margin-top: 30px;
                        margin-bottom: 15px;
                        padding-bottom: 10px;
                        border-bottom: 2px solid #f0f0f0;
                    }
                    .metadata {
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 0.9em;
                    }
                    .section {
                        margin-bottom: 40px;
                    }
                    .metric {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 5px;
                        margin: 10px 0;
                        border-left: 4px solid #667eea;
                    }
                    .metric-label {
                        font-weight: 600;
                        color: #555;
                    }
                    .metric-value {
                        font-size: 1.5em;
                        color: #667eea;
                        font-weight: bold;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 20px 0;
                    }
                    th, td {
                        padding: 12px;
                        text-align: left;
                        border-bottom: 1px solid #e0e0e0;
                    }
                    th {
                        background: #667eea;
                        color: white;
                        font-weight: 600;
                    }
                    tr:hover {
                        background: #f8f9fa;
                    }
                    .bar-chart {
                        margin: 20px 0;
                    }
                    .bar {
                        background: linear-gradient(90deg, #667eea, #764ba2);
                        height: 30px;
                        border-radius: 5px;
                        margin: 5px 0;
                        display: flex;
                        align-items: center;
                        padding: 0 10px;
                        color: white;
                        font-weight: 600;
                    }
                    .success { border-left-color: #28a745; }
                    .warning { border-left-color: #ffc107; }
                    .error { border-left-color: #dc3545; }
                    .info { border-left-color: #17a2b8; }
                </style>
            </head>
            <body>
            """;
    }

    private String getHtmlFooter() {
        return """
            <footer style="margin-top: 40px; padding-top: 20px; border-top: 2px solid #f0f0f0; text-align: center; color: #666;">
                <p>Generated by LogAnalyzer Plugin for IntelliJ IDEA</p>
            </footer>
            </body>
            </html>
            """;
    }

    private String buildOverviewSection(AnalysisResult result)
    {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>📊 Overview</h2>");

        if (data.containsKey("Total"))
        {
            sb.append("<div class='metric success'>");
            sb.append("<div class='metric-label'>Total Requests</div>");
            sb.append("<div class='metric-value'>").append(data.get("Total")).append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String buildTrafficSection(AnalysisResult result)
    {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>📈 Traffic by Hour</h2>");
        sb.append("<div class='bar-chart'>");

        // Group by hour and create bars
        Map<Integer, Long> hourlyTraffic = new HashMap<>();
        data.forEach((key, value) ->
        {
            try
            {
                String hourStr = key.split(" ")[1].split(":")[0];
                int hour = Integer.parseInt(hourStr);
                hourlyTraffic.merge(hour, (Long) value, Long::sum);
            } catch (Exception ignored) {}
        });

        if (!hourlyTraffic.isEmpty())
        {
            long max = hourlyTraffic.values().stream().max(Long::compare).orElse(1L);
            hourlyTraffic.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                    {
                        int hour = entry.getKey();
                        long count = entry.getValue();
                        int width = (int) ((count * 100.0) / max);
                        sb.append(String.format("<div class='bar' style='width: %d%%;'>%02d:00 - %d requests</div>",
                                width, hour, count));
                    });
        }

        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildStatusCodesSection(AnalysisResult result)
    {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>🔢 Status Codes</h2>");
        sb.append("<table>");
        sb.append("<thead><tr><th>Status Code</th><th>Count</th><th>Percentage</th></tr></thead>");
        sb.append("<tbody>");

        long total = data.values().stream().mapToLong(v -> (Long) v).sum();
        data.entrySet().stream()
                .sorted((a, b) -> Long.compare((Long) b.getValue(), (Long)a.getValue()))
                .forEach(entry ->
                {
                    String code = entry.getKey();
                    Long count = (Long) entry.getValue();
                    double percentage = 100.0 * count / total;
                    sb.append(String.format("<tr><td>%s</td><td>%d</td><td>%.1f%%</td></tr>",
                            code, count, percentage));
                });

        sb.append("</tbody>");
        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildEndpointsSection(AnalysisResult result)
    {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>🎯 Top Endpoints</h2>");
        sb.append("<table>");
        sb.append("<thead><tr><th>Rank</th><th>Endpoint</th><th>Requests</th></tr></thead>");
        sb.append("<tbody>");

        int rank = 1;
        for (Map.Entry<String, Object> entry : data.entrySet())
        {
//            if (!(entry.getValue() instanceof Long))
//            {
//                return "";
//            }

            sb.append(String.format("<tr><td>#%d</td><td>%s</td><td>%d</td></tr>",
                    rank++, entry.getKey(), entry.getValue()));
        }

        sb.append("</tbody>");
        sb.append("</table>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildPerformanceSection(AnalysisResult result) {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>⚡ Performance</h2>");

        if (data.containsKey("averageResponseSize")) {
            double avgKB = (Double) data.get("averageResponseSize") / 1024.0;
            sb.append("<div class='metric info'>");
            sb.append("<div class='metric-label'>Average Response Size</div>");
            sb.append(String.format("<div class='metric-value'>%.2f KB</div>", avgKB));
            sb.append("</div>");
        }

        if (data.containsKey("totalDataTransferred")) {
            double totalMB = (Double) data.get("totalDataTransferred") / (1024.0 * 1024.0);
            sb.append("<div class='metric info'>");
            sb.append("<div class='metric-label'>Total Data Transferred</div>");
            sb.append(String.format("<div class='metric-value'>%.2f MB</div>", totalMB));
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildSecuritySection(AnalysisResult result) {
        Map<String, Object> data = result.getData();
        StringBuilder sb = new StringBuilder();

        sb.append("<div class='section'>");
        sb.append("<h2>🔒 Security</h2>");

        if (data.containsKey("suspiciousRequestCount")) {
            int count = (Integer) data.get("suspiciousRequestCount");
            String cssClass = count > 0 ? "error" : "success";
            sb.append(String.format("<div class='metric %s'>", cssClass));
            sb.append("<div class='metric-label'>Suspicious Requests</div>");
            sb.append(String.format("<div class='metric-value'>%d</div>", count));
            sb.append("</div>");
        }

        if (data.containsKey("topOffendingIPs")) {

            Map<String, Long> ips = (Map<String, Long>) data.get("topOffendingIPs");
            if (!ips.isEmpty()) {
                sb.append("<h3>Top Offending IPs</h3>");
                sb.append("<table>");
                sb.append("<thead><tr><th>IP Address</th><th>Failed Attempts</th></tr></thead>");
                sb.append("<tbody>");
                ips.forEach((ip, count) -> {
                    sb.append(String.format("<tr><td>%s</td><td>%d</td></tr>", ip, count));
                });
                sb.append("</tbody>");
                sb.append("</table>");
            }
        }

        sb.append("</div>");
        return sb.toString();
    }

}
