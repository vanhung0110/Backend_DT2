package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.UserSearchResponse;
import com.example.hungdt2.user.entity.FollowEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.FollowRepository;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
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
    private final UserProfileRepository profileRepository;

    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new NotFoundException("INVALID", "Cannot follow yourself");
        }
        
        userRepository.findById(followingId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        boolean following = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        
        if (following) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
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
    public Page<UserSearchResponse> getFollowers(Long userId, Long currentUserId, Pageable pageable) {
        return followRepository.findByFollowingId(userId, pageable)
                .map(follow -> buildUserResponse(follow.getFollowerId(), currentUserId));
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> getFollowing(Long userId, Long currentUserId, Pageable pageable) {
        return followRepository.findByFollowerId(userId, pageable)
                .map(follow -> buildUserResponse(follow.getFollowingId(), currentUserId));
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(String query, Long currentUserId, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query, pageable)
                .map(user -> buildUserResponse(user.getId(), currentUserId));
    }

    private UserSearchResponse buildUserResponse(Long userId, Long currentUserId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        long followers = followRepository.countByFollowingId(userId);
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
        
        return new UserSearchResponse(
            userId,
            user.getUsername(),
            user.getDisplayName(),
            profile != null ? profile.getBio() : null,
            profile != null ? profile.getProfileImageUrl() : null,
            followers,
            isFollowing
        );
    }
}
