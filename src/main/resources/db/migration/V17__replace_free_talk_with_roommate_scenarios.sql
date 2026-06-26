-- 3차 MVP 시나리오 seed를 교환학생 룸메이트 대화 중심 데이터로 교체한다.
UPDATE categories
SET name = '룸메이트',
    display_order = 1,
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE categories
SET name = '수업',
    display_order = 2,
    locked = TRUE,
    lock_reason = 'COMING_SOON',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

UPDATE categories
SET name = '여행',
    display_order = 3,
    locked = TRUE,
    lock_reason = 'COMING_SOON',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;

DELETE FROM categories
WHERE id = 4
  AND NOT EXISTS (
      SELECT 1
      FROM scenarios
      WHERE category_id = 4
  );

UPDATE scenarios
SET title = '입주 첫날 — charlie와 첫 만남',
    briefing = '입주 첫날 룸메이트 charlie와 서로를 소개하고, 공동생활 방식과 식사, 파티 초대에 대해 이야기합니다.',
    conversation_goal = '룸메이트와 첫 만남에서 자기소개, 공동생활 방식, 음식 취향, 초대 거절을 부드럽게 말한다.',
    category_id = 1,
    display_order = 1,
    total_question_count = 4,
    counterpart_role = 'roommate',
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE scenarios
SET title = '카페에서 수다떨면서 주말 약속 잡기',
    briefing = '룸메이트와 카페에서 주말 약속을 잡고, 취미와 축하할 일, 장보기 부탁을 자연스럽게 이야기합니다.',
    conversation_goal = '친근한 제안에 관심을 보이고, 축하와 부탁 상황에서 차갑지 않게 반응한다.',
    category_id = 1,
    display_order = 2,
    total_question_count = 4,
    counterpart_role = 'roommate',
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

UPDATE scenarios
SET title = '서로 더 알아가는 밤 — 룸메 토크',
    briefing = '밤에 기숙사에서 룸메이트와 진실게임, 꿈, 기분, 생활 습관에 대해 더 깊게 이야기합니다.',
    conversation_goal = '친밀한 대화에서 선을 지키고, 걱정과 농담에 자연스럽고 부드럽게 반응한다.',
    category_id = 1,
    display_order = 3,
    total_question_count = 4,
    counterpart_role = 'roommate',
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;

UPDATE scenarios
SET locked = TRUE,
    lock_reason = 'COMING_SOON',
    updated_at = CURRENT_TIMESTAMP
WHERE category_id <> 1;

UPDATE scenario_questions
SET question_en = 'Hey, you must be my roommate! I''m charlie. Okay, tell me everything — what are you studying, what are you into?',
    question_ko = '야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'Wait, I''m so curious — what made you decide to come all the way here? Like, why this country?',
    question_ko = '잠깐, 나 완전 궁금해 — 너 어쩌다 여기까지 오게 된 거야? 그러니까, 왜 이 나라야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'Okay, real talk — how should we split the cleaning and stuff? Wanna make a schedule, or just figure it out as we go? What works better for you?',
    question_ko = '자 진지하게 — 청소 같은 거 어떻게 나눌까? 스케줄 짤까, 그냥 그때그때 할까? 넌 어떻게 하는 게 좋아?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'I''m making dinner tonight — wanna share? Oh wait, is there anything you really can''t eat? I don''t wanna make something you''ll hate.',
    question_ko = '오늘 저녁 내가 하는데 — 같이 먹을래? 아 참, 너 진짜 못 먹는 거 있어? 싫어하는 거 만들기 싫어서.',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 4;

UPDATE scenario_questions
SET question_en = 'We should totally hang out this weekend! Does Saturday or Sunday work better for you?',
    question_ko = '우리 이번 주말에 꼭 놀자! 토요일이랑 일요일 중에 언제가 더 좋아?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'So what do you usually do for fun? Or is there anything you''ve been dying to try ever since you got here?',
    question_ko = '넌 보통 뭐하고 놀아? 아님 여기 와서 꼭 해보고 싶었던 거 있어?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'Oh my god, I almost forgot to tell you — Dude, guess what — I finally got that internship I told you about! I''m freaking out, I''m so happy!',
    question_ko = '헐 너한테 말하는 거 까먹을 뻔 — 야, 있잖아 — 나 너한테 말했던 그 인턴십 드디어 됐어! 너무 신나, 진짜 행복해!',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'Oh shoot, is it already that late? I need to run to the store before it closes! Wanna come with? Or do you need me to grab you anything?',
    question_ko = '어 벌써 시간이 이렇게 됐네? 마트 문 닫기 전에 얼른 가야겠다! 같이 갈래? 아님 내가 뭐 사다 줄까?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 4;

UPDATE scenario_questions
SET question_en = 'Okay, let''s play Truth or Dare — well, just the truth part. Ask me anything and I''ll answer 100% honestly!',
    question_ko = '자 우리 진실게임하자 — 아니 벌칙은 빼고 진실만. 아무거나 물어봐, 100% 솔직하게 답할게!',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'Okay, my turn to ask! I''m curious about you — what''s your big dream? And what made you pick your major?',
    question_ko = '좋아, 이제 내가 물어볼 차례! 나 궁금한 거 있어 — 너는 꿈이 뭐야? 그리고 왜 그 전공을 선택했어?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'You''ve seemed kinda off lately — everything okay? You know you can talk to me, right? Come on, tell me everything!',
    question_ko = '너 요즘 좀 기운없어 보여 — 다 괜찮아? 나한테 얘기해도 되는 거 알지? 자, 나한테 다 털어놔봐!',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'Okay, let''s get some sleep! Oh, but before we do... don''t get mad, okay? You were snoring SO loud last night I genuinely thought there was a tiny monster under your bed, I swear, hahaha! Try not to snore tonight, okay~?',
    question_ko = '좋아 이제 자자! 아 근데 자기 전에.. 화내지 말고 들어, 아니 너 어젯밤에 코를 너무 크게 골아서 침대 밑에 뭔 애기 몬스터라도 있는 줄 알았잖아 진심ㅋㅋㅋ 오늘은 코 골지 마라~~?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 4;
