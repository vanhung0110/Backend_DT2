package com.example.hungdt2.user.repository;

import com.example.hungdt2.user.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("userCommentRepository")
public interface UserCommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    long countByPostId(Long postId);
}
