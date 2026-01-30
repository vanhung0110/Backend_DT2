package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.CreatePostRequest;
import com.example.hungdt2.user.dto.PostResponse;
import com.example.hungdt2.user.entity.PostEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.UserPostRepository;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final UserPostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        PostEntity post = new PostEntity();
        post.setUserId(userId);
        post.setContent(req.content());
        post.setImageUrl(req.imageUrl());
        
        PostEntity saved = postRepository.save(post);
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        
        return new PostResponse(
            saved.getId(),
            saved.getUserId(),
            user.getUsername(),
            user.getDisplayName(),
            profile != null ? profile.getProfileImageUrl() : null,
            saved.getContent(),
            saved.getImageUrl(),
            saved.getLikeCount(),
            saved.getCommentCount(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        String profileImage = profile != null ? profile.getProfileImageUrl() : null;
        
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(post -> new PostResponse(
                    post.getId(),
                    post.getUserId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    profileImage,
                    post.getContent(),
                    post.getImageUrl(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> {
                    UserEntity user = userRepository.findById(post.getUserId()).orElse(null);
                    UserProfileEntity profile = profileRepository.findByUserId(post.getUserId()).orElse(null);
                    return new PostResponse(
                        post.getId(),
                        post.getUserId(),
                        user != null ? user.getUsername() : "Unknown",
                        user != null ? user.getDisplayName() : "Unknown",
                        profile != null ? profile.getProfileImageUrl() : null,
                        post.getContent(),
                        post.getImageUrl(),
                        post.getLikeCount(),
                        post.getCommentCount(),
                        post.getCreatedAt()
                    );
                });
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        if (!post.getUserId().equals(userId)) {
            throw new NotFoundException("FORBIDDEN", "Cannot delete other user's post");
        }
        
        postRepository.delete(post);
    }
}
