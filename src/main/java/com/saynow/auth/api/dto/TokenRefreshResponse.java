// access token 재발급 결과 DTO
package com.saynow.auth.api.dto;

public record TokenRefreshResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}
