package br.com.project.to_do.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequestDTO(
        @NotBlank(message = "Token e obrigatorio.")
        String token,

        @NotBlank(message = "Nova senha e obrigatoria.")
        @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres.")
        String newPassword,

        @NotBlank(message = "Confirmacao de senha e obrigatoria.")
        String confirmPassword
) {
}
