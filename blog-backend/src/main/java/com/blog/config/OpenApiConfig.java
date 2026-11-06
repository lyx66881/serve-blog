package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger / OpenAPI 文档配置。
 * <p>
 * 访问地址：http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("登录/注册接口返回的 accessToken，请求时放入 Authorization: Bearer <token>");

        return new OpenAPI()
                .info(new Info()
                        .title("博客平台 API")
                        .description("博客平台后端服务：用户认证、博客管理（TipTap 富文本）、图片上传。")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
