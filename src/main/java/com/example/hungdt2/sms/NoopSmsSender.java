package com.example.hungdt2.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default SMS sender when no real provider is configured.
 * Intentionally a NO-OP for security: does not leak OTPs to logs or tests.
 */
@Component
public class NoopSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(NoopSmsSender.class);

    @Override
    public void sendSms(String phone, String message) {
        // Do not log OTPs in production
        log.warn("No SMS provider configured: not sending SMS to {}", phone);
    }
}
