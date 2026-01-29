package com.example.hungdt2.user.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long postId,
    Long userId,
    String username,
    String displayName,
    String profileImageUrl,
    String content,
    LocalDateTime createdAt
) {}
