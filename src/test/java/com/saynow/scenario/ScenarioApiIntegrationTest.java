package com.saynow.scenario;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScenarioApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsActiveCategoriesInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.categories", hasSize(5)))
                .andExpect(jsonPath("$.data.categories[0].categoryId").value("airport"))
                .andExpect(jsonPath("$.data.categories[0].name").value("공항"))
                .andExpect(jsonPath("$.data.categories[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data.categories[4].categoryId").value("taxi"));
    }

    @Test
    void listsScenariosForCategoryWithoutInternalSlotData() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios")
                        .param("categoryId", "cafe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios", hasSize(2)))
                .andExpect(jsonPath("$.data.scenarios[0].scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.data.scenarios[0].categoryId").value("cafe"))
                .andExpect(jsonPath("$.data.scenarios[0].title").value("아이스 아메리카노 주문하기"))
                .andExpect(jsonPath("$.data.scenarios[0].difficulty").value("쉬움"))
                .andExpect(jsonPath("$.data.scenarios[0].situationDescription").value("카페에서 원하는 음료를 주문해야 합니다."))
                .andExpect(jsonPath("$.data.scenarios[0].successGoal").value("아이스 아메리카노 주문에 성공하세요."))
                .andExpect(jsonPath("$.data.scenarios[0].thumbnailUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.scenarios[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data.scenarios[0].filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.scenarios[0].missingSlots").doesNotExist());
    }

    @Test
    void listsAllScenariosWithCategoryIdWithoutInternalSlotData() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios", hasSize(10)))
                .andExpect(jsonPath("$.data.scenarios[0].scenarioId").value("airport_immigration"))
                .andExpect(jsonPath("$.data.scenarios[0].categoryId").value("airport"))
                .andExpect(jsonPath("$.data.scenarios[0].title").value("입국 심사 통과하기"))
                .andExpect(jsonPath("$.data.scenarios[0].difficulty").value("보통"))
                .andExpect(jsonPath("$.data.scenarios[0].situationDescription").value("입국 심사대에서 여행 목적과 체류 기간을 설명해야 합니다."))
                .andExpect(jsonPath("$.data.scenarios[0].successGoal").value("여행 목적과 체류 기간을 말하고 입국 심사를 통과하세요."))
                .andExpect(jsonPath("$.data.scenarios[0].thumbnailUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.scenarios[0].filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.scenarios[0].missingSlots").doesNotExist());
    }

    @Test
    void listsAllScenariosWhenCategoryIdIsAll() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios")
                        .param("categoryId", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios", hasSize(10)))
                .andExpect(jsonPath("$.data.scenarios[0].categoryId").value("airport"))
                .andExpect(jsonPath("$.data.scenarios[9].scenarioId").value("taxi_card_payment"));
    }

    @Test
    void returnsNotFoundForUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios")
                        .param("categoryId", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("카테고리를 찾을 수 없습니다."));
    }
}
