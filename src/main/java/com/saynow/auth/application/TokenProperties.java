// SayNow access token과 refresh token의 발급 설정을 담는 프로퍼티
package com.saynow.auth.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "saynow.auth.token")
public class TokenProperties {

    private String secret = "saynow-development-token-secret-change-before-production";
    private long accessExpiresInSeconds = 1800;
    private long refreshExpiresInSeconds = 1209600;
}
