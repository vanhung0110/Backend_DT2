package com.example.hungdt2.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRoomRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank String type, // PUBLIC | PRIVATE
        List<Long> members,
        Boolean requireApproval
) {}
