package com.example.hungdt2.auth.repository;

import com.example.hungdt2.auth.entity.PasswordResetOtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtpEntity, Long> {

    @Query("SELECT p FROM PasswordResetOtpEntity p WHERE p.phone = :phone AND p.used = false ORDER BY p.createdAt DESC")
    Optional<PasswordResetOtpEntity> findLatestByPhone(@Param("phone") String phone);

    Optional<PasswordResetOtpEntity> findByResetToken(String token);
}