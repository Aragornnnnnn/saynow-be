// SayNow 회원 엔티티를 조회하고 저장하는 JPA 저장소
package com.saynow.auth.infrastructure;

import com.saynow.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndWithdrawnAtIsNull(Long id);

    boolean existsByIdAndWithdrawnAtIsNull(Long id);
}
