-- 시나리오별 상황 설명 컬럼과 기본 데이터를 추가한다.
ALTER TABLE scenarios ADD COLUMN situation VARCHAR(255);

UPDATE scenarios
SET situation = '카페에서 음료를 주문해야 하는 상황'
WHERE id = 1;

UPDATE scenarios
SET situation = '카페에서 음료 옵션을 선택해야 하는 상황'
WHERE id = 2;

UPDATE scenarios
SET situation = '카페 주문에 생긴 문제를 해결해야 하는 상황'
WHERE id = 3;

ALTER TABLE scenarios ALTER COLUMN situation SET NOT NULL;
