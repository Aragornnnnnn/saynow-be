// 세션 NPS 평가 저장 스키마를 검증한다.
package com.saynow.nps;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SessionNpsSchemaIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sessionNpsResponsesTableHasRequiredColumns() {
        assertThat(tableExists("session_nps_responses")).isTrue();
        assertThat(columnExists("session_nps_responses", "user_id")).isTrue();
        assertThat(columnExists("session_nps_responses", "session_id")).isTrue();
        assertThat(columnExists("session_nps_responses", "score")).isTrue();
        assertThat(columnExists("session_nps_responses", "low_score_reason")).isTrue();
        assertThat(columnExists("session_nps_responses", "created_at")).isTrue();
        assertThat(columnExists("session_nps_responses", "updated_at")).isTrue();
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
