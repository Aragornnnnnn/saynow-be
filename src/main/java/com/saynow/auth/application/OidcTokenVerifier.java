// provider ID Token을 검증하고 사용자 claim을 반환하는 인터페이스
package com.saynow.auth.application;

import com.saynow.auth.domain.SocialProvider;

public interface OidcTokenVerifier {

    OidcUserInfo verify(SocialProvider provider, String idToken, String nonce);
}
