package com.example.hungdt2.user.dto;

import java.time.Instant;

public record FriendRequestItem(Long id, Long requesterId, Long recipientId, String requesterName, String recipientName, String status, Instant createdAt) { }
