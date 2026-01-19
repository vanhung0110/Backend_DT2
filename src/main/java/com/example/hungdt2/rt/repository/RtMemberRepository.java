package com.example.hungdt2.rt.repository;

import com.example.hungdt2.rt.entity.RtMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RtMemberRepository extends JpaRepository<RtMemberEntity, Long> {
    List<RtMemberEntity> findByRoomId(Long roomId);
    Optional<RtMemberEntity> findByRoomIdAndUserId(Long roomId, Long userId);
    List<RtMemberEntity> findByUserId(Long userId);
}
