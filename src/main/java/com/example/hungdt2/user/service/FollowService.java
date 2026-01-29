package com.example.hungdt2.user.service;

import com.example.hungdt2.user.dto.UserSearchResponse;
import com.example.hungdt2.user.entity.FollowEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.FollowRepository;
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
public class FollowService {
    
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    
    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself");
        }
        
        UserEntity follower = userRepository.findById(followerId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Follower not found"));
        
        UserEntity following = userRepository.findById(followingId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User to follow not found"));
        
        var existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        
        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return false;
        } else {
            FollowEntity follow = new FollowEntity();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            followRepository.save(follow);
            return true;
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
    
    @Transactional(readOnly = true)
    public long getFollowersCount(Long userId) {
        return followRepository.countByFollowingId(userId);
    }
    
    @Transactional(readOnly = true)
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }
    
    @Transactional(readOnly = true)
    public Page<UserSearchResponse> getFollowers(Long userId, Pageable pageable) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        Page<FollowEntity> follows = followRepository.findByFollowingId(userId, pageable);
        
        return follows.map(follow -> buildUserResponse(follow.getFollowerId(), userId));
    }
    
    @Transactional(readOnly = true)
    public Page<UserSearchResponse> getFollowing(Long userId, Pageable pageable) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        Page<FollowEntity> follows = followRepository.findByFollowerId(userId, pageable);
        
        return follows.map(follow -> buildUserResponse(follow.getFollowingId(), userId));
    }
    
    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(String query, Long currentUserId, Pageable pageable) {
        Page<UserEntity> users = userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query, pageable);
        
        return users.map(user -> {
            UserProfileEntity profile = userProfileRepository.findByUserId(user.getId())
                .orElse(new UserProfileEntity());
            
            boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());
            long followersCount = followRepository.countByFollowingId(user.getId());
            
            return new UserSearchResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                profile.getBio(),
                profile.getProfileImageUrl(),
                followersCount,
                isFollowing
            );
        });
    }
    
    private UserSearchResponse buildUserResponse(Long userId, Long currentUserId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
            .orElse(new UserProfileEntity());
        
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
        long followersCount = followRepository.countByFollowingId(userId);
        
        return new UserSearchResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            profile.getBio(),
            profile.getProfileImageUrl(),
            followersCount,
            isFollowing
        );
    }
}
