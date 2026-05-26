// 2차 MVP 시나리오 진행 스키마의 테이블과 컬럼 이름을 검증한다.
package com.saynow.scenario;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
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
    void scenariosHaveAiRoleColumn() {
        assertThat(columnExists("scenarios", "ai_role")).isTrue();
    }

    @Test
    void scenarioSlotsHaveDescriptionColumn() {
        assertThat(columnExists("scenario_slots", "description")).isTrue();
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

        List<String> aiRoles = jdbcTemplate.queryForList("""
                SELECT s.ai_role
                FROM scenarios s
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Airport'
                ORDER BY s.display_order
                """, String.class);
        assertThat(aiRoles).containsExactly(
                "미국 공항 입국심사관",
                "항공사 수하물 서비스 직원",
                "공항 환승 안내 직원");

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

    @Test
    void scenarioSlotsAreSeededWithDescriptions() {
        List<String> descriptions = jdbcTemplate.queryForList("""
                SELECT c.name || '|' || s.title || '|' || ss.name || '|' || ss.description
                FROM scenario_slots ss
                JOIN scenarios s ON s.id = ss.scenario_id
                JOIN categories c ON c.id = s.category_id
                ORDER BY c.id, s.display_order, ss.id
                """, String.class);

        assertThat(descriptions).containsExactly(
                "Cafe|카페에서 주문하기|drink|사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부",
                "Cafe|카페에서 주문하기|size|사용자가 음료의 크기나 사이즈를 말했는지 여부",
                "Cafe|카페에서 옵션 말하기|temperature|사용자가 음료를 따뜻하게 받을지 차갑게 받을지 말했는지 여부",
                "Cafe|카페에서 옵션 말하기|option|사용자가 샷 추가, 시럽, 우유 변경, 얼음 양 등 추가 옵션을 말했거나 추가 옵션이 없다고 표현했는지 여부",
                "Cafe|카페에서 문제 해결하기|problem|사용자가 주문이나 음료에 생긴 문제를 구체적으로 설명했는지 여부",
                "Cafe|카페에서 문제 해결하기|request|사용자가 교환, 환불, 다시 만들기, 주문 수정 등 원하는 해결 방식을 요청했는지 여부",
                "Airport|공항에서 입국심사 받기|visit_purpose|사용자가 미국 방문 목적을 여행, 출장, 유학 등으로 설명했는지 여부",
                "Airport|공항에서 입국심사 받기|stay_duration|사용자가 미국에 머무를 기간이나 출국 예정 시점을 설명했는지 여부",
                "Airport|공항에서 입국심사 받기|accommodation|사용자가 머무를 숙소, 호텔, 주소, 지인 집 등 체류 장소를 설명했는지 여부",
                "Airport|공항에서 수하물 문제 해결하기|baggage_issue|사용자가 수하물 파손, 분실, 지연 등 짐에 생긴 문제를 설명했는지 여부",
                "Airport|공항에서 수하물 문제 해결하기|requested_help|사용자가 보상, 교환, 수리, 분실 신고 등 직원에게 원하는 도움을 요청했는지 여부",
                "Airport|공항에서 수하물 문제 해결하기|contact_info|사용자가 후속 안내를 받을 수 있는 연락처나 이메일을 제공했는지 여부",
                "Airport|공항에서 환승편 놓칠 위기 설명하기|gate_location|사용자가 Gate B 또는 환승편 탑승 게이트의 위치를 물어보거나 찾고 있음을 설명했는지 여부",
                "Airport|공항에서 환승편 놓칠 위기 설명하기|boarding_possibility|사용자가 환승편에 아직 탑승할 수 있는지 직원에게 확인 요청을 했는지 여부",
                "Airport|공항에서 환승편 놓칠 위기 설명하기|time_pressure|사용자가 비행기 출발 시간이 임박했거나 시간이 부족한 긴급 상황임을 설명했는지 여부");
    }

    @Test
    void airportSeedMigrationRestoresCategoriesBeforeScenarios() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V3__seed_airport_scenarios.sql"));

        assertThat(migration.indexOf("INSERT INTO categories")).isLessThan(migration.indexOf("INSERT INTO scenarios"));
        assertThat(migration).contains("SELECT 2, 'Airport'");
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
