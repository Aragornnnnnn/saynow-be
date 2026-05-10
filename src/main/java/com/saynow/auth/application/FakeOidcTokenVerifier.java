// 테스트 환경에서 외부 provider 없이 OIDC 사용자 정보를 만드는 verifier
package com.saynow.auth.application;

import com.saynow.auth.domain.SocialProvider;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "saynow.auth.oidc.fake-enabled", havingValue = "true")
public class FakeOidcTokenVerifier implements OidcTokenVerifier {

    @Override
    public OidcUserInfo verify(SocialProvider provider, String idToken, String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
        }
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }

        String[] parts = idToken.split("\\|", -1);
        if (parts.length < 3 || parts[0].isBlank()) {
            throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
        }
        return new OidcUserInfo(
                parts[0],
                parts[1].isBlank() ? null : parts[1],
                parts[1].isBlank() ? null : true,
                parts[2].isBlank() ? null : parts[2]
        );
    }
}
