// 사용자가 제출한 발화 원문을 받는 요청 DTO
package com.saynow.session.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUtteranceRequest(
        @NotBlank String userUtterance
) {
}
