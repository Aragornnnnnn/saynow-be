-- 룸메이트 시나리오 개편 후 누락될 수 있는 잠금 카테고리를 보정한다.
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

INSERT INTO categories (id, name, display_order, locked, lock_reason, created_at, updated_at)
SELECT 1, '룸메이트', 1, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE id = 1
);

INSERT INTO categories (id, name, display_order, locked, lock_reason, created_at, updated_at)
SELECT 2, '수업', 2, TRUE, 'COMING_SOON', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE id = 2
);

INSERT INTO categories (id, name, display_order, locked, lock_reason, created_at, updated_at)
SELECT 3, '여행', 3, TRUE, 'COMING_SOON', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE id = 3
);

ALTER TABLE categories ALTER COLUMN id RESTART WITH 4;
