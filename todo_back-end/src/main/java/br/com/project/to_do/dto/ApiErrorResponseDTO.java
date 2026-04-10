package br.com.project.to_do.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResponseDTO> fieldErrors
) {
}
