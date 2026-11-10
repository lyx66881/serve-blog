package com.blog.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.blog.config.AppProperties;

import jakarta.mail.internet.MimeMessage;

/**
 * 邮件发送服务。
 * app.mail.enabled=true 且已配置 SMTP 时真实发送；否则进入「模拟模式」：
 * 只记录日志并返回 false，由调用方决定是否将验证码随响应返回（devCode）。
 */
@Service
public class MailSenderService {

    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties appProperties;

    public MailSenderService(ObjectProvider<JavaMailSender> mailSenderProvider, AppProperties appProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.appProperties = appProperties;
    }

    public boolean isRealSendEnabled() {
        return appProperties.getMail().isEnabled() && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * 发送验证码邮件，返回是否真实发送成功（模拟模式返回 false）。
     */
    public boolean sendVerifyCode(String to, String code, String sceneLabel, int expireMinutes) {
        if (!isRealSendEnabled()) {
            log.info("[模拟发送] 邮箱验证码 to={} scene={} code={}（{} 分钟内有效）", to, sceneLabel, code, expireMinutes);
            return false;
        }
        try {
            JavaMailSender sender = Objects.requireNonNull(mailSenderProvider.getIfAvailable());
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject("墨迹验证码：" + sceneLabel);
            helper.setText(buildHtml(code, sceneLabel, expireMinutes), true);
            sender.send(message);
            log.info("验证码邮件已发送 to={} scene={}", to, sceneLabel);
            return true;
        } catch (Exception e) {
            log.error("验证码邮件发送失败 to={} scene={}: {}", to, sceneLabel, e.getMessage());
            return false;
        }
    }

    private String buildHtml(String code, String sceneLabel, int expireMinutes) {
        return """
                <div style="max-width:520px;margin:0 auto;padding:32px 28px;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;background:#f7f8fa;border-radius:12px;">
                  <h2 style="margin:0 0 8px;font-size:20px;color:#1f2329;">墨迹账号 %s</h2>
                  <p style="margin:0 0 20px;font-size:14px;color:#51565d;">你的验证码如下，%d 分钟内有效。请勿泄露给他人。</p>
                  <div style="display:inline-block;padding:14px 28px;font-size:28px;font-weight:700;letter-spacing:8px;color:#1f2329;background:#ffffff;border-radius:8px;border:1px solid #e5e6eb;">%s</div>
                  <p style="margin:24px 0 0;font-size:12px;color:#8a919f;">如果这不是你的操作，请忽略本邮件。</p>
                </div>
                """.formatted(sceneLabel, expireMinutes, code);
    }
}
