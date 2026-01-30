package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.entity.LikeEntity;
import com.example.hungdt2.user.entity.PostEntity;
import com.example.hungdt2.user.repository.UserLikeRepository;
import com.example.hungdt2.user.repository.UserPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userLikeService")
@RequiredArgsConstructor
public class UserLikeService {

    private final UserLikeRepository likeRepository;
    private final UserPostRepository postRepository;

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));

        boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);

        if (liked) {
            likeRepository.deleteByPostIdAndUserId(postId, userId);
        } else {
            LikeEntity like = new LikeEntity();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepository.save(like);
        }

        // Update post like count
        long likeCount = likeRepository.countByPostId(postId);
        post.setLikeCount((int) likeCount);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, Long userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId);
    }
}
