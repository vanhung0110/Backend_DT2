package com.example.hungdt2.rt.repository;

import com.example.hungdt2.rt.entity.RtRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RtRoomRepository extends JpaRepository<RtRoomEntity, Long> {
    List<RtRoomEntity> findByIsPublicTrueAndActiveTrue();

    // Return public rooms regardless of active state, ordered by last activity desc for UX
    List<RtRoomEntity> findByIsPublicTrueOrderByLastActivityAtDesc();
}
