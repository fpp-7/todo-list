package br.com.project.to_do.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank(message = "O nome e obrigatorio.")
        @Size(max = 60, message = "O nome deve ter no maximo 60 caracteres.")
        String firstName,

        @NotBlank(message = "O sobrenome e obrigatorio.")
        @Size(max = 80, message = "O sobrenome deve ter no maximo 80 caracteres.")
        String lastName,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "Informe um e-mail valido.")
        String login,

        @NotBlank(message = "A senha e obrigatoria.")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
        String password
) {
    public String displayName() {
        return (firstName.trim() + " " + lastName.trim()).replaceAll("\\s+", " ");
    }
}
