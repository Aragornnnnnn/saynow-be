// 앱 버전 정책을 조회하고 업데이트 필요 수준을 계산하는 서비스
package com.saynow.appversion.application;

import com.saynow.appversion.api.dto.AppVersionCheckResponse;
import com.saynow.appversion.domain.AppPlatform;
import com.saynow.appversion.domain.AppUpdateType;
import com.saynow.appversion.domain.AppVersion;
import com.saynow.appversion.infrastructure.AppVersionRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    public AppVersionCheckResponse check(String platformValue, String buildNumberValue, String versionName) {
        AppPlatform platform = AppPlatform.from(platformValue);
        long currentBuildNumber = parseBuildNumber(buildNumberValue);

        return appVersionRepository.findFirstByPlatformAndActiveTrueOrderByBuildNumberDesc(platform)
                .map(appVersion -> toResponse(appVersion, currentBuildNumber))
                .orElseGet(() -> AppVersionCheckResponse.noPolicy(versionName, currentBuildNumber));
    }

    private long parseBuildNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        try {
            long buildNumber = Long.parseLong(value.trim());
            if (buildNumber < 1) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            }
            return buildNumber;
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private AppVersionCheckResponse toResponse(AppVersion appVersion, long currentBuildNumber) {
        if (currentBuildNumber < appVersion.getMinimumSupportedBuildNumber()) {
            return AppVersionCheckResponse.from(appVersion, AppUpdateType.FORCE, appVersion.getForceUpdateReason());
        }

        if (currentBuildNumber < appVersion.getBuildNumber()) {
            return AppVersionCheckResponse.from(appVersion, AppUpdateType.SOFT, appVersion.getSoftUpdateReason());
        }

        return AppVersionCheckResponse.from(appVersion, AppUpdateType.NONE, null);
    }
}
