package com.example.hungdt2.user;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.dto.UserMeResponse;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NotFoundException("USER_NOT_FOUND", "User not found");
        }
        Long userId = (Long) authentication.getPrincipal();
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        UserMeResponse dto = new UserMeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), user.getDisplayName(), user.getIsActive());
        return new ApiResponse<>(dto);
    }

    @GetMapping("/{id}")
    public ApiResponse<com.example.hungdt2.user.dto.UserItem> getUser(@PathVariable Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        com.example.hungdt2.user.dto.UserItem item = new com.example.hungdt2.user.dto.UserItem(user.getId(), user.getUsername(), user.getDisplayName());
        return new ApiResponse<>(item);
    }

    @GetMapping("/search")
    public ApiResponse<com.example.hungdt2.user.dto.UserItem> findByPhone(@org.springframework.web.bind.annotation.RequestParam String phone) {
        var opt = userRepository.findByPhone(phone);
        if (opt.isEmpty()) return new ApiResponse<>(null);
        var u = opt.get();
        com.example.hungdt2.user.dto.UserItem item = new com.example.hungdt2.user.dto.UserItem(u.getId(), u.getUsername(), u.getDisplayName());
        return new ApiResponse<>(item);
    }
}
