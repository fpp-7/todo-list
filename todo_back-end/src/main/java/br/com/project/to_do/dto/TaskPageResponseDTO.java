package br.com.project.to_do.dto;

import br.com.project.to_do.model.Task;
import java.util.List;
import org.springframework.data.domain.Page;

public record TaskPageResponseDTO(
        List<TaskResponseDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static TaskPageResponseDTO fromPage(Page<Task> tasks) {
        return new TaskPageResponseDTO(
                tasks.getContent().stream().map(TaskResponseDTO::fromEntity).toList(),
                tasks.getNumber(),
                tasks.getSize(),
                tasks.getTotalElements(),
                tasks.getTotalPages(),
                tasks.hasNext(),
                tasks.hasPrevious()
        );
    }
}
