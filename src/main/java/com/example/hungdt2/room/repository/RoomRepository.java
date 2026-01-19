package com.example.hungdt2.room.repository;

import com.example.hungdt2.room.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    Optional<RoomEntity> findByCode(String code);

    @Query("SELECT r FROM RoomEntity r WHERE r.type = 'PRIVATE' AND :a IN (SELECT m.userId FROM com.example.hungdt2.room.entity.RoomMemberEntity m WHERE m.roomId = r.id) AND :b IN (SELECT m2.userId FROM com.example.hungdt2.room.entity.RoomMemberEntity m2 WHERE m2.roomId = r.id) AND (SELECT COUNT(m3) FROM com.example.hungdt2.room.entity.RoomMemberEntity m3 WHERE m3.roomId = r.id) = 2")
    Optional<RoomEntity> findDirectRoomBetweenUsers(@Param("a") Long a, @Param("b") Long b);
}
