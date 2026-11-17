package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:DangCap <noreply@dangcap.local>}")
    private String mailFrom;

    @Value("${app.mail.frontend-reset-url:http://localhost/reset-password}")
    private String frontendResetUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        String resetLink = buildResetLink(resetToken);

        log.info("[PASSWORD RESET] Queued password reset email for recipient '{}' (validity: 15 minutes)", maskEmail(toEmail));

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(mailUsername)) {
            log.warn("[EMAIL SERVICE] SMTP is not fully configured (spring.mail.username is empty). " +
                    "Password reset email could not be dispatched via SMTP.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(mailFrom);
            helper.setTo(toEmail);
            helper.setSubject("[DangCap] Yêu cầu đặt lại mật khẩu tài khoản");

            String htmlBody = buildHtmlTemplate(fullName, toEmail, resetLink, resetToken);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("[EMAIL SERVICE] Successfully dispatched password reset email via SMTP to {}", maskEmail(toEmail));
        } catch (Exception ex) {
            log.error("[EMAIL SERVICE] Failed to send email to {} via SMTP: {}", maskEmail(toEmail), ex.getMessage(), ex);
        }
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    private String buildResetLink(String resetToken) {
        String base = StringUtils.hasText(frontendResetUrl) ? frontendResetUrl.trim() : "http://localhost/reset-password";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "?token=" + resetToken;
    }

    private String buildHtmlTemplate(String fullName, String toEmail, String resetLink, String resetToken) {
        String safeName = HtmlUtils.htmlEscape(StringUtils.hasText(fullName) ? fullName : toEmail);
        String safeEmail = HtmlUtils.htmlEscape(toEmail);
        String safeLink = HtmlUtils.htmlEscape(resetLink);
        String safeToken = HtmlUtils.htmlEscape(resetToken);

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>Đặt lại mật khẩu - DangCap</title>
            </head>
            <body style="margin: 0; padding: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #1e293b;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width: 560px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                <tr>
                  <td style="padding: 32px 32px 24px 32px; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); text-align: center;">
                    <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;">DangCap</h1>
                    <p style="margin: 4px 0 0 0; font-size: 13px; color: #d1fae5;">Hệ thống Quản lý Tài chính Cá nhân</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 32px;">
                    <h2 style="margin: 0 0 16px 0; font-size: 18px; font-weight: 700; color: #0f172a;">Yêu cầu đặt lại mật khẩu</h2>
                    <p style="margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #334155;">
                      Xin chào <strong>%s</strong>,
                    </p>
                    <p style="margin: 0 0 20px 0; font-size: 14px; line-height: 1.6; color: #334155;">
                      Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản DangCap liên kết với email <strong>%s</strong>. Vui lòng bấm vào nút bên dưới để tiến hành tạo mật khẩu mới:
                    </p>
                    <div style="text-align: center; margin: 28px 0;">
                      <a href="%s" style="display: inline-block; background-color: #059669; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 10px; font-size: 15px; font-weight: 700; box-shadow: 0 2px 4px rgba(5, 150, 105, 0.3);">
                        Đặt lại mật khẩu
                      </a>
                    </div>
                    <p style="margin: 24px 0 8px 0; font-size: 13px; color: #64748b;">
                      Hoặc bạn có thể sao chép mã xác thực bên dưới và dán trực tiếp vào trang Đặt lại mật khẩu:
                    </p>
                    <div style="background-color: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 12px 16px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 15px; font-weight: bold; color: #0f172a; word-break: break-all; text-align: center;">
                      %s
                    </div>
                    <div style="margin-top: 24px; padding: 14px 16px; background-color: #fef3c7; border-left: 4px solid #f59e0b; border-radius: 6px;">
                      <p style="margin: 0; font-size: 12px; line-height: 1.5; color: #92400e;">
                        ⚠️ <strong>Lưu ý:</strong> Liên kết và mã xác thực có hiệu lực trong <strong>15 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai. Nếu bạn không gửi yêu cầu này, hãy yên tâm bỏ qua email này.
                      </p>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 20px 32px; background-color: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center;">
                    <p style="margin: 0; font-size: 12px; color: #94a3b8;">
                      © 2026 DangCap Personal Finance Manager. All rights reserved.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, safeEmail, safeLink, safeToken);
    }
}
