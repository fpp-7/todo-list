package br.com.project.to_do.dto;

import lombok.Data;

public record AuthenticationDTO(String login, String password) {
}
