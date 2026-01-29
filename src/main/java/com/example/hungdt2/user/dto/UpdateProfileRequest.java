package com.example.hungdt2.user.dto;

public record UpdateProfileRequest(
    String displayName,
    String bio,
    String location,
    String website,
    String profileImageUrl,
    String coverImageUrl
) {}
