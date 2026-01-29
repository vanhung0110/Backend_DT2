package com.example.hungdt2.user.dto;

import java.time.LocalDateTime;

public record StatusResponse(
    Long id,
    Long userId,
    String username,
    String displayName,
    String profileImageUrl,
    String content,
    String imageUrl,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
