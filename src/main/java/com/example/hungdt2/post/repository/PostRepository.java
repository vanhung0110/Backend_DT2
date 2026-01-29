package com.example.hungdt2.post.repository;

import com.example.hungdt2.post.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    Page<PostEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
