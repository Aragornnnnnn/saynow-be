package com.saynow.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private final String serverUrl;

    public OpenApiConfig(@Value("${saynow.openapi.server-url:https://saynow.p-e.kr}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Bean
    public OpenAPI saynowOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl)))
                .info(new Info()
                        .title("SayNow Backend API")
                        .version("v1")
                        .description("SayNow MVP backend API documentation."));
    }
}
