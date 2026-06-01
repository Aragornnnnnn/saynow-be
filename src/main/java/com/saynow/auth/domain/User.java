// SayNow 사용자 인증 식별자와 기본 프로필을 저장하는 엔티티
package com.saynow.auth.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_provider_sub", columnNames = {"provider", "sub"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nickname;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String sub;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SocialProvider provider;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public User(String nickname, String email, String sub, SocialProvider provider) {
        this.nickname = nickname;
        this.email = email;
        this.sub = sub;
        this.provider = provider;
    }

    public void updateProfile(String nickname, String email) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (email != null) {
            this.email = email;
        }
    }

    public void withdraw(LocalDateTime deletedAt) {
        this.nickname = null;
        this.email = null;
        this.sub = null;
        this.provider = null;
        this.deletedAt = deletedAt;
    }
}
