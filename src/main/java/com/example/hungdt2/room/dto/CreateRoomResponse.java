package com.example.hungdt2.room.dto;

public record CreateRoomResponse(Long id, String code, String name, String type, Long ownerId, Boolean voiceEnabled) {}
