package br.com.project.to_do.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequestDTO(
        @NotBlank(message = "Refresh token e obrigatorio.")
        String refreshToken
) {
}
