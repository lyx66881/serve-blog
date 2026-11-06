package com.blog.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：上传文件静态资源映射。
 * <p>
 * GET /uploads/** 映射到本地磁盘上传目录（app.upload.base-dir），
 * 例如 /uploads/avatar/2026/11/xxx.png -> {base-dir}/avatar/2026/11/xxx.png
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = appProperties.getUpload().getBaseDir();
        String location = Paths.get(baseDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(appProperties.getUpload().getBaseUrl() + "/**")
                .addResourceLocations(location);
    }
}
