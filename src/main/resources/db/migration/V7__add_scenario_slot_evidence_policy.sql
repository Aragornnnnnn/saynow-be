-- 시나리오 슬롯별 AI 근거 검증 정책을 저장하고 환승편 시나리오 슬롯을 갱신한다.
ALTER TABLE scenario_slots ADD COLUMN evidence_policy TEXT;

UPDATE scenarios
SET title = '환승편을 놓친 뒤 도움 요청하기',
    goal = '수하물 문제 때문에 환승편을 놓쳤다고 설명하고, 다음 조치나 대체편을 물어볼 수 있다.',
    situation = '수하물 문제를 해결하느라 Gate B에서 출발한 환승편을 이미 놓친 상황이에요. 항공사 환승 데스크 직원에게 상황을 설명하고 다음 조치를 물어봐야 해요.',
    ai_role = '항공사 환승 데스크 직원',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 6;

UPDATE scenario_slots
SET name = 'missed_connection',
    description = '사용자가 환승편을 이미 놓쳤거나 비행기가 이미 출발했다고 설명했는지 여부',
    evidence_policy = '{"mode":"semantic_evidence","hints":["missed connecting flight","missed my flight","flight already left","could not catch my connection"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND name IN ('gate_location', 'missed_connection');

UPDATE scenario_slots
SET name = 'baggage_delay_reason',
    description = '사용자가 수하물 지연이나 수하물 문제 때문에 환승편을 놓쳤다고 설명했는지 여부',
    evidence_policy = '{"mode":"semantic_evidence","hints":["baggage","luggage","suitcase","bag","checked bag","baggage claim"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND name IN ('boarding_possibility', 'baggage_delay_reason');

UPDATE scenario_slots
SET name = 'next_options_request',
    description = '사용자가 다음에 무엇을 해야 하는지, 대체 항공편이나 재예약 가능 여부를 물었는지 여부',
    evidence_policy = '{"mode":"semantic_evidence","hints":["next flight","another flight","rebook","what can I do","help me"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND name IN ('time_pressure', 'next_options_request');

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["coffee","americano","latte","tea","drink"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND name = 'drink';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["small","medium","large","regular","size"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 1
  AND name = 'size';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["hot","iced","cold","warm"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND name = 'temperature';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["extra shot","syrup","milk","less ice","no option"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 2
  AND name = 'option';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["wrong order","problem","spilled","too cold","does not taste right"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND name = 'problem';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["refund","exchange","make it again","change my order","fix it"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 3
  AND name = 'request';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["travel","business","study","vacation","visit"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 4
  AND name = 'visit_purpose';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["days","weeks","until","stay for","return"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 4
  AND name = 'stay_duration';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["hotel","address","friend house","stay at","accommodation"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 4
  AND name = 'accommodation';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["broken suitcase","lost baggage","delayed luggage","damaged bag","baggage problem"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 5
  AND name = 'baggage_issue';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["compensation","repair","replace","report","help me"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 5
  AND name = 'requested_help';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"explicit_pattern","hints":["phone number","email","123-4567","name@example.com"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 5
  AND name = 'contact_info';
