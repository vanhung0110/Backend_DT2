package com.example.hungdt2.auth;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.LoginResponse;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.auth.dto.RegisterResponse;
import com.example.hungdt2.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResponse data = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse data = authService.login(req);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }
}
