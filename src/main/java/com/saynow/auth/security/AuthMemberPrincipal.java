// 인증된 회원 ID를 Spring Security principal로 표현하는 객체
package com.saynow.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthMemberPrincipal(Long memberId) implements UserDetails {

    public AuthMemberPrincipal {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return memberId.toString();
    }
}
