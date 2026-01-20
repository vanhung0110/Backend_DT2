package com.example.hungdt2.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestOtpRequest(@NotBlank String phone) { }
