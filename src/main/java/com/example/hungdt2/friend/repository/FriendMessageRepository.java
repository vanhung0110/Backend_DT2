package com.example.hungdt2.friend.repository;

import com.example.hungdt2.friend.entity.FriendMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendMessageRepository extends JpaRepository<FriendMessageEntity, Long> {
    List<FriendMessageEntity> findByFriendRoomIdOrderByIdAsc(Long friendRoomId);
}