package br.com.project.to_do.service;

import br.com.project.to_do.exception.InvalidTokenException;
import br.com.project.to_do.model.Member;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-minutes:15}")
    private long expirationMinutes;

    public String generateToken(Member member) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(member.getLogin())
                    .withExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new InvalidTokenException("Não foi possível gerar o token de autenticação.");
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            log.warn("Token JWT inválido ou expirado.");
            throw new InvalidTokenException("Token de autenticação inválido ou expirado.");
        }
    }
}
