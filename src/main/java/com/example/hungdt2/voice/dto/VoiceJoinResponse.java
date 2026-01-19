package com.example.hungdt2.voice.dto;

public class VoiceJoinResponse {
    public String token;
    public String channel;
    public Long expiresAt;
    public String appId; // Agora appId (nullable)

    public VoiceJoinResponse(String token, String channel, Long expiresAt) {
        this.token = token;
        this.channel = channel;
        this.expiresAt = expiresAt;
    }

    public VoiceJoinResponse(String token, String channel, Long expiresAt, String appId) {
        this.token = token;
        this.channel = channel;
        this.expiresAt = expiresAt;
        this.appId = appId;
    }
}