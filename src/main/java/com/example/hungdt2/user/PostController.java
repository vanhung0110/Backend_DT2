package com.example.hungdt2.user;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.user.dto.CreatePostRequest;
import com.example.hungdt2.user.dto.PostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            Authentication auth,
            @Valid @RequestBody CreatePostRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(postService.createPost(userId, req)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(postService.getUserPosts(userId, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(postService.getAllPosts(pageable)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(new ApiResponse<>("Post deleted"));
    }
}
