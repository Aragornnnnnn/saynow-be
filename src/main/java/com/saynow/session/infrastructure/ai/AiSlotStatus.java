// AI 요청에 포함할 현재 슬롯 충족 상태 DTO
package com.saynow.session.infrastructure.ai;

public record AiSlotStatus(
        String slotName,
        boolean filled
) {
}
