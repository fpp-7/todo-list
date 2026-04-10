package br.com.project.to_do.dto;

import jakarta.validation.constraints.Size;

public record UpdatePhotoRequestDTO(
        @Size(max = 2800000, message = "A foto enviada excede o tamanho maximo permitido.")
        String photoDataUrl
) {
}
