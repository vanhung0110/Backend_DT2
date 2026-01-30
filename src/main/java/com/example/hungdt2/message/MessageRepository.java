package com.example.hungdt2.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // Fetch messages for room, optionally before a timestamp, newest first limited
    // by pageable
    @Query("SELECT m FROM com.example.hungdt2.message.MessageEntity m WHERE m.roomId = :roomId AND (:before IS NULL OR m.createdAt < :before) ORDER BY m.createdAt DESC")
    List<MessageEntity> findMessages(@Param("roomId") Long roomId, @Param("before") Instant before, Pageable pageable);

    // Fetch messages after a timestamp (ascending order) to efficiently get new
    // messages
    @Query("SELECT m FROM com.example.hungdt2.message.MessageEntity m WHERE m.roomId = :roomId AND (:after IS NULL OR m.createdAt > :after) ORDER BY m.createdAt ASC")
    List<MessageEntity> findMessagesAfter(@Param("roomId") Long roomId, @Param("after") Instant after,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE com.example.hungdt2.message.MessageEntity m SET m.isRead = true WHERE m.roomId = :roomId AND m.senderId != :userId AND m.isRead = false")
    void markRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
