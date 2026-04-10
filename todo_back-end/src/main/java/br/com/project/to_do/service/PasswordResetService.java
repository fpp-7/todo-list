package br.com.project.to_do.service;

import br.com.project.to_do.dto.PasswordResetConfirmRequestDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.PasswordResetToken;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.repository.PasswordResetTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokenService;
    private final PasswordResetMailService passwordResetMailService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Transactional
    public void requestPasswordReset(String email) {
        memberRepository.findByLogin(email).ifPresent(member -> {
            String rawToken = secureTokenService.generateOpaqueToken();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setTokenHash(secureTokenService.hashToken(rawToken));
            resetToken.setMember(member);
            resetToken.setCreatedAt(Instant.now());
            resetToken.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);

            passwordResetMailService.sendResetLink(member.getLogin(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequestDTO requestDTO) {
        if (!requestDTO.newPassword().equals(requestDTO.confirmPassword())) {
            throw new BusinessRuleException("A confirmacao de senha deve ser igual a nova senha.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(secureTokenService.hashToken(requestDTO.token()))
                .orElseThrow(() -> new BusinessRuleException("Token de recuperacao invalido ou expirado."));

        if (!resetToken.isActive(Instant.now())) {
            throw new BusinessRuleException("Token de recuperacao invalido ou expirado.");
        }

        Member member = resetToken.getMember();
        member.setPassword(passwordEncoder.encode(requestDTO.newPassword()));
        resetToken.setUsedAt(Instant.now());
        refreshTokenService.revokeActiveTokens(member);
    }
}
