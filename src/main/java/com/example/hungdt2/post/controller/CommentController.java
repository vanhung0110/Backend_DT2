package com.example.hungdt2.post.controller;

import com.example.hungdt2.post.dto.ApiResponse;
import com.example.hungdt2.post.dto.CommentResponse;
import com.example.hungdt2.post.dto.CreateCommentRequest;
import com.example.hungdt2.post.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        CommentResponse response = commentService.createComment(postId, userId, request);
        
        return ResponseEntity.ok(new ApiResponse<>(response));
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentResponse> comments = commentService.getPostComments(postId, pageable);
        
        return ResponseEntity.ok(new ApiResponse<>(comments));
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        commentService.deleteComment(commentId, userId);
        
        return ResponseEntity.ok(new ApiResponse<>(null));
    }
}
