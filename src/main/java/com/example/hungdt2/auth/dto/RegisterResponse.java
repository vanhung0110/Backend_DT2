package com.example.hungdt2.auth.dto;

public record RegisterResponse(Long id, String username, String email, String phone, String displayName, Boolean isActive) {}
