// 검증된 OIDC ID Token에서 추출한 사용자 정보를 전달하는 값 객체
package com.saynow.auth.application;

public record OidcUserInfo(
        String subject,
        String email,
        Boolean emailVerified,
        String nickname
) {
}
