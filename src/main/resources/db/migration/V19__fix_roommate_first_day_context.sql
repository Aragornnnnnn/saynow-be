-- 룸메이트 첫 만남 시나리오에서 제거된 파티 질문 흔적을 보정한다.
UPDATE scenarios
SET briefing = '입주 첫날 룸메이트 charlie와 서로를 소개하고, 공동생활 방식과 식사 취향에 대해 이야기합니다.',
    conversation_goal = '룸메이트와 첫 만남에서 자기소개, 공동생활 방식, 음식 취향을 부드럽게 말한다.',
    total_question_count = 4,
    counterpart_role = 'roommate',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE scenario_questions
SET question_en = 'I''m making dinner tonight — wanna share? Oh wait, is there anything you really can''t eat? I don''t wanna make something you''ll hate.',
    question_ko = '오늘 저녁 내가 하는데 — 같이 먹을래? 아 참, 너 진짜 못 먹는 거 있어? 싫어하는 거 만들기 싫어서.',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 4;
