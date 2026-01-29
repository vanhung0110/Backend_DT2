package com.example.hungdt2.post.repository;

import com.example.hungdt2.post.entity.StatusEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StatusRepository extends JpaRepository<StatusEntity, Long> {
    Page<StatusEntity> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now, Pageable pageable);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
