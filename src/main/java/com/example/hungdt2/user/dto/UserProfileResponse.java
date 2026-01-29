package com.example.hungdt2.user.dto;

public record UserProfileResponse(
    Long userId,
    String username,
    String displayName,
    String email,
    String phone,
    String bio,
    String profileImageUrl,
    String coverImageUrl,
    String location,
    String website
) {}
