package br.com.project.to_do.dto;

import br.com.project.to_do.model.Task;
import java.time.LocalDate;

public record TaskResponseDTO(
        long id,
        String name,
        String description,
        String category,
        String priority,
        LocalDate dueDate,
        boolean done
) {
    public static TaskResponseDTO fromEntity(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getNameTask(),
                task.getDescription(),
                task.getCategory(),
                task.getPriority(),
                task.getDueDate(),
                task.isDone()
        );
    }
}
