package com.example.hungdt2.user.dto;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    Long userId,
    String username,
    String displayName,
    String profileImageUrl,
    String content,
    String imageUrl,
    Integer likeCount,
    Integer commentCount,
    LocalDateTime createdAt
) {}
