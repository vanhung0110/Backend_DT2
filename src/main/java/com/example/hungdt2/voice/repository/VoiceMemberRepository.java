package com.example.hungdt2.voice.repository;

import com.example.hungdt2.voice.entity.VoiceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceMemberRepository extends JpaRepository<VoiceMemberEntity, Long> {
    Optional<VoiceMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);
    List<VoiceMemberEntity> findByRoomId(Long roomId);
}
