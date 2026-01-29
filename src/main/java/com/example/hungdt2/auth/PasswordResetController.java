package com.example.hungdt2.auth;

import com.example.hungdt2.auth.dto.RequestOtpRequest;
import com.example.hungdt2.auth.dto.ResetPasswordRequest;
import com.example.hungdt2.auth.dto.VerifyOtpRequest;
import com.example.hungdt2.auth.dto.VerifyOtpResponse;
import com.example.hungdt2.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/forgot")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<String>> requestOtp(@Valid @RequestBody RequestOtpRequest req) {
        service.requestOtp(req.phone());
        return ResponseEntity.ok(new ApiResponse<>("OK"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        String token = service.verifyOtp(req.phone(), req.otp());
        return ResponseEntity.ok(new ApiResponse<>(new VerifyOtpResponse(token)));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(req.resetToken(), req.password());
        return ResponseEntity.ok(new ApiResponse<>("OK"));
    }
}