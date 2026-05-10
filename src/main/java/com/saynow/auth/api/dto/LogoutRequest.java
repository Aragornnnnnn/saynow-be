// 로그아웃 요청 본문을 표현하는 DTO
package com.saynow.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String refreshToken
) {
}
