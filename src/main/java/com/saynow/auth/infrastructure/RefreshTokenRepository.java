// refresh token 해시를 조회하고 저장하는 JPA 저장소
package com.saynow.auth.infrastructure;

import com.saynow.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
