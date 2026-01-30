package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.CreateStatusRequest;
import com.example.hungdt2.user.dto.StatusResponse;
import com.example.hungdt2.user.entity.StatusEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.UserStatusRepository;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final UserStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    @Transactional
    public StatusResponse createStatus(Long userId, CreateStatusRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        StatusEntity status = new StatusEntity();
        status.setUserId(userId);
        status.setContent(req.content());
        status.setImageUrl(req.imageUrl());
        
        StatusEntity saved = statusRepository.save(status);
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        
        return new StatusResponse(
            saved.getId(),
            saved.getUserId(),
            user.getUsername(),
            user.getDisplayName(),
            profile != null ? profile.getProfileImageUrl() : null,
            saved.getContent(),
            saved.getImageUrl(),
            saved.getCreatedAt(),
            saved.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public Page<StatusResponse> getUserStatuses(Long userId, Pageable pageable) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        UserProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        String profileImage = profile != null ? profile.getProfileImageUrl() : null;
        
        return statusRepository.findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now(), pageable)
                .map(status -> new StatusResponse(
                    status.getId(),
                    status.getUserId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    profileImage,
                    status.getContent(),
                    status.getImageUrl(),
                    status.getCreatedAt(),
                    status.getExpiresAt()
                ));
    }

    @Transactional
    public void deleteStatus(Long statusId, Long userId) {
        StatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "Status not found"));
        
        if (!status.getUserId().equals(userId)) {
            throw new NotFoundException("FORBIDDEN", "Cannot delete other user's status");
        }
        
        statusRepository.delete(status);
    }

    @Transactional
    public void cleanupExpiredStatuses() {
        List<StatusEntity> expired = statusRepository.findByExpiresAtBefore(LocalDateTime.now());
        statusRepository.deleteAll(expired);
    }
}
