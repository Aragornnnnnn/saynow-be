-- 3차 MVP Free Talk 시나리오 1~3의 주제와 고정 질문을 최신 데이터로 갱신한다.
UPDATE scenarios
SET title = '여행 취향 이야기하기',
    briefing = '가고 싶은 여행지, 여행 방식, 예상치 못한 상황, 해외 생활에 대해 이야기합니다.',
    conversation_goal = '여행 취향과 해외 생활에 대한 생각을 영어로 자연스럽게 설명할 수 있다.',
    display_order = 1,
    total_question_count = 4,
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

UPDATE scenarios
SET title = '음식 취향 이야기하기',
    briefing = '자주 먹고 싶은 음식, 위로가 되는 음식, 평생 먹을 음식 선택에 대해 이야기합니다.',
    conversation_goal = '음식 취향과 선택 이유를 영어로 자연스럽게 설명할 수 있다.',
    display_order = 2,
    total_question_count = 4,
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

UPDATE scenarios
SET title = '음악 취향 이야기하기',
    briefing = '요즘 듣는 노래, 음악 앱, 콘서트 경험, 노래방 애창곡에 대해 이야기합니다.',
    conversation_goal = '음악 취향과 감상 경험을 영어로 자연스럽게 설명할 수 있다.',
    display_order = 3,
    total_question_count = 4,
    locked = FALSE,
    lock_reason = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;

UPDATE scenario_questions
SET question_en = 'If you could travel anywhere for free right now, where would you go? And what draws you to that place?',
    question_ko = '지금 당장 무료로 어디든 여행 갈 수 있다면 어디로 갈래? 그곳의 어떤 점이 끌려?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'Do you prefer traveling alone, or with other people? Why?',
    question_ko = '혼자 여행이 더 좋아, 같이 가는 게 더 좋아? 왜?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'Do you plan everything before a trip, or just go and figure it out? Has anything unexpected ever thrown you off?',
    question_ko = '여행 전에 다 계획해, 아니면 그냥 가서 해결해? 예상 못한 일이 생겨서 멘붕온 적 있어?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'Do you dream of living abroad someday, or would you rather stay in Korea? Why?',
    question_ko = '언젠가 해외에서 사는 게 꿈이야, 아니면 한국이 좋아? 왜?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND sequence = 4;

UPDATE scenario_questions
SET question_en = 'What food could you eat every week and never get tired of?',
    question_ko = '매주 먹어도 안 질릴 음식 뭐야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'What''s your go-to comfort food?',
    question_ko = '스트레스 받을 때 찾는 음식 뭐야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'If you could eat only one food forever, what would you pick?',
    question_ko = '평생 한 가지만 먹을 수 있다면 뭐 고를래?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'Never eat meat again, or never eat rice & bread again? Which would you pick? Why?',
    question_ko = '평생 고기 금지 vs 평생 밥·빵(탄수) 금지. 뭐 고를래? 왜?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND sequence = 4;

UPDATE scenario_questions
SET question_en = 'What song have you been playing on repeat lately?',
    question_ko = '요즘 무한 반복하는 노래 뭐야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 1;

UPDATE scenario_questions
SET question_en = 'What music app do you use, and what makes it your favorite?',
    question_ko = '음악 스트리밍 어플 뭐 써? 그거 쓰는 이유가 뭐야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 2;

UPDATE scenario_questions
SET question_en = 'Have you ever seen an artist live in concert? How was it? If not, who would you most want to see?',
    question_ko = '실제로 콘서트 가본 적 있어? 어땠어? 만약 없다면 제일 보고 싶은 가수는 누구야?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 3;

UPDATE scenario_questions
SET question_en = 'What''s your go-to karaoke song — the one you can always nail? Why that one?',
    question_ko = '노래방 18번 뭐야? 가면 무조건 부르는 곡. 왜 그거 불러?',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND sequence = 4;
