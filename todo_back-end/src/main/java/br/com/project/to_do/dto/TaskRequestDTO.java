package br.com.project.to_do.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequestDTO(
        @NotBlank(message = "O nome da tarefa é obrigatório.")
        @Size(max = 140, message = "O nome da tarefa deve ter no máximo 140 caracteres.")
        String name,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,
        @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres.")
        String category,
        @NotBlank(message = "A prioridade é obrigatória.")
        String priority,
        LocalDate dueDate,
        @NotNull(message = "O status de conclusão é obrigatório.")
        Boolean done
) {
}
