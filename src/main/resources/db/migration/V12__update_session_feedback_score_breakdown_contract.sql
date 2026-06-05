-- 세션 최종 피드백을 후킹 메시지 계약으로 전환한다.
ALTER TABLE session_feedbacks ADD COLUMN highlight_message TEXT;

UPDATE session_feedbacks
SET highlight_message = summary;

ALTER TABLE session_feedbacks ALTER COLUMN highlight_message SET NOT NULL;

ALTER TABLE session_feedbacks DROP COLUMN native_level_label;
ALTER TABLE session_feedbacks DROP COLUMN summary;

ALTER TABLE turn_feedbacks ADD COLUMN positive_feedback TEXT;
ALTER TABLE turn_feedbacks ADD COLUMN benchmark_message TEXT;

UPDATE turn_feedbacks
SET feedback_detail = CASE
        WHEN better_expression IS NULL OR better_expression = '' THEN feedback_detail
        ELSE feedback_detail || ' ' || better_expression
    END;

ALTER TABLE turn_feedbacks DROP COLUMN better_expression;
