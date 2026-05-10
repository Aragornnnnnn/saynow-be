// 외부 OIDC 계정과 SayNow 회원의 연결을 저장하는 엔티티
package com.saynow.auth.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_social_accounts_provider_subject", columnNames = {"provider", "provider_subject"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(length = 255)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(length = 100)
    private String nickname;

    public SocialAccount(Member member, SocialProvider provider, String providerSubject, String email, Boolean emailVerified, String nickname) {
        this.member = member;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.emailVerified = emailVerified;
        this.nickname = nickname;
    }

    public void updateProfile(String email, Boolean emailVerified, String nickname) {
        if (email != null) {
            this.email = email;
        }
        if (emailVerified != null) {
            this.emailVerified = emailVerified;
        }
        if (nickname != null) {
            this.nickname = nickname;
        }
    }
}
