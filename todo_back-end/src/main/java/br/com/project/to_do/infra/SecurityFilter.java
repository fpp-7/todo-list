package br.com.project.to_do.infra;

import br.com.project.to_do.exception.InvalidTokenException;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.service.AuthCookieService;
import br.com.project.to_do.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final MemberRepository memberRepository;
    private final AuthCookieService authCookieService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = recoverToken(request);

        if (token != null) {
            try {
                String login = tokenService.validateToken(token);
                memberRepository.findByLogin(login).ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (InvalidTokenException exception) {
                log.debug("Requisição recebida com token inválido para {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return authCookieService.resolveAccessToken(request).orElse(null);
        }

        return authHeader.replace("Bearer ", "").trim();
    }
}
