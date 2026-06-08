// 앱 버전 정책을 저장하는 Flyway 스키마를 검증한다.
package com.saynow.appversion;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionSchemaIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appVersionsTableContainsVersionPolicyColumns() {
        assertThat(tableExists("app_versions")).isTrue();
        assertThat(columnExists("app_versions", "platform")).isTrue();
        assertThat(columnExists("app_versions", "version_name")).isTrue();
        assertThat(columnExists("app_versions", "build_number")).isTrue();
        assertThat(columnExists("app_versions", "minimum_supported_build_number")).isTrue();
        assertThat(columnExists("app_versions", "force_update_reason")).isTrue();
        assertThat(columnExists("app_versions", "soft_update_reason")).isTrue();
        assertThat(columnExists("app_versions", "release_note")).isTrue();
        assertThat(columnExists("app_versions", "store_url")).isTrue();
        assertThat(columnExists("app_versions", "active")).isTrue();
        assertThat(columnExists("app_versions", "released_at")).isTrue();
        assertThat(columnExists("app_versions", "created_at")).isTrue();
        assertThat(columnExists("app_versions", "updated_at")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
