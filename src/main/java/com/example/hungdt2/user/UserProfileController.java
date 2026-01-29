package com.example.hungdt2.user;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.user.dto.UpdateProfileRequest;
import com.example.hungdt2.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(userProfileService.getProfile(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(userProfileService.getProfile(userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(userProfileService.updateProfile(userId, req)));
    }
}
