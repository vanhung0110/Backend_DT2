package com.example.hungdt2.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
