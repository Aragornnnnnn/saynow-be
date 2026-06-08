// 앱 버전 정책 조회 API의 업데이트 판단 결과를 검증한다.
package com.saynow.appversion;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppVersionApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM app_versions");
    }

    @Test
    void returnsForceUpdateWhenBuildIsBelowMinimumSupportedBuild() throws Exception {
        insertIosPolicy();

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "14")
                        .param("versionName", "1.3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("FORCE"))
                .andExpect(jsonPath("$.data.latestVersionName").value("1.4.0"))
                .andExpect(jsonPath("$.data.latestBuildNumber").value(18))
                .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(15))
                .andExpect(jsonPath("$.data.reason").value("이전 버전에서 로그인 안정성 문제가 있어 업데이트가 필요합니다."))
                .andExpect(jsonPath("$.data.storeUrl").doesNotExist())
                .andExpect(jsonPath("$.data.releasedAt").isString());
    }

    @Test
    void returnsSoftUpdateWhenBuildIsSupportedButBelowLatestBuild() throws Exception {
        insertIosPolicy();

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("SOFT"))
                .andExpect(jsonPath("$.data.latestVersionName").value("1.4.0"))
                .andExpect(jsonPath("$.data.latestBuildNumber").value(18))
                .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(15))
                .andExpect(jsonPath("$.data.reason").value("새로운 대화 품질 개선이 포함되어 있습니다."))
                .andExpect(jsonPath("$.data.storeUrl").doesNotExist());
    }

    @Test
    void returnsNoUpdateWhenBuildIsLatestOrNewer() throws Exception {
        insertIosPolicy();

        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "IOS")
                        .param("buildNumber", "18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("NONE"))
                .andExpect(jsonPath("$.data.latestVersionName").value("1.4.0"))
                .andExpect(jsonPath("$.data.latestBuildNumber").value(18))
                .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(15))
                .andExpect(jsonPath("$.data.reason").value(nullValue()))
                .andExpect(jsonPath("$.data.storeUrl").doesNotExist());
    }

    @Test
    void returnsNoUpdateWhenPlatformPolicyDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "ANDROID")
                        .param("buildNumber", "3")
                        .param("versionName", "1.0.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateType").value("NONE"))
                .andExpect(jsonPath("$.data.latestVersionName").value("1.0.2"))
                .andExpect(jsonPath("$.data.latestBuildNumber").value(3))
                .andExpect(jsonPath("$.data.minimumSupportedBuildNumber").value(3))
                .andExpect(jsonPath("$.data.reason").value(nullValue()))
                .andExpect(jsonPath("$.data.storeUrl").doesNotExist())
                .andExpect(jsonPath("$.data.releasedAt").value(nullValue()));
    }

    @Test
    void rejectsUnsupportedPlatform() throws Exception {
        mockMvc.perform(get("/api/v1/app-versions/check")
                        .param("platform", "WINDOWS")
                        .param("buildNumber", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    private void insertIosPolicy() {
        jdbcTemplate.update("""
                INSERT INTO app_versions (
                    platform,
                    version_name,
                    build_number,
                    minimum_supported_build_number,
                    force_update_reason,
                    soft_update_reason,
                    release_note,
                    active,
                    released_at,
                    created_at,
                    updated_at
                )
                VALUES (
                    'IOS',
                    '1.4.0',
                    18,
                    15,
                    '이전 버전에서 로그인 안정성 문제가 있어 업데이트가 필요합니다.',
                    '새로운 대화 품질 개선이 포함되어 있습니다.',
                    '로그인 안정성과 대화 품질을 개선했습니다.',
                    TRUE,
                    TIMESTAMP '2026-06-09 12:00:00',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """);
    }
}
