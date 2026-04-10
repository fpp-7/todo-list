package br.com.project.to_do.service;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    public void sendResetLink(String email, String token) {
        String resetLink = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP nao configurado. Link de reset para {}: {}", email, resetLink);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Recuperacao de senha - Todo List");
            message.setText("""
                    Recebemos uma solicitacao para redefinir sua senha.

                    Acesse o link abaixo para criar uma nova senha:
                    %s

                    Se voce nao solicitou isso, ignore este e-mail.
                    """.formatted(resetLink));

            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Falha ao enviar e-mail de reset de senha para {}", email, exception);
        }
    }
}
