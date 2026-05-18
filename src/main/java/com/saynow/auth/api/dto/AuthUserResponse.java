// 인증 응답에 포함되는 사용자 요약 DTO
package com.saynow.auth.api.dto;

public record AuthUserResponse(
        String userId,
        String nickname,
        String email,
        String provider,
        boolean newUser
) {
}
