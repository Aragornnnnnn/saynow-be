// AI가 이번 발화로 충족됐다고 판단한 슬롯을 표현한다.
package com.saynow.session.infrastructure.ai;

public record AiFilledSlot(
        String slotName
) {
}
