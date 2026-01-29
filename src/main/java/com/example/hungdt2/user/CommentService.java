package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.CommentResponse;
import com.example.hungdt2.user.dto.CreateCommentRequest;
import com.example.hungdt2.user.entity.CommentEntity;
import com.example.hungdt2.user.entity.PostEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.CommentRepository;
import com.example.hungdt2.user.repository.PostRepository;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
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
    private final UserProfileRepository profileRepository;

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CreateCommentRequest req) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(req.content());
        
        CommentEntity saved = commentRepository.save(comment);
        
        // Update post comment count
        post.setCommentCount((int) commentRepository.countByPostId(postId));
        postRepository.save(post);
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        
        return new CommentResponse(
            saved.getId(),
            saved.getPostId(),
            saved.getUserId(),
            user.getUsername(),
            user.getDisplayName(),
            profile != null ? profile.getProfileImageUrl() : null,
            saved.getContent(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getPostComments(Long postId, Pageable pageable) {
        postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable)
                .map(comment -> {
                    UserEntity user = userRepository.findById(comment.getUserId()).orElse(null);
                    UserProfileEntity profile = profileRepository.findByUserId(comment.getUserId()).orElse(null);
                    return new CommentResponse(
                        comment.getId(),
                        comment.getPostId(),
                        comment.getUserId(),
                        user != null ? user.getUsername() : "Unknown",
                        user != null ? user.getDisplayName() : "Unknown",
                        profile != null ? profile.getProfileImageUrl() : null,
                        comment.getContent(),
                        comment.getCreatedAt()
                    );
                });
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND", "Comment not found"));
        
        if (!comment.getUserId().equals(userId)) {
            throw new NotFoundException("FORBIDDEN", "Cannot delete other user's comment");
        }
        
        commentRepository.delete(comment);
        
        // Update post comment count
        PostEntity post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post != null) {
            post.setCommentCount((int) commentRepository.countByPostId(comment.getPostId()));
            postRepository.save(post);
        }
    }
}
