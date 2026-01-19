package com.example.hungdt2.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record ApiError(String code, String message, Map<String, Object> details) { }
