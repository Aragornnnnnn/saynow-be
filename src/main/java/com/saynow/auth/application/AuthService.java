// 소셜 로그인과 SayNow token 발급 흐름을 처리하는 서비스
package com.saynow.auth.application;

import com.saynow.auth.api.dto.AuthMemberResponse;
import com.saynow.auth.api.dto.AuthTokenResponse;
import com.saynow.auth.api.dto.LogoutRequest;
import com.saynow.auth.api.dto.SocialLoginRequest;
import com.saynow.auth.api.dto.TokenRefreshRequest;
import com.saynow.auth.api.dto.TokenRefreshResponse;
import com.saynow.auth.domain.Member;
import com.saynow.auth.domain.RefreshToken;
import com.saynow.auth.domain.SocialAccount;
import com.saynow.auth.domain.SocialProvider;
import com.saynow.auth.infrastructure.MemberRepository;
import com.saynow.auth.infrastructure.RefreshTokenRepository;
import com.saynow.auth.infrastructure.SocialAccountRepository;
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
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthTokenResponse socialLogin(SocialLoginRequest request) {
        SocialProvider provider = parseProvider(request.provider());
        OidcUserInfo userInfo = oidcTokenVerifier.verify(provider, request.idToken(), request.nonce());
        SocialAccount socialAccount = socialAccountRepository.findByProviderAndProviderSubject(provider, userInfo.subject())
                .orElse(null);

        boolean newMember = socialAccount == null;
        Member member;
        if (newMember) {
            member = memberRepository.save(new Member(userInfo.nickname(), userInfo.email()));
            socialAccount = socialAccountRepository.save(new SocialAccount(
                    member,
                    provider,
                    userInfo.subject(),
                    userInfo.email(),
                    userInfo.emailVerified(),
                    userInfo.nickname()));
        } else {
            member = socialAccount.getMember();
            member.updateProfile(userInfo.nickname(), userInfo.email());
            socialAccount.updateProfile(userInfo.email(), userInfo.emailVerified(), userInfo.nickname());
        }

        IssuedTokens issuedTokens = issueTokens(member);
        return new AuthTokenResponse(
                TOKEN_TYPE,
                issuedTokens.accessToken(),
                tokenProperties.getAccessExpiresInSeconds(),
                issuedTokens.refreshToken(),
                tokenProperties.getRefreshExpiresInSeconds(),
                new AuthMemberResponse(member.getId().toString(), member.getNickname(), member.getEmail(), provider.name(), newMember));
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
        IssuedTokens issuedTokens = issueTokens(refreshToken.getMember());
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
    public void withdraw(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        Member member = memberRepository.findByIdAndWithdrawnAtIsNull(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));

        refreshTokenRepository.revokeAllActiveByMemberId(memberId, now);
        socialAccountRepository.deleteByMemberId(memberId);
        member.withdraw(now);
    }

    private IssuedTokens issueTokens(Member member) {
        String accessToken = saynowTokenService.createAccessToken(member.getId());
        String refreshToken = saynowTokenService.createRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                member,
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
