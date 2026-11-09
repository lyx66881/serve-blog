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
    private Mail mail = new Mail();
    private EmailCode emailCode = new EmailCode();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public EmailCode getEmailCode() { return emailCode; }
    public void setEmailCode(EmailCode emailCode) { this.emailCode = emailCode; }

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

    public static class Mail {
        /** 是否启用真实邮件发送；false 时为模拟模式（验证码仅记日志并随响应返回 devCode） */
        private boolean enabled = false;
        /** 发件人（如 no-reply@blog.com，须与 SMTP 账号一致） */
        private String from = "no-reply@blog.local";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class EmailCode {
        /** 验证码有效期（分钟） */
        private int expireMinutes = 10;
        /** 可再次发送的最小间隔（秒） */
        private int resendIntervalSeconds = 60;
        /** 验证失败次数上限，达到后验证码作废 */
        private int maxAttempts = 5;

        public int getExpireMinutes() { return expireMinutes; }
        public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
        public int getResendIntervalSeconds() { return resendIntervalSeconds; }
        public void setResendIntervalSeconds(int resendIntervalSeconds) { this.resendIntervalSeconds = resendIntervalSeconds; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }
}
