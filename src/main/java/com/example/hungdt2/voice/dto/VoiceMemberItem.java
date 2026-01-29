package com.example.hungdt2.voice.dto;

public class VoiceMemberItem {
    public Long userId;
    public String role;
    public Long lastSeen;
    public Boolean kicked;

    public VoiceMemberItem(Long userId, String role, Long lastSeen, Boolean kicked) {
        this.userId = userId;
        this.role = role;
        this.lastSeen = lastSeen;
        this.kicked = kicked;
    }
}