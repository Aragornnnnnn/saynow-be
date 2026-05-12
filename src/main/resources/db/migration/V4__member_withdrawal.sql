-- 회원 탈퇴 상태를 회원 테이블에 기록하는 마이그레이션
ALTER TABLE members ADD COLUMN withdrawn_at TIMESTAMP(6);
