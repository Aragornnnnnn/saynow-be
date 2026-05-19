// 개발 서버 EC2 배포 workflow의 환경 분리를 검증한다.
package com.saynow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DevDeploymentWorkflowTest {

    @Test
    void devDeployWorkflowUsesDevelopEnvironmentAndParameterPath() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy-dev-ec2.yml"));

        assertThat(workflow).contains("name: Deploy Dev EC2");
        assertThat(workflow).contains("group: deploy-dev-ec2");
        assertThat(workflow).contains("PARAMETER_PATH: /saynow/develop");
        assertThat(workflow).contains("name: Deploy dev to EC2");
        assertThat(workflow).contains("environment: develop");
        assertThat(workflow).contains("~/.ssh/saynow_dev");
        assertThat(workflow).contains("values[\"SPRING_PROFILES_ACTIVE\"] = \"dev\"");
    }

    @Test
    void devDeployWorkflowKeepsProdDeploymentMechanics() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy-dev-ec2.yml"));

        assertThat(workflow).contains("uses: actions/setup-java@v4");
        assertThat(workflow).contains("run: ./gradlew clean build");
        assertThat(workflow).contains("aws-actions/configure-aws-credentials@v4");
        assertThat(workflow).contains("aws ec2 authorize-security-group-ingress");
        assertThat(workflow).contains("aws ssm get-parameters-by-path");
        assertThat(workflow).contains("sudo systemctl restart \"${SERVICE_NAME}\"");
        assertThat(workflow).contains("http://127.0.0.1:${APP_PORT}/actuator/health");
        assertThat(workflow).contains("aws ec2 revoke-security-group-ingress");
    }
}
