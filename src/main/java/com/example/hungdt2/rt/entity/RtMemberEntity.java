package com.example.hungdt2.rt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rt_members")
public class RtMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id")
    private Long userId;

    private String role = "member";

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_seen")
    private Instant lastSeen;

    private Boolean muted = false;

    private Integer volume = 100;

    private Boolean kicked = false;

    private Boolean online = false;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { if (joinedAt == null) joinedAt = Instant.now(); if (online == null) online = false; if (updatedAt == null) updatedAt = joinedAt; }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getMuted() { return muted; }
    public void setMuted(Boolean muted) { this.muted = muted; }
    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }
    public Boolean getKicked() { return kicked; }
    public void setKicked(Boolean kicked) { this.kicked = kicked; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
}
