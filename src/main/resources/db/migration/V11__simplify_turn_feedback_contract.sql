-- 새 최종 피드백 명세에 맞춰 턴별 피드백 저장 컬럼을 단순화한다.
ALTER TABLE turn_feedbacks ADD COLUMN feedback_detail TEXT;
ALTER TABLE turn_feedbacks ADD COLUMN better_expression VARCHAR(500);

UPDATE turn_feedbacks
SET feedback_detail = COALESCE(correction_reason, praise_reason, correction_point, praise_summary, korean_analogy),
    better_expression = plus_one_expression;

ALTER TABLE turn_feedbacks ALTER COLUMN feedback_detail SET NOT NULL;

ALTER TABLE turn_feedbacks DROP COLUMN correction_point;
ALTER TABLE turn_feedbacks DROP COLUMN correction_reason;
ALTER TABLE turn_feedbacks DROP COLUMN plus_one_expression;
ALTER TABLE turn_feedbacks DROP COLUMN praise_summary;
ALTER TABLE turn_feedbacks DROP COLUMN praise_reason;
