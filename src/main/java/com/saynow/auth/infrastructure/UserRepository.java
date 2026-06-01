// SayNow 사용자 엔티티를 조회하고 저장하는 JPA 저장소
package com.saynow.auth.infrastructure;

import com.saynow.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndSubAndDeletedAtIsNull(com.saynow.auth.domain.SocialProvider provider, String sub);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);
}
