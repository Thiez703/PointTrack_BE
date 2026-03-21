package com.teco.pointtrack.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PointTrack API",
                version = "1.0.0",
                description = "PointTrack Backend API - Authentication & Authorization starter."
        ),
        security = {@SecurityRequirement(name = "bearerAuth")}
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${APP_SERVER_URL:http://localhost:8080}") String serverUrl) {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(serverUrl).description("Default Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Token vào đây (không cần từ khóa Bearer)")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0. All APIs")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1. Authentication")
                .pathsToMatch("/auth/**", "/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi personnelApi() {
        return GroupedOpenApi.builder()
                .group("2. Personnel")
                .pathsToMatch("/personnel/**", "/v1/personnel/**", "/salary-levels/**", "/v1/salary-levels/**", "/customers/**", "/v1/customers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi attendanceApi() {
        return GroupedOpenApi.builder()
                .group("4. Attendance")
                .pathsToMatch("/attendance/**", "/v1/attendance/**")
                .build();
    }

    @Bean
    public GroupedOpenApi schedulingApi() {
        return GroupedOpenApi.builder()
                .group("3. Scheduling")
                .pathsToMatch("/scheduling/**", "/v1/scheduling/**")
                .build();
    }
}
