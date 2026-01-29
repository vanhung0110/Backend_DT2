package com.example.hungdt2.friend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friend_rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"user_a", "user_b"}))
public class FriendRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_a", nullable = false)
    private Long userA;

    @Column(name = "user_b", nullable = false)
    private Long userB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getUserA() { return userA; }
    public void setUserA(Long userA) { this.userA = userA; }

    public Long getUserB() { return userB; }
    public void setUserB(Long userB) { this.userB = userB; }

    public Instant getCreatedAt() { return createdAt; }
}