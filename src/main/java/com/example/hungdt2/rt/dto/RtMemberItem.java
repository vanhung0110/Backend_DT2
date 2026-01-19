package com.example.hungdt2.rt.dto;

public class RtMemberItem {
    public Long userId;
    public String role;
    public Long joinedAt;
    public Boolean muted;
    public Integer volume;
    public Boolean kicked;
    public Boolean online;

    public RtMemberItem(Long userId, String role, Long joinedAt, Boolean muted, Integer volume, Boolean kicked, Boolean online) {
        this.userId = userId; this.role = role; this.joinedAt = joinedAt; this.muted = muted; this.volume = volume; this.kicked = kicked; this.online = online;
    }

    public Long getUserId() { return this.userId; }
    public String getRole() { return this.role; }
    public Long getJoinedAt() { return this.joinedAt; }
    public Boolean getMuted() { return this.muted; }
    public Integer getVolume() { return this.volume; }
    public Boolean getKicked() { return this.kicked; }
    public Boolean getOnline() { return this.online; }
}
