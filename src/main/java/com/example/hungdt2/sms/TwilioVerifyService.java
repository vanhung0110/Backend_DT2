package com.example.hungdt2.sms;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class TwilioVerifyService {
    private final Logger log = LoggerFactory.getLogger(TwilioVerifyService.class);

    @Value("${SMS_TWILIO_ACCOUNT_SID:}")
    private String accountSid;

    @Value("${SMS_TWILIO_AUTH_TOKEN:}")
    private String authToken;

    @Value("${SMS_TWILIO_VERIFY_SERVICE_SID:}")
    private String serviceSid;

    @PostConstruct
    public void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank() || serviceSid == null || serviceSid.isBlank()) {
            log.warn("TwilioVerifyService configured but missing credentials or service SID; service will not function until env vars are set");
        }
        try {
            Twilio.init(accountSid, authToken);
        } catch (Exception e) {
            log.warn("Twilio init failed: {}", e.getMessage());
        }
    }

    public void sendVerification(String phoneE164) {
        try {
            Verification.creator(serviceSid, phoneE164, "sms").create();
            log.info("Twilio Verify: sent verification to {}", phoneE164);
        } catch (ApiException e) {
            log.error("Twilio Verify send failed: {}", e.getMessage());
            throw e;
        }
    }

    public boolean checkVerification(String phoneE164, String code) {
        try {
            VerificationCheck check = VerificationCheck.creator(serviceSid).setTo(phoneE164).setCode(code).create();
            log.info("Twilio Verify: check status for {} -> {}", phoneE164, check.getStatus());
            return "approved".equalsIgnoreCase(check.getStatus());
        } catch (ApiException e) {
            log.warn("Twilio Verify check failed: {}", e.getMessage());
            return false;
        }
    }
}