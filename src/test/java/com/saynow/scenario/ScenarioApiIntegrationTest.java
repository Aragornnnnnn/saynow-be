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
        mockMvc.perform(get("/api/v1/categories/cafe/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value("cafe"))
                .andExpect(jsonPath("$.data.scenarios", hasSize(2)))
                .andExpect(jsonPath("$.data.scenarios[0].scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.data.scenarios[0].title").value("아이스 아메리카노 주문하기"))
                .andExpect(jsonPath("$.data.scenarios[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.data.scenarios[0].successGoal").value("아이스 아메리카노 주문에 성공하세요."))
                .andExpect(jsonPath("$.data.scenarios[0].thumbnailUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.scenarios[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data.scenarios[0].filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.scenarios[0].missingSlots").doesNotExist());
    }

    @Test
    void returnsScenarioDetailForModalOnly() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios/cafe_iced_americano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.data.categoryId").value("cafe"))
                .andExpect(jsonPath("$.data.title").value("아이스 아메리카노 주문하기"))
                .andExpect(jsonPath("$.data.difficulty").value("EASY"))
                .andExpect(jsonPath("$.data.situationDescription").value("카페에서 원하는 음료를 주문해야 합니다."))
                .andExpect(jsonPath("$.data.successGoal").value("아이스 아메리카노 주문에 성공하세요."))
                .andExpect(jsonPath("$.data.openingBabsaeText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.data.openingTtsUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.maxFollowUpCount").value(5))
                .andExpect(jsonPath("$.data.requiredSlots").doesNotExist())
                .andExpect(jsonPath("$.data.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.missingSlots").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/categories/unknown/scenarios"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("카테고리를 찾을 수 없습니다."));
    }
}
