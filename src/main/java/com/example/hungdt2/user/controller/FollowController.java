package com.example.hungdt2.user.controller;

import com.example.hungdt2.post.dto.ApiResponse;
import com.example.hungdt2.user.dto.UserSearchResponse;
import com.example.hungdt2.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {
    
    private final FollowService followService;
    @PostMapping("/{userId}/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(
            @PathVariable Long userId,
            Authentication authentication) {
        
        Long followerId = Long.parseLong(authentication.getName());
        boolean isFollowing = followService.toggleFollow(followerId, userId);
        long followersCount = followService.getFollowersCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isFollowing", isFollowing);
        response.put("followersCount", followersCount);
        
        return ResponseEntity.ok(new ApiResponse<>(response));
    }
    
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<Page<UserSearchResponse>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSearchResponse> followers = followService.getFollowers(userId, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(followers));
    }
    
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<Page<UserSearchResponse>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSearchResponse> following = followService.getFollowing(userId, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(following));
    }
    
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<UserSearchResponse>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        Long currentUserId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSearchResponse> results = followService.searchUsers(q, currentUserId, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(results));
    }
}
