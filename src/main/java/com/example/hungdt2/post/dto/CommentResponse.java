package com.example.hungdt2.post.dto;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long userId,
    Long postId,
    String content,
    String username,
    String userDisplayName,
    String userProfileImage,
    LocalDateTime createdAt
) {}
