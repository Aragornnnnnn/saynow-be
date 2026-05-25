// 2차 MVP 시나리오 진행 스키마의 테이블과 컬럼 이름을 검증한다.
package com.saynow.scenario;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioSchemaIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void userScenarioProgressUsesProgressTableAndClearedColumn() {
        assertThat(tableExists("user_scenario_progress")).isTrue();
        assertThat(columnExists("user_scenario_progress", "cleared")).isTrue();
        assertThat(tableExists("user_scenario_clears")).isFalse();
        assertThat(columnExists("user_scenario_progress", "is_cleared")).isFalse();
    }

    @Test
    void scenariosHaveSituationColumn() {
        assertThat(columnExists("scenarios", "situation")).isTrue();
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
