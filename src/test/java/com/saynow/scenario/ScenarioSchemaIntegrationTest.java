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
        assertThat(columnExists("session_feedbacks", "attempted_word_score")).isFalse();
        assertThat(columnExists("session_feedbacks", "sentence_complexity_score")).isFalse();
        assertThat(columnExists("session_feedbacks", "comprehensibility_score")).isFalse();
        assertThat(columnExists("session_feedbacks", "highlight_message")).isTrue();
        assertThat(columnExists("session_feedbacks", "native_level_label")).isFalse();
        assertThat(columnExists("session_feedbacks", "summary")).isFalse();
        assertThat(columnExists("session_feedbacks", "generated_at")).isTrue();
        assertThat(columnExists("session_feedbacks", "comprehension_score")).isFalse();
        assertThat(columnExists("session_feedbacks", "feedback_summary")).isFalse();

        assertThat(columnExists("turn_feedbacks", "status")).isTrue();
        assertThat(columnExists("turn_feedbacks", "feedback_type")).isTrue();
        assertThat(columnExists("turn_feedbacks", "korean_analogy")).isTrue();
        assertThat(columnExists("turn_feedbacks", "positive_feedback")).isTrue();
        assertThat(columnExists("turn_feedbacks", "feedback_detail")).isTrue();
        assertThat(columnExists("turn_feedbacks", "benchmark_message")).isTrue();
        assertThat(columnExists("turn_feedbacks", "better_expression")).isFalse();
        assertThat(columnExists("turn_feedbacks", "correction_point")).isFalse();
        assertThat(columnExists("turn_feedbacks", "plus_one_expression")).isFalse();
        assertThat(columnExists("turn_feedbacks", "praise_summary")).isFalse();
        assertThat(columnExists("turn_feedbacks", "praise_reason")).isFalse();
        assertThat(columnExists("turn_feedbacks", "feedback_required")).isFalse();
    }

    @Test
    void aiInnerThoughtAndCorrectionSplitColumnsExist() {
        assertThat(columnExists("scenarios", "counterpart_role")).isTrue();
        assertThat(isNullable("scenarios", "counterpart_role")).isFalse();

        assertThat(columnExists("session_turns", "inner_thought")).isTrue();
        assertThat(columnExists("session_turns", "inner_thought_type")).isTrue();
        assertThat(isNullable("session_turns", "inner_thought")).isTrue();
        assertThat(isNullable("session_turns", "inner_thought_type")).isTrue();

        assertThat(columnExists("turn_feedbacks", "correction_expression")).isTrue();
        assertThat(columnExists("turn_feedbacks", "correction_reason")).isTrue();
        assertThat(isNullable("turn_feedbacks", "correction_expression")).isTrue();
        assertThat(isNullable("turn_feedbacks", "correction_reason")).isTrue();
        assertThat(isNullable("turn_feedbacks", "feedback_detail")).isTrue();
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
                "1|여행 취향 이야기하기|4|FALSE",
                "2|음식 취향 이야기하기|4|FALSE",
                "3|음악 취향 이야기하기|4|FALSE");

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

        List<String> questions = jdbcTemplate.queryForList("""
                SELECT s.id || '|' || q.sequence || '|' || q.question_en || '|' || q.question_ko
                FROM scenario_questions q
                JOIN scenarios s ON s.id = q.scenario_id
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = 'Free Talk'
                ORDER BY s.display_order, q.sequence
                """, String.class);

        assertThat(questions).containsExactly(
                "1|1|If you could travel anywhere for free right now, where would you go? And what draws you to that place?|지금 당장 무료로 어디든 여행 갈 수 있다면 어디로 갈래? 그곳의 어떤 점이 끌려?",
                "1|2|Do you prefer traveling alone, or with other people? Why?|혼자 여행이 더 좋아, 같이 가는 게 더 좋아? 왜?",
                "1|3|Do you plan everything before a trip, or just go and figure it out? Has anything unexpected ever thrown you off?|여행 전에 다 계획해, 아니면 그냥 가서 해결해? 예상 못한 일이 생겨서 멘붕온 적 있어?",
                "1|4|Do you dream of living abroad someday, or would you rather stay in Korea? Why?|언젠가 해외에서 사는 게 꿈이야, 아니면 한국이 좋아? 왜?",
                "2|1|What food could you eat every week and never get tired of?|매주 먹어도 안 질릴 음식 뭐야?",
                "2|2|What's your go-to comfort food?|스트레스 받을 때 찾는 음식 뭐야?",
                "2|3|If you could eat only one food forever, what would you pick?|평생 한 가지만 먹을 수 있다면 뭐 고를래?",
                "2|4|Never eat meat again, or never eat rice & bread again? Which would you pick? Why?|평생 고기 금지 vs 평생 밥·빵(탄수) 금지. 뭐 고를래? 왜?",
                "3|1|What song have you been playing on repeat lately?|요즘 무한 반복하는 노래 뭐야?",
                "3|2|What music app do you use, and what makes it your favorite?|음악 스트리밍 어플 뭐 써? 그거 쓰는 이유가 뭐야?",
                "3|3|Have you ever seen an artist live in concert? How was it? If not, who would you most want to see?|실제로 콘서트 가본 적 있어? 어땠어? 만약 없다면 제일 보고 싶은 가수는 누구야?",
                "3|4|What's your go-to karaoke song — the one you can always nail? Why that one?|노래방 18번 뭐야? 가면 무조건 부르는 곡. 왜 그거 불러?");
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

    private boolean isNullable(String tableName, String columnName) {
        String nullable = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_name = ?
                  AND column_name = ?
                """, String.class, tableName, columnName);
        return "YES".equals(nullable);
    }
}
