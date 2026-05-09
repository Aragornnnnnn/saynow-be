// 소셜 로그인 요청 본문을 표현하는 DTO
package com.saynow.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank String provider,
        @NotBlank String idToken,
        @NotBlank String nonce
) {
}
