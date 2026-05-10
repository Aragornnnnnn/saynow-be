// 인증 응답에 포함되는 회원 요약 DTO
package com.saynow.auth.api.dto;

public record AuthMemberResponse(
        String memberId,
        String nickname,
        String email,
        String provider,
        boolean newMember
) {
}
