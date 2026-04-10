package br.com.project.to_do.controller;

import br.com.project.to_do.dto.AuthenticationDTO;
import br.com.project.to_do.dto.InviteRequestDTO;
import br.com.project.to_do.dto.LoginResponseDTO;
import br.com.project.to_do.dto.OperationStatusResponseDTO;
import br.com.project.to_do.dto.PasswordResetConfirmRequestDTO;
import br.com.project.to_do.dto.PasswordResetRequestDTO;
import br.com.project.to_do.dto.ProfileResponseDTO;
import br.com.project.to_do.dto.TokenRefreshRequestDTO;
import br.com.project.to_do.dto.TokenRefreshResponseDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.service.PasswordResetService;
import br.com.project.to_do.service.RefreshTokenService;
import br.com.project.to_do.service.TokenService;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        Member member = (Member) auth.getPrincipal();
        String token = tokenService.generateToken(member);
        String refreshToken = refreshTokenService.issueToken(member);

        log.info("Login realizado com sucesso para {}", member.getLogin());
        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                refreshToken,
                ProfileResponseDTO.fromEntity(member)
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDTO> refresh(@RequestBody @Valid TokenRefreshRequestDTO requestDTO) {
        return ResponseEntity.ok(refreshTokenService.rotateToken(requestDTO.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<OperationStatusResponseDTO> logout(@RequestBody @Valid TokenRefreshRequestDTO requestDTO) {
        refreshTokenService.revokeToken(requestDTO.refreshToken());
        return ResponseEntity.ok(new OperationStatusResponseDTO("SUCCESS", "Sessao encerrada com sucesso."));
    }

    @PostMapping("/register")
    public ResponseEntity<OperationStatusResponseDTO> register(@RequestBody @Valid AuthenticationDTO data) {
        repository.findByLogin(data.login()).ifPresent(existingUser -> {
            throw new BusinessRuleException("Ja existe uma conta cadastrada com este e-mail.");
        });

        Member member = new Member(
                data.login(),
                passwordEncoder.encode(data.password()),
                extractDisplayName(data.login())
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

    @PostMapping("/invite-request")
    public ResponseEntity<OperationStatusResponseDTO> inviteRequest(
            @RequestBody @Valid InviteRequestDTO requestDTO
    ) {
        log.info("Solicitacao de convite recebida para {} ({})", requestDTO.email(), requestDTO.company());
        return ResponseEntity.ok(new OperationStatusResponseDTO(
                "RECEIVED",
                "Solicitacao de convite registrada com sucesso."
        ));
    }

    private String extractDisplayName(String login) {
        String localPart = login.split("@")[0];
        String normalized = localPart.replace('.', ' ').replace('_', ' ').trim();
        return normalized.isBlank() ? login : normalized;
    }
}
