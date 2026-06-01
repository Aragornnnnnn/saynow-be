-- 세션 턴별 다음 질문 target slot 힌트를 저장한다.
ALTER TABLE session_turns ADD COLUMN next_question_target_slot_name VARCHAR(80);
