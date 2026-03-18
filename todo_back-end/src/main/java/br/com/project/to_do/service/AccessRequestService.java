package br.com.project.to_do.service;

import br.com.project.to_do.dto.access.AccessActionResponse;
import br.com.project.to_do.dto.access.InviteRequest;
import br.com.project.to_do.dto.access.PasswordResetRequest;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessRequestService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public AccessActionResponse solicitarRecuperacaoSenha(PasswordResetRequest request) {
        String email = normalizeRequired(
                request.email(),
                "Informe um e-mail para recuperar a senha."
        );

        validateEmail(email, "Informe um e-mail v\u00E1lido para recuperar a senha.");

        return new AccessActionResponse(
                "RECEBIDO",
                "Solicita\u00E7\u00E3o de recupera\u00E7\u00E3o recebida para " + email + "."
        );
    }

    public AccessActionResponse solicitarConvite(InviteRequest request) {
        String name = normalizeRequired(
                request.name(),
                "Informe seu nome para solicitar o convite."
        );
        String email = normalizeRequired(
                request.email(),
                "Informe um e-mail para solicitar o convite."
        );
        String company = normalizeOptional(request.company());

        validateEmail(email, "Informe um e-mail v\u00E1lido para solicitar o convite.");

        String message = company == null
                ? "Solicita\u00E7\u00E3o de convite registrada para " + name + " (" + email + ")."
                : "Solicita\u00E7\u00E3o de convite registrada para " + name + " (" + email
                        + ") da empresa " + company + ".";

        return new AccessActionResponse("RECEBIDO", message);
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateEmail(String email, String errorMessage) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }
}
