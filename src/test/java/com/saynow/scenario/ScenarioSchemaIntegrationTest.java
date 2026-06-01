// 3차 MVP 프리톡 시나리오 스키마와 seed 데이터를 검증한다.
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
    void thirdMvpTablesAndColumnsReplaceSlotAndHeartSchema() {
        assertThat(columnExists("categories", "display_order")).isTrue();
        assertThat(columnExists("categories", "locked")).isTrue();
        assertThat(columnExists("categories", "lock_reason")).isTrue();

        assertThat(columnExists("scenarios", "briefing")).isTrue();
        assertThat(columnExists("scenarios", "conversation_goal")).isTrue();
        assertThat(columnExists("scenarios", "total_question_count")).isTrue();
        assertThat(columnExists("scenarios", "locked")).isTrue();
        assertThat(columnExists("scenarios", "lock_reason")).isTrue();
        assertThat(columnExists("scenarios", "original_question")).isFalse();
        assertThat(columnExists("scenarios", "heart")).isFalse();

        assertThat(tableExists("scenario_questions")).isTrue();
        assertThat(columnExists("session_turns", "scenario_question_id")).isTrue();
        assertThat(columnExists("session_turns", "answered_at")).isTrue();
        assertThat(columnExists("session_turns", "next_question_target_slot_name")).isFalse();
        assertThat(columnExists("sessions", "remaining_hearts")).isFalse();

        assertThat(columnExists("user_scenario_progress", "completed")).isTrue();
        assertThat(columnExists("user_scenario_progress", "completed_at")).isTrue();
        assertThat(columnExists("user_scenario_progress", "cleared")).isFalse();

        assertThat(tableExists("scenario_slots")).isFalse();
        assertThat(tableExists("session_slot_statuses")).isFalse();
    }

    @Test
    void feedbackSchemaUsesNativeScoreAndQualityFeedbackFields() {
        assertThat(columnExists("session_feedbacks", "status")).isTrue();
        assertThat(columnExists("session_feedbacks", "native_score")).isTrue();
        assertThat(columnExists("session_feedbacks", "native_level_label")).isTrue();
        assertThat(columnExists("session_feedbacks", "summary")).isTrue();
        assertThat(columnExists("session_feedbacks", "generated_at")).isTrue();
        assertThat(columnExists("session_feedbacks", "comprehension_score")).isFalse();
        assertThat(columnExists("session_feedbacks", "feedback_summary")).isFalse();

        assertThat(columnExists("turn_feedbacks", "status")).isTrue();
        assertThat(columnExists("turn_feedbacks", "feedback_type")).isTrue();
        assertThat(columnExists("turn_feedbacks", "korean_analogy")).isTrue();
        assertThat(columnExists("turn_feedbacks", "correction_point")).isTrue();
        assertThat(columnExists("turn_feedbacks", "correction_reason")).isTrue();
        assertThat(columnExists("turn_feedbacks", "plus_one_expression")).isTrue();
        assertThat(columnExists("turn_feedbacks", "praise_summary")).isTrue();
        assertThat(columnExists("turn_feedbacks", "praise_reason")).isTrue();
        assertThat(columnExists("turn_feedbacks", "feedback_required")).isFalse();
        assertThat(columnExists("turn_feedbacks", "better_expression")).isFalse();
    }

    @Test
    void freeTalkScenariosAreSeededWithFourFixedQuestionsEach() {
        List<String> categories = jdbcTemplate.queryForList("""
                SELECT name || '|' || locked || '|' || COALESCE(lock_reason, '')
                FROM categories
                ORDER BY display_order
                """, String.class);

        assertThat(categories).containsExactly(
                "Free Talk|FALSE|",
                "Airport|TRUE|COMING_SOON",
                "Hotel|TRUE|COMING_SOON",
                "Restaurant|TRUE|COMING_SOON");

        List<String> scenarios = jdbcTemplate.queryForList("""
                SELECT s.id || '|' || s.title || '|' || s.total_question_count || '|' || s.locked
                FROM scenarios s
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Free Talk'
                ORDER BY s.display_order
                """, String.class);

        assertThat(scenarios).containsExactly(
                "1|음식 취향 이야기하기|4|FALSE",
                "2|여행 경험 이야기하기|4|FALSE",
                "3|일상 루틴 이야기하기|4|FALSE");

        List<Integer> questionCounts = jdbcTemplate.queryForList("""
                SELECT COUNT(*)
                FROM scenario_questions q
                JOIN scenarios s ON s.id = q.scenario_id
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Free Talk'
                GROUP BY s.id
                ORDER BY s.display_order
                """, Integer.class);

        assertThat(questionCounts).containsExactly(4, 4, 4);
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
