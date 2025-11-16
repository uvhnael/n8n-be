package org.uvhnael.fbadsbe2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FB Ads Analytics & Content Generator API")
                        .version("1.0.0")
                        .description("""
                                Backend API Spring Boot cho hệ thống:
                                - 📊 Quản lý & phân tích Facebook Ads từ n8n
                                - 🤖 Auto tạo bài viết dựa trên trend phân tích (AI-powered)
                                - ⏰ Hẹn giờ đăng bài tự động
                                - 📈 Lưu trữ insights, keywords, CTAs
                                - 📅 Dashboard analytics
                                
                                ## Features:
                                - **Ads Management**: CRUD operations for Facebook Ads from n8n workflow
                                - **Content Generator**: AI-powered content generation using Google Gemini
                                - **Scheduled Posts**: Auto-publish posts at scheduled times
                                - **Insights & Analytics**: Track and analyze ad performance
                                - **Trend Analysis**: AI-based trend detection and suggestions
                                
                                ## Authentication:
                                Most endpoints require JWT authentication. Get your token from `/api/auth/login`.
                                """)
                        .contact(new Contact()
                                .name("FB Ads Team")
                                .email("support@fbads.com")
                                .url("https://github.com/uvhnael/n8n-be"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.fbads.com")
                                .description("Production Server (if applicable)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token for authentication. Format: `Bearer <token>`"));
    }
}
