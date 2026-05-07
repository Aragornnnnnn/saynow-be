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
                .andExpect(jsonPath("$.categories", hasSize(5)))
                .andExpect(jsonPath("$.categories[0].categoryId").value("airport"))
                .andExpect(jsonPath("$.categories[0].name").value("공항"))
                .andExpect(jsonPath("$.categories[0].sortOrder").value(1))
                .andExpect(jsonPath("$.categories[4].categoryId").value("taxi"));
    }

    @Test
    void listsScenariosForCategoryWithoutInternalSlotData() throws Exception {
        mockMvc.perform(get("/api/v1/categories/cafe/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value("cafe"))
                .andExpect(jsonPath("$.scenarios", hasSize(2)))
                .andExpect(jsonPath("$.scenarios[0].scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.scenarios[0].title").value("아이스 아메리카노 주문하기"))
                .andExpect(jsonPath("$.scenarios[0].successGoal").value("아이스 아메리카노 주문에 성공하세요."))
                .andExpect(jsonPath("$.scenarios[0].thumbnailUrl").value(nullValue()))
                .andExpect(jsonPath("$.scenarios[0].filledSlots").doesNotExist())
                .andExpect(jsonPath("$.scenarios[0].missingSlots").doesNotExist());
    }

    @Test
    void returnsScenarioDetailForModalOnly() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios/cafe_iced_americano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.categoryId").value("cafe"))
                .andExpect(jsonPath("$.title").value("아이스 아메리카노 주문하기"))
                .andExpect(jsonPath("$.situationDescription").value("카페에서 원하는 음료를 주문해야 합니다."))
                .andExpect(jsonPath("$.successGoal").value("아이스 아메리카노 주문에 성공하세요."))
                .andExpect(jsonPath("$.openingBabsaeText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.openingTtsUrl").value(nullValue()))
                .andExpect(jsonPath("$.maxFollowUpCount").value(5))
                .andExpect(jsonPath("$.requiredSlots").doesNotExist())
                .andExpect(jsonPath("$.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.missingSlots").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/categories/unknown/scenarios"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
