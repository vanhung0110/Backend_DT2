package com.example.hungdt2.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard success response wrapper
 */
public record ApiResponse<T>(T data) {
}
