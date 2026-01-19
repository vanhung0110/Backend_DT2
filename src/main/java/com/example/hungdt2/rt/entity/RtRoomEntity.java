package com.example.hungdt2.rt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rt_rooms")
public class RtRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "owner_id")
    private Long ownerId;

    private String description;

    @Column(name = "max_members")
    private Integer maxMembers = 50;

    private Boolean active = true;

    // sleeping indicates the room is temporarily paused due to inactivity
    private Boolean sleeping = false;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = Instant.now(); if (lastActivityAt == null) lastActivityAt = Instant.now(); if (sleeping == null) sleeping = false; }

    // getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getSleeping() { return sleeping; }
    public void setSleeping(Boolean sleeping) { this.sleeping = sleeping; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
