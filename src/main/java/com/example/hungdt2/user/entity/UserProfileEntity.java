package com.example.hungdt2.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity {
    
    @Id
    private Long userId;
    
    @Column(length = 500)
    private String bio;
    
    @Column(length = 255)
    private String profileImageUrl;
    
    @Column(length = 255)
    private String coverImageUrl;
    
    @Column(length = 255)
    private String location;
    
    @Column(length = 255)
    private String website;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
