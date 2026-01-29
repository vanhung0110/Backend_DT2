package com.example.hungdt2.message.dto;

import java.time.Instant;

public record MessageItem(Long id, Long roomId, Long senderId, String content, String type, String audioUrl, Long audioDurationMs, Instant createdAt) {}
