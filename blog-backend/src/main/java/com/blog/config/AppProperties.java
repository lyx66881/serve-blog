package com.blog.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用自定义配置（application.yml 中 app.* 前缀）。
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Cors cors = new Cors();
    private PasswordReset passwordReset = new PasswordReset();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public void setPasswordReset(PasswordReset passwordReset) { this.passwordReset = passwordReset; }

    public static class Jwt {
        private String secret;
        /** token 有效期（小时） */
        private long expireHours = 72;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpireHours() { return expireHours; }
        public void setExpireHours(long expireHours) { this.expireHours = expireHours; }
    }

    public static class Upload {
        /** 上传文件根目录 */
        private String baseDir;
        /** 静态资源访问前缀 */
        private String baseUrl = "/uploads";
        /** 单文件大小上限（MB） */
        private int maxSizeMb = 10;

        public String getBaseDir() { return baseDir; }
        public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getMaxSizeMb() { return maxSizeMb; }
        public void setMaxSizeMb(int maxSizeMb) { this.maxSizeMb = maxSizeMb; }
    }

    public static class Cors {
        /** 允许的跨域来源，逗号分隔，* 表示全部 */
        private List<String> allowedOrigins = List.of("*");

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class PasswordReset {
        /** 验证码有效期（分钟） */
        private int expireMinutes = 10;
        /** 可再次发送的最小间隔（秒） */
        private int resendIntervalSeconds = 60;
        /** 验证失败次数上限，达到后验证码作废 */
        private int maxAttempts = 5;
        /** 未接入邮件服务前是否将验证码随响应返回（模拟发送）；接入 SMTP 后应置为 false */
        private boolean exposeCode = true;

        public int getExpireMinutes() { return expireMinutes; }
        public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
        public int getResendIntervalSeconds() { return resendIntervalSeconds; }
        public void setResendIntervalSeconds(int resendIntervalSeconds) { this.resendIntervalSeconds = resendIntervalSeconds; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public boolean isExposeCode() { return exposeCode; }
        public void setExposeCode(boolean exposeCode) { this.exposeCode = exposeCode; }
    }
}
