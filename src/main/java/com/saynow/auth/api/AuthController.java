// 소셜 로그인과 SayNow token 관리를 제공하는 REST 컨트롤러
package com.saynow.auth.api;

import com.saynow.auth.api.dto.AuthTokenResponse;
import com.saynow.auth.api.dto.LogoutRequest;
import com.saynow.auth.api.dto.SocialLoginRequest;
import com.saynow.auth.api.dto.TokenRefreshRequest;
import com.saynow.auth.api.dto.TokenRefreshResponse;
import com.saynow.auth.application.AuthService;
import com.saynow.auth.security.AuthUserPrincipal;
import com.saynow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "소셜 로그인 인증 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social-login")
    @Operation(summary = "소셜 로그인", description = "provider ID Token을 검증하고 SayNow token을 발급합니다.")
    public ApiResponse<AuthTokenResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.success(authService.socialLogin(request));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "토큰 재발급", description = "refresh token을 회전하고 새 SayNow token을 발급합니다.")
    public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refresh token을 폐기합니다.")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/me")
    @Operation(summary = "사용자 탈퇴", description = "현재 사용자을 탈퇴 처리하고 refresh token과 소셜 계정 연결을 정리합니다.")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthUserPrincipal principal) {
        authService.withdraw(principal.userId());
        return ApiResponse.success(null);
    }
}
