package br.com.project.to_do.dto;

import jakarta.validation.constraints.Size;

public record UpdatePhotoRequestDTO(
        @Size(max = 2000000, message = "A foto enviada excede o tamanho máximo permitido.")
        String photoDataUrl
) {
}
