package com.example.hungdt2.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AgoraTokenService {

    @Value("${voice.agora.appId:}")
    private String appId;

    @Value("${voice.agora.appCertificate:}")
    private String appCertificate;

    public String generateToken(Long roomId, Long userId, int ttlSeconds) {
        // If real Agora credentials provided, attempt to generate a real RTC token using the Agora access-token library.
        if (appId != null && !appId.isBlank() && appCertificate != null && !appCertificate.isBlank()) {
            try {
                String channel = "room-" + roomId;
                String uid = String.valueOf(userId);
                int now = (int) Instant.now().getEpochSecond();
                int expireTs = now + ttlSeconds;
                // Use our local RtcTokenBuilder implementation to generate a token per Agora spec.
                String token = com.example.hungdt2.voice.agora.RtcTokenBuilder.buildTokenWithUid(appId, appCertificate, channel, uid, com.example.hungdt2.voice.agora.RtcTokenBuilder.Role.ROLE_PUBLISHER, expireTs);
                if (token != null) return token;
            } catch (Exception ex) {
                System.err.println("Agora token generation error: " + ex.getMessage());
            }

            // Final fallback: an encoded token that includes appId + expiry so the client can detect a live-ish token.
            String payload = String.format("%s:%s:%d:%d:%d", appId, roomId, userId, Instant.now().getEpochSecond(), ttlSeconds);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString((payload + ":" + UUID.randomUUID()).getBytes());
            return "agora-fallback:" + encoded;
        }

        // No credentials provided — return simple UUID-based token for testing
        return "stub-" + UUID.randomUUID();
    }

    public String getAppId() { return appId; }
}