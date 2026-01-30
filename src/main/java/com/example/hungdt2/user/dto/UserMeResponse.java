package com.example.hungdt2.user.dto;

public record UserMeResponse(Long id, String username, String email, String phone, String displayName, String avatarUrl,
        Boolean isActive) {
}
