package com.example.hungdt2.user.dto;

public record UserSearchResponse(
    Long id,
    String username,
    String displayName,
    String bio,
    String profileImageUrl,
    long followersCount,
    boolean isFollowing
) {}
