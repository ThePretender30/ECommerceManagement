package com.ecommerce.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;

    @Override
    public SendResult send(String toEmail, String subject, String bodyText) {
        return send(toEmail, subject, bodyText, null);
    }

    @Override
    public SendResult send(String toEmail, String subject, String bodyText, String bodyHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            if (bodyHtml != null && !bodyHtml.isBlank()) {
                helper.setText(bodyText != null ? bodyText : bodyHtml, bodyHtml);
            } else {
                helper.setText(bodyText, false);
            }

            mailSender.send(message);
            String messageId = "SMTP-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Sent email via SMTP to {} (messageId={})", toEmail, messageId);
            return SendResult.sent(messageId);
        } catch (Exception e) {
            log.error("Failed to send email via SMTP to {}: {}", toEmail, e.getMessage(), e);
            return SendResult.failed(e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "SMTP";
    }
}
