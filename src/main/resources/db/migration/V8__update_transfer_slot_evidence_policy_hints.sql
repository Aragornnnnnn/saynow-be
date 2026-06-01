-- 환승편 시나리오 슬롯별 AI 근거 검증 힌트를 보정한다.
UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["baggage","luggage","suitcase","checked bag","baggage claim","items came out late","baggage took too long","delayed at baggage claim"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND name = 'baggage_delay_reason';

UPDATE scenario_slots
SET evidence_policy = '{"mode":"semantic_evidence","hints":["next flight","another flight","rebook","what should I do","what can I do","help me rebook","find another flight"],"requiresEvidenceText":true,"mustBeGroundedIn":"latest_user_utterance"}',
    updated_at = CURRENT_TIMESTAMP
WHERE scenario_id = 6
  AND name = 'next_options_request';
