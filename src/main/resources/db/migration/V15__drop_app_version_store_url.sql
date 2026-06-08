-- 앱 버전 정책에서 스토어 URL 관리 필드를 제거한다.
ALTER TABLE app_versions
    DROP COLUMN store_url;
