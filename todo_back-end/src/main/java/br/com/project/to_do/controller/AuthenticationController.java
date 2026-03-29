package br.com.project.to_do.controller;

import br.com.project.to_do.dto.AuthenticationDTO;
import br.com.project.to_do.dto.LoginResponseDTO;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.repository.MemberRepository;
import br.com.project.to_do.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MemberRepository repository;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data) {
        // 1. Encapsula as credenciais enviadas pelo usuário
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());

        // 2. Autentica (O Spring vai usar seu AuthorizationService/UserDetailsService aqui)
        var auth = this.authenticationManager.authenticate(usernamePassword);

        // 3. Gera o Token JWT se a senha estiver correta
        var token = tokenService.generateToken((Member)auth.getPrincipal());

        // 4. Retorna o token na resposta
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid AuthenticationDTO data) {
        if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        Member member = new Member(data.login(), encryptedPassword);

        this.repository.save(member);

        return ResponseEntity.ok().build();
    }

}
