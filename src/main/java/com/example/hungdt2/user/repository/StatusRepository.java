package com.example.hungdt2.user.repository;

import com.example.hungdt2.user.entity.StatusEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatusRepository extends JpaRepository<StatusEntity, Long> {
    Page<StatusEntity> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now, Pageable pageable);
    List<StatusEntity> findByExpiresAtBefore(LocalDateTime now);
}
