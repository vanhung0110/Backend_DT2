package com.example.hungdt2.friend.repository;

import com.example.hungdt2.friend.entity.FriendRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface FriendRoomRepository extends JpaRepository<FriendRoomEntity, Long> {
    @Query("SELECT fr FROM FriendRoomEntity fr WHERE (fr.userA = :a AND fr.userB = :b) OR (fr.userA = :b AND fr.userB = :a)")
    Optional<FriendRoomEntity> findBetweenUsers(@Param("a") Long a, @Param("b") Long b);

    List<FriendRoomEntity> findByUserAOrUserB(Long a, Long b);
}