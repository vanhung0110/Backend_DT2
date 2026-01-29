package com.example.hungdt2.post.service;

import com.example.hungdt2.post.entity.LikeEntity;
import com.example.hungdt2.post.entity.PostEntity;
import com.example.hungdt2.post.repository.LikeRepository;
import com.example.hungdt2.post.repository.PostRepository;
import com.example.hungdt2.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {
    
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
        
        var existingLike = likeRepository.findByPostIdAndUserId(postId, userId);
        
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            LikeEntity like = new LikeEntity();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            return true;
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, Long userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }
    
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }
}
