package br.com.project.to_do.repository;

import br.com.project.to_do.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.member.id = :memberId
               and token.revokedAt is null
            """)
    int revokeActiveTokensByMemberId(
            @Param("memberId") long memberId,
            @Param("revokedAt") Instant revokedAt
    );
}
