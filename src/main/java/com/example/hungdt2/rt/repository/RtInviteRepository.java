package com.example.hungdt2.rt.repository;

import com.example.hungdt2.rt.entity.RtInviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RtInviteRepository extends JpaRepository<RtInviteEntity, Long> {
    List<RtInviteEntity> findByRoomId(Long roomId);
    Optional<RtInviteEntity> findByRoomIdAndInvitedUserId(Long roomId, Long invitedUserId);
    List<RtInviteEntity> findByInvitedUserId(Long invitedUserId);
}
