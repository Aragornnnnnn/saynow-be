// SayNow 회원의 기본 프로필 정보를 저장하는 엔티티
package com.saynow.auth.domain;

import com.saynow.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nickname;

    @Column(length = 255)
    private String email;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public Member(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
    }

    public void updateProfile(String nickname, String email) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (email != null) {
            this.email = email;
        }
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.nickname = null;
        this.email = null;
        this.withdrawnAt = withdrawnAt;
    }
}
