package br.com.project.to_do.dto;

import br.com.project.to_do.exception.BusinessRuleException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public enum TaskStatusFilter {
    HOJE(Set.of("hoje", "today")),
    EM_ANDAMENTO(Set.of("em_andamento", "em_andamentos", "in_progress", "in_progresses")),
    PLANEJADA(Set.of("planejada", "planejadas", "planned")),
    CONCLUIDA(Set.of("concluida", "concluidas", "done", "completed"));

    private final Set<String> aliases;

    TaskStatusFilter(Set<String> aliases) {
        this.aliases = aliases;
    }

    public static TaskStatusFilter from(String rawStatus) {
        String normalizedStatus = normalize(rawStatus);

        if (normalizedStatus == null) {
            return null;
        }

        for (TaskStatusFilter statusFilter : values()) {
            if (statusFilter.aliases.contains(normalizedStatus)) {
                return statusFilter;
            }
        }

        throw new BusinessRuleException(
                "Filtro de status inválido. Use: hoje, em_andamento, planejada ou concluida."
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isBlank()) {
            return null;
        }

        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
