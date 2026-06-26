// 개발 배포 프로파일에서 원격 AI 클라이언트가 선택되는지 검증한다.
package com.saynow;

import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiClientProperties;
import com.saynow.session.infrastructure.ai.RemoteAiConversationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.datasource.url=jdbc:h2:mem:saynow-dev;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureMockMvc
class DevAiClientModeTest {

    @Autowired
    private Environment environment;

    @Autowired
    private AiConversationClient aiConversationClient;

    @Autowired
    private AiClientProperties aiClientProperties;

    @Test
    void devProfileUsesRemoteAiClientByDefault() {
        assertThat(environment.getProperty("saynow.ai.client-mode")).isEqualTo("remote");
        assertThat(environment.getProperty("sentry.environment")).isEqualTo("dev");
        assertThat(aiConversationClient).isInstanceOf(RemoteAiConversationClient.class);
        assertThat(aiClientProperties.serviceAudience().name()).isEqualTo("KOREAN_LEARNER");
    }
}
