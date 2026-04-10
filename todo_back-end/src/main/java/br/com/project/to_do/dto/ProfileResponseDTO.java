package br.com.project.to_do.dto;

import br.com.project.to_do.model.Member;

public record ProfileResponseDTO(
        long id,
        String email,
        String displayName,
        String photoDataUrl
) {
    public static ProfileResponseDTO fromEntity(Member member) {
        return new ProfileResponseDTO(
                member.getId(),
                member.getLogin(),
                member.getDisplayName(),
                member.getPhotoDataUrl()
        );
    }
}
