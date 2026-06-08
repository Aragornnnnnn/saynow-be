// 앱 업데이트 필요 수준과 최신 버전 정보를 내려주는 응답 DTO
package com.saynow.appversion.api.dto;

import com.saynow.appversion.domain.AppUpdateType;
import com.saynow.appversion.domain.AppVersion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "앱 버전 업데이트 확인 응답")
public record AppVersionCheckResponse(
        @Schema(description = "업데이트 필요 수준", example = "SOFT")
        AppUpdateType updateType,

        @Schema(description = "서버가 알고 있는 최신 앱 버전명", example = "1.4.0")
        String latestVersionName,

        @Schema(description = "서버가 알고 있는 최신 빌드 번호", example = "18")
        long latestBuildNumber,

        @Schema(description = "서버가 허용하는 최소 빌드 번호", example = "15")
        long minimumSupportedBuildNumber,

        @Schema(description = "업데이트 안내 사유. 업데이트가 필요 없으면 null입니다.")
        String reason,

        @Schema(description = "최신 버전 릴리스 시각. 정책이 없으면 null입니다.")
        LocalDateTime releasedAt
) {

    public static AppVersionCheckResponse from(AppVersion appVersion, AppUpdateType updateType, String reason) {
        return new AppVersionCheckResponse(
                updateType,
                appVersion.getVersionName(),
                appVersion.getBuildNumber(),
                appVersion.getMinimumSupportedBuildNumber(),
                reason,
                appVersion.getReleasedAt());
    }

    public static AppVersionCheckResponse noPolicy(String versionName, long buildNumber) {
        return new AppVersionCheckResponse(
                AppUpdateType.NONE,
                versionName,
                buildNumber,
                buildNumber,
                null,
                null);
    }
}
