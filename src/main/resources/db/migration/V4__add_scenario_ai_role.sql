-- 시나리오별 AI 역할 컬럼과 기존 seed 값을 추가한다.
ALTER TABLE scenarios ADD COLUMN ai_role VARCHAR(100);

UPDATE scenarios
SET ai_role = '카페 주문 접수 직원'
WHERE id = 1;

UPDATE scenarios
SET ai_role = '카페 음료 옵션 확인 직원'
WHERE id = 2;

UPDATE scenarios
SET ai_role = '카페 주문 문제 해결 직원'
WHERE id = 3;

UPDATE scenarios
SET ai_role = '미국 공항 입국심사관'
WHERE id = 4;

UPDATE scenarios
SET ai_role = '항공사 수하물 서비스 직원'
WHERE id = 5;

UPDATE scenarios
SET ai_role = '공항 환승 안내 직원'
WHERE id = 6;

ALTER TABLE scenarios ALTER COLUMN ai_role SET NOT NULL;
