package br.com.project.to_do.dto;

import java.time.LocalDate;

public record TaskListQueryDTO(
        Integer page,
        Integer size,
        String query,
        String category,
        String priority,
        Boolean done,
        String status,
        LocalDate dueDateFrom,
        LocalDate dueDateTo
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public boolean hasAdvancedQuery() {
        return page != null || size != null || hasFilters();
    }

    public int resolvedPage() {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    public int resolvedSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    public String normalizedQuery() {
        return normalize(query);
    }

    public String normalizedCategory() {
        return normalize(category);
    }

    public String normalizedPriority() {
        return normalize(priority);
    }

    public TaskStatusFilter resolvedStatus() {
        return TaskStatusFilter.from(status);
    }

    private boolean hasFilters() {
        return normalizedQuery() != null
                || normalizedCategory() != null
                || normalizedPriority() != null
                || done != null
                || resolvedStatus() != null
                || dueDateFrom != null
                || dueDateTo != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
