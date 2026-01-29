package com.example.hungdt2.post.service;

import com.example.hungdt2.post.dto.CommentResponse;
import com.example.hungdt2.post.dto.CreateCommentRequest;
import com.example.hungdt2.post.entity.CommentEntity;
import com.example.hungdt2.post.entity.PostEntity;
import com.example.hungdt2.post.repository.CommentRepository;
import com.example.hungdt2.post.repository.PostRepository;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
import com.example.hungdt2.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    
    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.content());
        
        commentRepository.save(comment);
        
        // Update post comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
            .orElse(new UserProfileEntity());
        
        return new CommentResponse(
            comment.getId(),
            userId,
            postId,
            comment.getContent(),
            user.getUsername(),
            user.getDisplayName(),
            profile.getProfileImageUrl(),
            comment.getCreatedAt()
        );
    }
    
    @Transactional(readOnly = true)
    public Page<CommentResponse> getPostComments(Long postId, Pageable pageable) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        Page<CommentEntity> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
        
        return comments.map(comment -> {
            UserEntity user = userRepository.findById(comment.getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            
            UserProfileEntity profile = userProfileRepository.findByUserId(comment.getUserId())
                .orElse(new UserProfileEntity());
            
            return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getPostId(),
                comment.getContent(),
                user.getUsername(),
                user.getDisplayName(),
                profile.getProfileImageUrl(),
                comment.getCreatedAt()
            );
        });
    }
    
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND", "Comment not found"));
        
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }
        
        PostEntity post = postRepository.findById(comment.getPostId())
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        commentRepository.delete(comment);
        
        // Update post comment count
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }
}
