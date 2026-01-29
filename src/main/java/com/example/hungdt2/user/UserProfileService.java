package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.UpdateProfileRequest;
import com.example.hungdt2.user.dto.UserProfileResponse;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.entity.UserProfileEntity;
import com.example.hungdt2.user.repository.UserProfileRepository;
import com.example.hungdt2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        UserProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        
        return new UserProfileResponse(
            userId,
            user.getUsername(),
            user.getDisplayName(),
            user.getEmail(),
            user.getPhone(),
            profile.getBio(),
            profile.getProfileImageUrl(),
            profile.getCoverImageUrl(),
            profile.getLocation(),
            profile.getWebsite()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        
        UserProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        
        // Update user info
        if (req.displayName() != null) user.setDisplayName(req.displayName());
        userRepository.save(user);
        
        // Update profile
        if (req.bio() != null) profile.setBio(req.bio());
        if (req.location() != null) profile.setLocation(req.location());
        if (req.website() != null) profile.setWebsite(req.website());
        if (req.profileImageUrl() != null) profile.setProfileImageUrl(req.profileImageUrl());
        if (req.coverImageUrl() != null) profile.setCoverImageUrl(req.coverImageUrl());
        
        profileRepository.save(profile);
        
        return getProfile(userId);
    }

    private UserProfileEntity createDefaultProfile(Long userId) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        return profileRepository.save(profile);
    }
}
