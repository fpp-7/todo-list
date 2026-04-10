package br.com.project.to_do.service;

import br.com.project.to_do.dto.UpdatePasswordRequestDTO;
import br.com.project.to_do.dto.UpdatePhotoRequestDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.model.Member;
import br.com.project.to_do.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ProfilePhotoStorageService profilePhotoStorageService;

    public Member getProfile(Member member) {
        return member;
    }

    public Member updatePassword(Member member, UpdatePasswordRequestDTO requestDTO) {
        if (!passwordEncoder.matches(requestDTO.currentPassword(), member.getPassword())) {
            throw new BusinessRuleException("A senha atual informada está incorreta.");
        }

        if (!requestDTO.newPassword().equals(requestDTO.confirmPassword())) {
            throw new BusinessRuleException("A confirmação da nova senha não confere.");
        }

        member.setPassword(passwordEncoder.encode(requestDTO.newPassword()));
        refreshTokenService.revokeActiveTokens(member);
        log.info("Senha atualizada para o membro {}", member.getId());
        return memberRepository.save(member);
    }

    public Member updatePhoto(Member member, UpdatePhotoRequestDTO requestDTO) {
        String normalizedPhoto = normalize(requestDTO.photoDataUrl());
        member.setPhotoDataUrl(
                normalizedPhoto == null
                        ? null
                        : profilePhotoStorageService.storeProfilePhoto(member.getId(), normalizedPhoto)
        );
        log.info("Foto de perfil atualizada para o membro {}", member.getId());
        return memberRepository.save(member);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
