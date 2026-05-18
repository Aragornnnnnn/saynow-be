// 소셜 로그인과 SayNow token 발급 흐름을 처리하는 서비스
package com.saynow.auth.application;

import com.saynow.auth.api.dto.AuthUserResponse;
import com.saynow.auth.api.dto.AuthTokenResponse;
import com.saynow.auth.api.dto.LogoutRequest;
import com.saynow.auth.api.dto.SocialLoginRequest;
import com.saynow.auth.api.dto.TokenRefreshRequest;
import com.saynow.auth.api.dto.TokenRefreshResponse;
import com.saynow.auth.domain.User;
import com.saynow.auth.domain.RefreshToken;
import com.saynow.auth.domain.SocialProvider;
import com.saynow.auth.infrastructure.UserRepository;
import com.saynow.auth.infrastructure.RefreshTokenRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final OidcTokenVerifier oidcTokenVerifier;
    private final SaynowTokenService saynowTokenService;
    private final TokenProperties tokenProperties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthTokenResponse socialLogin(SocialLoginRequest request) {
        SocialProvider provider = parseProvider(request.provider());
        OidcUserInfo userInfo = oidcTokenVerifier.verify(provider, request.idToken(), request.nonce());
        User user = userRepository.findByProviderAndSubAndDeletedAtIsNull(provider, userInfo.subject())
                .orElse(null);

        boolean newUser = user == null;
        if (newUser) {
            user = userRepository.save(new User(userInfo.nickname(), userInfo.email(), userInfo.subject(), provider));
        } else {
            user.updateProfile(userInfo.nickname(), userInfo.email());
        }

        IssuedTokens issuedTokens = issueTokens(user);
        return new AuthTokenResponse(
                TOKEN_TYPE,
                issuedTokens.accessToken(),
                tokenProperties.getAccessExpiresInSeconds(),
                issuedTokens.refreshToken(),
                tokenProperties.getRefreshExpiresInSeconds(),
                new AuthUserResponse(user.getId().toString(), user.getNickname(), user.getEmail(), provider.name(), newUser));
    }

    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(saynowTokenService.hashRefreshToken(request.refreshToken()))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (!refreshToken.isActive(now)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshToken.revoke(now);
        IssuedTokens issuedTokens = issueTokens(refreshToken.getUser());
        return new TokenRefreshResponse(
                TOKEN_TYPE,
                issuedTokens.accessToken(),
                tokenProperties.getAccessExpiresInSeconds(),
                issuedTokens.refreshToken(),
                tokenProperties.getRefreshExpiresInSeconds());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(saynowTokenService.hashRefreshToken(request.refreshToken()))
                .ifPresent(refreshToken -> refreshToken.revoke(LocalDateTime.now()));
    }

    @Transactional
    public void withdraw(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));

        refreshTokenRepository.revokeAllActiveByUserId(userId, now);
        user.withdraw(now);
    }

    private IssuedTokens issueTokens(User user) {
        String accessToken = saynowTokenService.createAccessToken(user.getId());
        String refreshToken = saynowTokenService.createRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                user,
                saynowTokenService.hashRefreshToken(refreshToken),
                LocalDateTime.now().plusSeconds(tokenProperties.getRefreshExpiresInSeconds())));
        return new IssuedTokens(accessToken, refreshToken);
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.valueOf(provider);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }
    }

    private record IssuedTokens(String accessToken, String refreshToken) {
    }
}
