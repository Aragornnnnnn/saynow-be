-- 기본 카테고리를 복구하고 공항 시나리오를 추가한다.
UPDATE categories SET name = 'Cafe', updated_at = CURRENT_TIMESTAMP WHERE id = 1;
INSERT INTO categories (id, name, created_at, updated_at)
SELECT 1, 'Cafe', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 1);

UPDATE categories SET name = 'Airport', updated_at = CURRENT_TIMESTAMP WHERE id = 2;
INSERT INTO categories (id, name, created_at, updated_at)
SELECT 2, 'Airport', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 2);

UPDATE categories SET name = 'Hotel', updated_at = CURRENT_TIMESTAMP WHERE id = 3;
INSERT INTO categories (id, name, created_at, updated_at)
SELECT 3, 'Hotel', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 3);

UPDATE categories SET name = 'Restaurant', updated_at = CURRENT_TIMESTAMP WHERE id = 4;
INSERT INTO categories (id, name, created_at, updated_at)
SELECT 4, 'Restaurant', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 4);

ALTER TABLE categories ALTER COLUMN id RESTART WITH 5;

INSERT INTO scenarios (
    id, title, original_question, translated_question, goal, situation, emoji, background_image,
    heart, display_order, category_id, created_at, updated_at
) VALUES
(4, '공항에서 입국심사 받기', 'Hi, what''s the purpose of your visit?', '안녕하세요. 방문 목적이 어떻게 되시나요?', '입국 목적과 체류 정보를 설명하고 입국심사를 통과할 수 있다.', '미국 공항에 도착해 입국심사를 받는 상황입니다. 심사관의 질문에 여행 계획을 차분히 설명해야 합니다.', '🛂', 'https://static.saynow.local/airport-immigration.png', 3, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '공항에서 수하물 문제 해결하기', 'Oh, how can I help you today?', '네, 무엇을 도와드릴까요?', '항공사 직원에게 수하물 문제를 설명하고 도움을 요청할 수 있다.', '수하물을 찾은 뒤 캐리어가 파손된 것을 발견했습니다. 항공사 직원에게 상황을 설명하고 도움을 요청해야 합니다.', '🧳', 'https://static.saynow.local/airport-baggage-help.png', 3, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '공항에서 환승편 놓칠 위기 설명하기', 'Oh, you look worried. What''s going on?', '괜찮으세요? 무슨 일 있으신가요?', '직원에게 게이트 위치와 탑승 가능 여부를 빠르게 물어볼 수 있다.', '짐 문제로 시간이 지체되어 Gate B에서 출발하는 환승편을 놓칠 수 있는 상황입니다. 공항 직원에게 빠르게 도움을 요청해야 합니다.', '✈️', 'https://static.saynow.local/airport-transfer-risk.png', 3, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE scenarios ALTER COLUMN id RESTART WITH 7;

INSERT INTO scenario_slots (scenario_id, name, created_at, updated_at) VALUES
(4, 'visit_purpose', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'stay_duration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'accommodation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'baggage_issue', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'requested_help', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'contact_info', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'gate_location', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'boarding_possibility', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'time_pressure', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
