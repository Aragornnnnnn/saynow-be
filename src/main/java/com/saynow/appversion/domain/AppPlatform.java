// 앱 버전 정책을 구분하는 클라이언트 플랫폼 enum
package com.saynow.appversion.domain;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;

import java.util.Locale;

public enum AppPlatform {
    IOS,
    ANDROID;

    public static AppPlatform from(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        try {
            return AppPlatform.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
