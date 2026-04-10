package br.com.project.to_do.controller;

import br.com.project.to_do.dto.OperationStatusResponseDTO;
import br.com.project.to_do.dto.ProfileResponseDTO;
import br.com.project.to_do.dto.UpdatePasswordRequestDTO;
import br.com.project.to_do.dto.UpdatePhotoRequestDTO;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponseDTO getProfile(@AuthenticationPrincipal Member member) {
        return ProfileResponseDTO.fromEntity(profileService.getProfile(member));
    }

    @PutMapping("/password")
    public OperationStatusResponseDTO updatePassword(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid UpdatePasswordRequestDTO requestDTO
    ) {
        profileService.updatePassword(member, requestDTO);
        return new OperationStatusResponseDTO("SUCCESS", "Senha atualizada com sucesso.");
    }

    @PutMapping("/photo")
    public ProfileResponseDTO updatePhoto(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid UpdatePhotoRequestDTO requestDTO
    ) {
        return ProfileResponseDTO.fromEntity(profileService.updatePhoto(member, requestDTO));
    }
}
