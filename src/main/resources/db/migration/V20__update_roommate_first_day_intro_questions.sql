-- 룸메이트 첫 만남 시나리오를 자기소개, 취미, 한국 추천지 3문항 흐름으로 교체한다.
UPDATE scenarios
SET briefing = '입주 첫날 룸메이트 Charlie와 처음 인사하고, 자기소개와 취미, 한국 추천 관광지에 대해 이야기합니다.',
    conversation_goal = '이름과 자기소개를 자연스럽게 말하고, 취미의 매력과 추천 장소의 이유를 영어로 설명한다.',
    total_question_count = 3,
    counterpart_role = 'roommate',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE scenario_questions
SET question_en = 'Hey, you''re my roommate, right?! I''m Charlie, nice to meet you! What''s your name? Tell me a little about yourself!',
    question_ko = '안녕 너 내 룸메이트지?! 난 charlie야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라.',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'What are you into? What do you love about it?',
    question_ko = '취미는 뭐야? 그게 어떤 매력이 있어?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'I''m obsessed with Korea! Tell me your must-visit spots and why I should go!',
    question_ko = '나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 3;

DELETE FROM scenario_questions q
WHERE q.scenario_id = 1
  AND q.sequence > 3
  AND NOT EXISTS (
      SELECT 1
      FROM session_turns st
      WHERE st.scenario_question_id = q.id
  );
