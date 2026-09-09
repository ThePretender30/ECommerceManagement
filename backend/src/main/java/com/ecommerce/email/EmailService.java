package com.ecommerce.email;

public interface EmailService {

    SendResult send(String toEmail, String subject, String bodyText);

    default SendResult send(String toEmail, String subject, String bodyText, String bodyHtml) {
        return send(toEmail, subject, bodyText);
    }

    String providerName();
}
