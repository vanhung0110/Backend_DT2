package com.example.hungdt2.user.repository;

import com.example.hungdt2.user.entity.FriendRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {
    Optional<FriendRequestEntity> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);
    Page<FriendRequestEntity> findByRecipientIdAndStatus(Long recipientId, String status, Pageable pageable);
    Page<FriendRequestEntity> findByRequesterIdAndStatus(Long requesterId, String status, Pageable pageable);
}
