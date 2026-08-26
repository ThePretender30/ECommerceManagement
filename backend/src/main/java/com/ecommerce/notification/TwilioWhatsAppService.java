package com.ecommerce.notification;

import com.ecommerce.config.WhatsAppProperties;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class TwilioWhatsAppService implements WhatsAppService {

    private final WhatsAppProperties properties;

    @PostConstruct
    void init() {
        Twilio.init(properties.getAccountSid(), properties.getAuthToken());
        log.info("Twilio WhatsApp sender initialised (from={})", properties.normalisedFromNumber());
    }

    @Override
    public SendResult send(String toPhoneNumber, String message) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            return SendResult.skipped("Recipient has no phone number on file.");
        }

        String to = normaliseRecipient(toPhoneNumber);

        try {
            Message sent;
            if (properties.getContentSid() != null && !properties.getContentSid().isBlank()) {
                sent = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(properties.normalisedFromNumber()),
                        message
                ).setContentSid(properties.getContentSid()).create();
            } else {
                sent = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(properties.normalisedFromNumber()),
                        message
                ).create();
            }

            log.info("WhatsApp message {} sent to {}", sent.getSid(), maskPhone(to));
            return SendResult.sent(sent.getSid());

        } catch (ApiException ex) {
            String detail = "Twilio error %s: %s".formatted(ex.getCode(), ex.getMessage());
            log.warn("WhatsApp delivery to {} failed. {}", maskPhone(to), detail);
            return SendResult.failed(detail);

        } catch (Exception ex) {
            log.warn("WhatsApp delivery to {} failed unexpectedly: {}", maskPhone(to), ex.getMessage());
            return SendResult.failed(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "TWILIO";
    }

    private String normaliseRecipient(String phone) {
        String trimmed = phone.trim();
        return trimmed.startsWith("whatsapp:") ? trimmed : "whatsapp:" + trimmed;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
