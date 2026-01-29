package com.example.hungdt2.user.repository;

import com.example.hungdt2.user.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    Page<PostEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
