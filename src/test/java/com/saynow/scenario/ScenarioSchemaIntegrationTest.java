// 2차 MVP 시나리오 진행 스키마의 테이블과 컬럼 이름을 검증한다.
package com.saynow.scenario;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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

    @Test
    void airportScenariosAreSeededWithSituationsAndSlots() {
        List<String> scenarioTitles = jdbcTemplate.queryForList("""
                SELECT s.title
                FROM scenarios s
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Airport'
                ORDER BY s.display_order
                """, String.class);

        assertThat(scenarioTitles).containsExactly(
                "공항에서 입국심사 받기",
                "공항에서 수하물 문제 해결하기",
                "공항에서 환승편 놓칠 위기 설명하기");

        String transferQuestion = jdbcTemplate.queryForObject("""
                SELECT s.translated_question
                FROM scenarios s
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Airport'
                  AND s.display_order = 3
                """, String.class);
        assertThat(transferQuestion).isEqualTo("괜찮으세요? 무슨 일 있으신가요?");

        List<String> transferSlots = jdbcTemplate.queryForList("""
                SELECT ss.name
                FROM scenario_slots ss
                JOIN scenarios s ON s.id = ss.scenario_id
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Airport'
                  AND s.display_order = 3
                ORDER BY ss.name
                """, String.class);
        assertThat(transferSlots).containsExactly("boarding_possibility", "gate_location", "time_pressure");
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
