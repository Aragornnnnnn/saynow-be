// 로그인과 재발급 결과의 SayNow token DTO
package com.saynow.auth.api.dto;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        AuthMemberResponse member
) {
}
