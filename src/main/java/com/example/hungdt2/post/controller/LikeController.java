package com.example.hungdt2.post.controller;

import com.example.hungdt2.post.dto.ApiResponse;
import com.example.hungdt2.post.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;
    
    @PostMapping("/{postId}/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable Long postId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        boolean isLiked = likeService.toggleLike(postId, userId);
        long likeCount = likeService.getLikeCount(postId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);
        
        return ResponseEntity.ok(new ApiResponse<>(response));
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable Long postId) {
        long count = likeService.getLikeCount(postId);
        return ResponseEntity.ok(new ApiResponse<>(count));
    }
    
    @GetMapping("/{postId}/user-liked")
    public ResponseEntity<ApiResponse<Boolean>> isLiked(
            @PathVariable Long postId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        boolean isLiked = likeService.isLiked(postId, userId);
        
        return ResponseEntity.ok(new ApiResponse<>(isLiked));
    }
}
