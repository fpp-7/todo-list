package br.com.project.to_do.service;

import br.com.project.to_do.dto.TokenRefreshResponseDTO;
import br.com.project.to_do.exception.InvalidTokenException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.model.RefreshToken;
import br.com.project.to_do.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenService secureTokenService;
    private final TokenService tokenService;

    @Value("${app.security.refresh-token.expiration-days:7}")
    private long expirationDays;

    @Transactional
    public String issueToken(Member member) {
        String rawToken = secureTokenService.generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(secureTokenService.hashToken(rawToken));
        refreshToken.setMember(member);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public TokenRefreshResponseDTO rotateToken(String rawRefreshToken) {
        RefreshToken currentToken = getActiveToken(rawRefreshToken);
        Member member = currentToken.getMember();

        currentToken.setRevokedAt(Instant.now());
        String nextRefreshToken = issueToken(member);
        String nextAccessToken = tokenService.generateToken(member);

        return new TokenRefreshResponseDTO(nextAccessToken, nextRefreshToken);
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(secureTokenService.hashToken(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeActiveTokens(Member member) {
        refreshTokenRepository.revokeActiveTokensByMemberId(member.getId(), Instant.now());
    }

    private RefreshToken getActiveToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(secureTokenService.hashToken(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalido ou expirado."));

        if (!refreshToken.isActive(Instant.now())) {
            throw new InvalidTokenException("Refresh token invalido ou expirado.");
        }

        return refreshToken;
    }
}
