package com.example.hungdt2.friend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friend_messages")
public class FriendMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "friend_room_id", nullable = false)
    private Long friendRoomId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFriendRoomId() { return friendRoomId; }
    public void setFriendRoomId(Long friendRoomId) { this.friendRoomId = friendRoomId; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Instant getCreatedAt() { return createdAt; }
}