package br.com.project.to_do.controller;

import br.com.project.to_do.dto.AccessActionResponse;
import br.com.project.to_do.dto.InviteRequest;
import br.com.project.to_do.dto.PasswordResetRequest;
import br.com.project.to_do.service.AccessRequestService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://127.0.0.1:4200", "http://localhost:4200"})
@RequestMapping("/auth")
public class AccessController {

    private final AccessRequestService accessRequestService;

    public AccessController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping("/esqueci-senha")
    public AccessActionResponse forgotPassword(@RequestBody PasswordResetRequest request) {
        return accessRequestService.solicitarRecuperacaoSenha(request);
    }

    @PostMapping("/solicitar-convite")
    public AccessActionResponse requestInvite(@RequestBody InviteRequest request) {
        return accessRequestService.solicitarConvite(request);
    }
}
