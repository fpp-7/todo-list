package br.com.project.to_do.controller;

import br.com.project.to_do.dto.AuthenticationDTO;
import br.com.project.to_do.dto.LoginResponseDTO;
import br.com.project.to_do.dto.OperationStatusResponseDTO;
import br.com.project.to_do.dto.PasswordResetConfirmRequestDTO;
import br.com.project.to_do.dto.PasswordResetRequestDTO;
import br.com.project.to_do.dto.ProfileResponseDTO;
import br.com.project.to_do.dto.RegisterRequestDTO;
import br.com.project.to_do.dto.TokenRefreshRequestDTO;
import br.com.project.to_do.dto.TokenRefreshResponseDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.service.AuthCookieService;
import br.com.project.to_do.service.PasswordResetService;
import br.com.project.to_do.service.RefreshTokenService;
import br.com.project.to_do.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final MemberRepository repository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid AuthenticationDTO data,
            HttpServletResponse response
    ) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        Member member = (Member) auth.getPrincipal();
        String token = tokenService.generateToken(member);
        String refreshToken = refreshTokenService.issueToken(member);

        authCookieService.addAuthCookies(response, token, refreshToken);
        log.info("Login realizado com sucesso para {}", member.getLogin());
        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                refreshToken,
                ProfileResponseDTO.fromEntity(member)
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDTO> refresh(
            @RequestBody(required = false) TokenRefreshRequestDTO requestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(requestDTO, request);
        TokenRefreshResponseDTO tokens = refreshTokenService.rotateToken(refreshToken);
        authCookieService.addAuthCookies(response, tokens.token(), tokens.refreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<OperationStatusResponseDTO> logout(
            @RequestBody(required = false) TokenRefreshRequestDTO requestDTO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        resolveOptionalRefreshToken(requestDTO, request).ifPresent(refreshTokenService::revokeToken);
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.ok(new OperationStatusResponseDTO("SUCCESS", "Sessao encerrada com sucesso."));
    }

    @PostMapping("/register")
    public ResponseEntity<OperationStatusResponseDTO> register(@RequestBody @Valid RegisterRequestDTO data) {
        repository.findByLogin(data.login()).ifPresent(existingUser -> {
            throw new BusinessRuleException("Ja existe uma conta cadastrada com este e-mail.");
        });

        Member member = new Member(
                data.login(),
                passwordEncoder.encode(data.password()),
                data.displayName()
        );
        repository.save(member);

        log.info("Novo membro registrado: {}", member.getLogin());
        return ResponseEntity.ok(new OperationStatusResponseDTO("SUCCESS", "Conta criada com sucesso."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OperationStatusResponseDTO> forgotPassword(
            @RequestBody @Valid PasswordResetRequestDTO requestDTO
    ) {
        log.info("Solicitacao de recuperacao de senha recebida para {}", requestDTO.email());
        passwordResetService.requestPasswordReset(requestDTO.email());
        return ResponseEntity.ok(new OperationStatusResponseDTO(
                "RECEIVED",
                "Solicitacao recebida. Verifique seu e-mail para os proximos passos."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<OperationStatusResponseDTO> resetPassword(
            @RequestBody @Valid PasswordResetConfirmRequestDTO requestDTO
    ) {
        passwordResetService.resetPassword(requestDTO);
        return ResponseEntity.ok(new OperationStatusResponseDTO("SUCCESS", "Senha redefinida com sucesso."));
    }

    private String resolveRefreshToken(TokenRefreshRequestDTO requestDTO, HttpServletRequest request) {
        return resolveOptionalRefreshToken(requestDTO, request)
                .orElseThrow(() -> new br.com.project.to_do.exception.InvalidTokenException(
                        "Refresh token invalido ou expirado."
                ));
    }

    private java.util.Optional<String> resolveOptionalRefreshToken(
            TokenRefreshRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        if (requestDTO != null && requestDTO.refreshToken() != null && !requestDTO.refreshToken().isBlank()) {
            return java.util.Optional.of(requestDTO.refreshToken());
        }

        return authCookieService.resolveRefreshToken(request);
    }
}
