package com.mocamp.mocamp_backend.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Mocamp Backend API Document")
                .version("v0.0.1")
                .description("서버의 API 명세를 정리한 페이지입니다");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}
