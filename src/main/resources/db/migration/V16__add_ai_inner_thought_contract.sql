-- AI 다음 질문 속마음과 턴 피드백 개선 표현 분리 저장 컬럼을 추가한다.
ALTER TABLE scenarios ADD COLUMN counterpart_role VARCHAR(50);

UPDATE scenarios
SET counterpart_role = 'friend';

ALTER TABLE scenarios ALTER COLUMN counterpart_role SET NOT NULL;

ALTER TABLE session_turns ADD COLUMN inner_thought TEXT;
ALTER TABLE session_turns ADD COLUMN inner_thought_type VARCHAR(20);

ALTER TABLE turn_feedbacks ADD COLUMN correction_expression VARCHAR(500);
ALTER TABLE turn_feedbacks ADD COLUMN correction_reason TEXT;
ALTER TABLE turn_feedbacks ALTER COLUMN feedback_detail DROP NOT NULL;
