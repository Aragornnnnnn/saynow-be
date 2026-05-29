// 세션 NPS 평가 점수와 낮은 평가 사유를 받는 요청 DTO
package com.saynow.nps.api.dto;

public record NpsSubmitRequest(
        Integer score,
        String lowScoreReason
) {
}
