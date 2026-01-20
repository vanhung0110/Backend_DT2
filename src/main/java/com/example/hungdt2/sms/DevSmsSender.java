package com.example.hungdt2.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DevSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(DevSmsSender.class);

    // Keep a simple in-memory last message map for tests/dev inspection
    private static final java.util.concurrent.ConcurrentHashMap<String, String> lastMessages = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void sendSms(String phone, String message) {
        // In dev, just log the OTP instead of actually sending SMS
        log.info("[DevSmsSender] SMS to {}: {}", phone, message);
        lastMessages.put(phone, message);
    }

    // Test helper: return last message sent to phone
    public static String getLastMessageForPhone(String phone) {
        return lastMessages.get(phone);
    }
}