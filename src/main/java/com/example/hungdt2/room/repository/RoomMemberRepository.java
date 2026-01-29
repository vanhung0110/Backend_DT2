package com.example.hungdt2.room.repository;

import com.example.hungdt2.room.entity.RoomMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, Long> {
    Optional<RoomMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);
    List<RoomMemberEntity> findByUserId(Long userId);
    List<RoomMemberEntity> findByRoomIdAndStatus(Long roomId, String status);
}
