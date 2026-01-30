package com.example.hungdt2.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "UserStatus")
@Table(name = "statuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 500)
    private String imageUrl;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime expiresAt; // 24h auto delete
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusHours(24);
    }
}
