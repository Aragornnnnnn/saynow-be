// 세션 NPS 평가 제출 검증과 저장을 처리하는 서비스
package com.saynow.nps.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.nps.api.dto.NpsSubmitRequest;
import com.saynow.nps.domain.SessionNpsResponse;
import com.saynow.nps.infrastructure.SessionNpsResponseRepository;
import com.saynow.session.domain.Session;
import com.saynow.session.domain.SessionStatus;
import com.saynow.session.infrastructure.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NpsService {

    private final SessionRepository sessionRepository;
    private final SessionNpsResponseRepository sessionNpsResponseRepository;

    @Transactional
    public void submitNps(Long userId, Long sessionId, NpsSubmitRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new ApiException(ErrorCode.SESSION_IN_PROGRESS);
        }
        if (sessionNpsResponseRepository.existsBySession(session)) {
            throw new ApiException(ErrorCode.NPS_ALREADY_SUBMITTED);
        }

        int score = validateScore(request);
        String lowScoreReason = normalizeLowScoreReason(request.lowScoreReason());
        if (score >= 3 && lowScoreReason != null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        sessionNpsResponseRepository.save(new SessionNpsResponse(
                session.getUser(),
                session,
                score,
                lowScoreReason));
    }

    private int validateScore(NpsSubmitRequest request) {
        if (request == null || request.score() == null || request.score() < 1 || request.score() > 5) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return request.score();
    }

    private String normalizeLowScoreReason(String lowScoreReason) {
        if (lowScoreReason == null || lowScoreReason.isBlank()) {
            return null;
        }
        return lowScoreReason.trim();
    }
}
