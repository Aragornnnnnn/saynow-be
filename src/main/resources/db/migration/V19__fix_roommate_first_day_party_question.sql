-- 룸메이트 첫 만남 시나리오의 마지막 질문을 파티 초대 흐름으로 고정한다.
UPDATE scenarios
SET briefing = '입주 첫날 룸메이트 charlie와 서로를 소개하고, 공동생활 방식과 파티 초대에 대해 이야기합니다.',
    conversation_goal = '룸메이트와 첫 만남에서 자기소개, 공동생활 방식, 초대 수락이나 거절을 부드럽게 말한다.',
    total_question_count = 4,
    counterpart_role = 'roommate',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE scenario_questions
SET question_en = 'Oh, do you like parties? A bunch of us are going to a party tonight — you in?',
    question_ko = '아, 너 파티 좋아해? 오늘 밤에 우리 여럿이 파티 가는데 — 너도 갈래?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 4;
