// 앱 실행 시 버전 업데이트 필요 여부를 조회하는 공개 API 컨트롤러
package com.saynow.appversion.api;

import com.saynow.appversion.api.dto.AppVersionCheckResponse;
import com.saynow.appversion.application.AppVersionService;
import com.saynow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app-versions")
@RequiredArgsConstructor
@Tag(name = "App Version", description = "앱 버전 업데이트 정책 조회 API")
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping("/check")
    @Operation(summary = "앱 버전 업데이트 확인", description = "클라이언트 플랫폼과 빌드 번호 기준으로 강제/소프트 업데이트 필요 여부를 조회합니다.")
    public ResponseEntity<ApiResponse<AppVersionCheckResponse>> checkAppVersion(
            @RequestParam String platform,
            @RequestParam String buildNumber,
            @RequestParam(required = false) String versionName
    ) {
        return ApiResponse.success(HttpStatus.OK, appVersionService.check(platform, buildNumber, versionName));
    }
}
