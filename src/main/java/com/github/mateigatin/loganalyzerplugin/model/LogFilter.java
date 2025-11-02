package com.github.mateigatin.loganalyzerplugin.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.chrono.ChronoZonedDateTime;


public class LogFilter
{
    @Getter
    private String searchQuery;
    @Setter @Getter
    private LocalDateTime startDate;
    @Setter @Getter
    private LocalDateTime endDate;
    @Setter @Getter
    private Integer statusCodeFilter;

    public LogFilter()
    {
        this.searchQuery = "";
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery != null ? searchQuery : "";
    }

    public boolean hasFilters()
    {
        return !searchQuery.isEmpty() || startDate != null || endDate != null || statusCodeFilter != null;
    }

    public boolean matches(AbstractLogEntry entry)
    {
        // Check search query (matches IP, endpoint , or any text)
        if (!searchQuery.isEmpty())
        {
            String query = searchQuery.toLowerCase();
            boolean matchesQuery =
                    entry.getIpAddress().toLowerCase().contains(query) ||
                    entry.getEndpoint().toLowerCase().contains(query) ||
                    String.valueOf(entry.getHttpStatusCode().getCode()).contains(query);

            if (!matchesQuery)
            {
                return false;
            }
        }

        // check date range
        if (startDate != null && entry.getTimeStamp().isBefore(ChronoZonedDateTime.from(startDate)))
        {
            return false;
        }

        if (endDate != null && entry.getTimeStamp().isAfter(ChronoZonedDateTime.from(endDate)))
        {
            return false;
        }

        // Check status code
        return statusCodeFilter == null || entry.getHttpStatusCode().getCode() == statusCodeFilter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Active Filters: ");

        if (!searchQuery.isEmpty()) {
            sb.append("Search='").append(searchQuery).append("' ");
        }

        if (startDate != null) {
            sb.append("From=").append(startDate).append(" ");
        }

        if (endDate != null) {
            sb.append("To=").append(endDate).append(" ");
        }

        if (statusCodeFilter != null) {
            sb.append("Status=").append(statusCodeFilter).append(" ");
        }

        return sb.toString();
    }
}
