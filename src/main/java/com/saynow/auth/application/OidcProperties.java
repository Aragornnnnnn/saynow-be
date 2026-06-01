// OIDC provider 검증에 필요한 audience 설정을 담는 프로퍼티
package com.saynow.auth.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "saynow.auth.oidc")
public class OidcProperties {

    private boolean fakeEnabled = false;
    private boolean nonceRequired = true;
    private List<String> googleAudiences = new ArrayList<>();
    private List<String> kakaoAudiences = new ArrayList<>();
}
