package com.example.hungdt2.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String resetToken, @NotBlank String password) { }
