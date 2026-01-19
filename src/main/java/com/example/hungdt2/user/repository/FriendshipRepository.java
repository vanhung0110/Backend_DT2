package com.example.hungdt2.user.repository;

import com.example.hungdt2.user.entity.FriendshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {
    Optional<FriendshipEntity> findByUserIdAndFriendId(Long userId, Long friendId);
    org.springframework.data.domain.Page<FriendshipEntity> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
    java.util.List<FriendshipEntity> findByUserId(Long userId);
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}
