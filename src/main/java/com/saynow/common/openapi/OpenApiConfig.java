package com.saynow.common.openapi;

import com.saynow.auth.api.AuthController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String PROD_SERVER_URL = "https://saynow.p-e.kr";
    private static final String DEV_SERVER_URL = "https://dev-saynow.p-e.kr";
    private static final Set<String> PUBLIC_AUTH_METHODS = Set.of("socialLogin", "refresh");

    private final String serverUrl;

    public OpenApiConfig(
            @Value("${saynow.openapi.server-url:https://saynow.p-e.kr}") String serverUrl,
            Environment environment
    ) {
        this.serverUrl = resolveServerUrl(serverUrl, environment);
    }

    @Bean
    public OpenAPI saynowOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl)))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .security(List.of(new SecurityRequirement().addList(BEARER_AUTH)))
                .info(new Info()
                        .title("SayNow Backend API")
                        .version("v1")
                        .description("SayNow MVP backend API documentation."));
    }

    @Bean
    public OperationCustomizer publicAuthOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().equals(AuthController.class)
                    && PUBLIC_AUTH_METHODS.contains(handlerMethod.getMethod().getName())) {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }

    private String resolveServerUrl(String configuredServerUrl, Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("dev")) && isProdServerUrl(configuredServerUrl)) {
            return DEV_SERVER_URL;
        }
        return configuredServerUrl;
    }

    private boolean isProdServerUrl(String configuredServerUrl) {
        if (configuredServerUrl == null) {
            return false;
        }
        return PROD_SERVER_URL.equals(stripTrailingSlash(configuredServerUrl.trim()));
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
