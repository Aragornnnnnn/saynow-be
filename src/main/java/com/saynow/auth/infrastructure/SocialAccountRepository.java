// 소셜 계정 연결 정보를 조회하고 저장하는 JPA 저장소
package com.saynow.auth.infrastructure;

import com.saynow.auth.domain.SocialAccount;
import com.saynow.auth.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderSubject(SocialProvider provider, String providerSubject);

    @Modifying
    @Query("delete from SocialAccount account where account.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}
