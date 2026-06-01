-- 시나리오 슬롯별 AI 판정 기준 설명을 추가한다.
ALTER TABLE scenario_slots ADD COLUMN description VARCHAR(255);

UPDATE scenario_slots
SET description = '사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부'
WHERE scenario_id = 1 AND name = 'drink';

UPDATE scenario_slots
SET description = '사용자가 음료의 크기나 사이즈를 말했는지 여부'
WHERE scenario_id = 1 AND name = 'size';

UPDATE scenario_slots
SET description = '사용자가 음료를 따뜻하게 받을지 차갑게 받을지 말했는지 여부'
WHERE scenario_id = 2 AND name = 'temperature';

UPDATE scenario_slots
SET description = '사용자가 샷 추가, 시럽, 우유 변경, 얼음 양 등 추가 옵션을 말했거나 추가 옵션이 없다고 표현했는지 여부'
WHERE scenario_id = 2 AND name = 'option';

UPDATE scenario_slots
SET description = '사용자가 주문이나 음료에 생긴 문제를 구체적으로 설명했는지 여부'
WHERE scenario_id = 3 AND name = 'problem';

UPDATE scenario_slots
SET description = '사용자가 교환, 환불, 다시 만들기, 주문 수정 등 원하는 해결 방식을 요청했는지 여부'
WHERE scenario_id = 3 AND name = 'request';

UPDATE scenario_slots
SET description = '사용자가 미국 방문 목적을 여행, 출장, 유학 등으로 설명했는지 여부'
WHERE scenario_id = 4 AND name = 'visit_purpose';

UPDATE scenario_slots
SET description = '사용자가 미국에 머무를 기간이나 출국 예정 시점을 설명했는지 여부'
WHERE scenario_id = 4 AND name = 'stay_duration';

UPDATE scenario_slots
SET description = '사용자가 머무를 숙소, 호텔, 주소, 지인 집 등 체류 장소를 설명했는지 여부'
WHERE scenario_id = 4 AND name = 'accommodation';

UPDATE scenario_slots
SET description = '사용자가 수하물 파손, 분실, 지연 등 짐에 생긴 문제를 설명했는지 여부'
WHERE scenario_id = 5 AND name = 'baggage_issue';

UPDATE scenario_slots
SET description = '사용자가 보상, 교환, 수리, 분실 신고 등 직원에게 원하는 도움을 요청했는지 여부'
WHERE scenario_id = 5 AND name = 'requested_help';

UPDATE scenario_slots
SET description = '사용자가 후속 안내를 받을 수 있는 연락처나 이메일을 제공했는지 여부'
WHERE scenario_id = 5 AND name = 'contact_info';

UPDATE scenario_slots
SET description = '사용자가 Gate B 또는 환승편 탑승 게이트의 위치를 물어보거나 찾고 있음을 설명했는지 여부'
WHERE scenario_id = 6 AND name = 'gate_location';

UPDATE scenario_slots
SET description = '사용자가 환승편에 아직 탑승할 수 있는지 직원에게 확인 요청을 했는지 여부'
WHERE scenario_id = 6 AND name = 'boarding_possibility';

UPDATE scenario_slots
SET description = '사용자가 비행기 출발 시간이 임박했거나 시간이 부족한 긴급 상황임을 설명했는지 여부'
WHERE scenario_id = 6 AND name = 'time_pressure';

ALTER TABLE scenario_slots ALTER COLUMN description SET NOT NULL;
