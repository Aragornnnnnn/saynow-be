// AI가 슬롯 충족 근거를 검증할 때 참고하는 정책 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiSlotEvidencePolicy(
        String mode,
        List<String> hints,
        boolean requiresEvidenceText,
        String mustBeGroundedIn
) {
}
