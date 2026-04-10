package br.com.project.to_do.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String login,
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
        String password
) {
}
