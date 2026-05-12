// refresh token 해시를 조회하고 저장하는 JPA 저장소
package com.saynow.auth.infrastructure;

import com.saynow.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.member.id = :memberId
              and token.revokedAt is null
            """)
    void revokeAllActiveByMemberId(@Param("memberId") Long memberId, @Param("revokedAt") LocalDateTime revokedAt);
}
