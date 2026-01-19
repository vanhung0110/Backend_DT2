package com.example.hungdt2.voice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "voice_members", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class VoiceMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role")
    private String role = "member";

    @Column(name = "joined_at")
    private Instant joinedAt = Instant.now();

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "kicked")
    private Boolean kicked = false;

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

    public Boolean getKicked() { return kicked; }
    public void setKicked(Boolean kicked) { this.kicked = kicked; }
}