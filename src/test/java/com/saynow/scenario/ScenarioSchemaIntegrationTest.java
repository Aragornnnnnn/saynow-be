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
    void roommateScenariosAreSeededWithRequestedCategoriesAndFixedQuestions() {
        List<String> categories = jdbcTemplate.queryForList("""
                SELECT name || '|' || locked || '|' || COALESCE(lock_reason, '')
                FROM categories
                ORDER BY display_order
                """, String.class);

        assertThat(categories).containsExactly(
                "룸메이트|FALSE|",
                "수업|TRUE|COMING_SOON",
                "여행|TRUE|COMING_SOON");

        List<String> scenarios = jdbcTemplate.queryForList("""
                SELECT s.id || '|' || s.title || '|' || s.total_question_count || '|' || s.locked || '|' || s.counterpart_role
                FROM scenarios s
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = '룸메이트'
                ORDER BY s.display_order
                """, String.class);

        assertThat(scenarios).containsExactly(
                "1|입주 첫날 — charlie와 첫 만남|3|FALSE|roommate",
                "2|카페에서 수다떨면서 주말 약속 잡기|4|FALSE|roommate",
                "3|서로 더 알아가는 밤 — 룸메 토크|4|FALSE|roommate");

        String firstScenarioContext = jdbcTemplate.queryForObject("""
                SELECT briefing || '|' || conversation_goal
                FROM scenarios
                WHERE id = 1
                """, String.class);

        assertThat(firstScenarioContext)
                .isEqualTo("입주 첫날 룸메이트 Charlie와 처음 인사하고, 자기소개와 취미, 한국 추천 관광지에 대해 이야기합니다."
                        + "|이름과 자기소개를 자연스럽게 말하고, 취미의 매력과 추천 장소의 이유를 영어로 설명한다.");

        List<Integer> questionCounts = jdbcTemplate.queryForList("""
                SELECT COUNT(*)
                FROM scenario_questions q
                JOIN scenarios s ON s.id = q.scenario_id
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = '룸메이트'
                GROUP BY s.id
                ORDER BY s.display_order
                """, Integer.class);

        assertThat(questionCounts).containsExactly(3, 4, 4);

        List<String> questions = jdbcTemplate.queryForList("""
                SELECT s.id || '|' || q.sequence || '|' || q.question_en || '|' || q.question_ko
                FROM scenario_questions q
                JOIN scenarios s ON s.id = q.scenario_id
                JOIN categories c ON c.id = s.category_id
                WHERE c.name = '룸메이트'
                ORDER BY s.display_order, q.sequence
                """, String.class);

        assertThat(questions).containsExactly(
                "1|1|Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!|안녕 너 내 룸메이트지?! 난 charlie야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라.",
                "1|2|What are you into? What do you love about it?|취미는 뭐야? 그게 어떤 매력이 있어?",
                "1|3|I'm obsessed with Korea! Tell me your must-visit spots and why I should go!|나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!",
                "2|1|We should totally hang out this weekend! Does Saturday or Sunday work better for you?|우리 이번 주말에 꼭 놀자! 토요일이랑 일요일 중에 언제가 더 좋아?",
                "2|2|So what do you usually do for fun? Or is there anything you've been dying to try ever since you got here?|넌 보통 뭐하고 놀아? 아님 여기 와서 꼭 해보고 싶었던 거 있어?",
                "2|3|Oh my god, I almost forgot to tell you — Dude, guess what — I finally got that internship I told you about! I'm freaking out, I'm so happy!|헐 너한테 말하는 거 까먹을 뻔 — 야, 있잖아 — 나 너한테 말했던 그 인턴십 드디어 됐어! 너무 신나, 진짜 행복해!",
                "2|4|Oh shoot, is it already that late? I need to run to the store before it closes! Wanna come with? Or do you need me to grab you anything?|어 벌써 시간이 이렇게 됐네? 마트 문 닫기 전에 얼른 가야겠다! 같이 갈래? 아님 내가 뭐 사다 줄까?",
                "3|1|Okay, let's play Truth or Dare — well, just the truth part. Ask me anything and I'll answer 100% honestly!|자 우리 진실게임하자 — 아니 벌칙은 빼고 진실만. 아무거나 물어봐, 100% 솔직하게 답할게!",
                "3|2|Okay, my turn to ask! I'm curious about you — what's your big dream? And what made you pick your major?|좋아, 이제 내가 물어볼 차례! 나 궁금한 거 있어 — 너는 꿈이 뭐야? 그리고 왜 그 전공을 선택했어?",
                "3|3|You've seemed kinda off lately — everything okay? You know you can talk to me, right? Come on, tell me everything!|너 요즘 좀 기운없어 보여 — 다 괜찮아? 나한테 얘기해도 되는 거 알지? 자, 나한테 다 털어놔봐!",
                "3|4|Okay, let's get some sleep! Oh, but before we do... don't get mad, okay? You were snoring SO loud last night I genuinely thought there was a tiny monster under your bed, I swear, hahaha! Try not to snore tonight, okay~?|좋아 이제 자자! 아 근데 자기 전에.. 화내지 말고 들어, 아니 너 어젯밤에 코를 너무 크게 골아서 침대 밑에 뭔 애기 몬스터라도 있는 줄 알았잖아 진심ㅋㅋㅋ 오늘은 코 골지 마라~~?");
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
