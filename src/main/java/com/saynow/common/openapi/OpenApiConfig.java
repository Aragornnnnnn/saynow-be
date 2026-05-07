package com.saynow.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI saynowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SayNow Backend API")
                        .version("v1")
                        .description("SayNow MVP backend API documentation."));
    }
}
