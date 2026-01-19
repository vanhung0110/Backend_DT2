package com.example.hungdt2.user;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.user.dto.CreateFriendRequest;
import com.example.hungdt2.user.dto.FriendItem;
import com.example.hungdt2.user.dto.FriendRequestItem;
import com.example.hungdt2.user.entity.FriendRequestEntity;
import com.example.hungdt2.user.entity.FriendshipEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.FriendshipRepository;
import com.example.hungdt2.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendController(FriendService friendService, FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendService = friendService;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequestItem>> sendRequest(Authentication authentication, @Valid @RequestBody CreateFriendRequest req) {
        Long userId = (Long) authentication.getPrincipal();
        FriendRequestEntity created = friendService.sendRequest(userId, req.phone());
        var requester = userRepository.findById(created.getRequesterId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
        var recipient = userRepository.findById(created.getRecipientId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
        FriendRequestItem item = new FriendRequestItem(created.getId(), created.getRequesterId(), created.getRecipientId(), requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername(), recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername(), created.getStatus(), created.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(item));
    }

    @GetMapping("/requests/incoming")
    public ApiResponse<List<FriendRequestItem>> incoming(Authentication authentication, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        Page<FriendRequestEntity> p = friendService.listIncomingRequests(userId, page, size);
        List<FriendRequestItem> list = p.stream().map(f -> {
            var requester = userRepository.findById(f.getRequesterId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
            var recipient = userRepository.findById(f.getRecipientId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
            return new FriendRequestItem(f.getId(), f.getRequesterId(), f.getRecipientId(), requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername(), recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername(), f.getStatus(), f.getCreatedAt());
        }).collect(Collectors.toList());
        return new ApiResponse<>(list);
    }

    @GetMapping("/requests/outgoing")
    public ApiResponse<List<FriendRequestItem>> outgoing(Authentication authentication, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        Page<FriendRequestEntity> p = friendService.listOutgoingRequests(userId, page, size);
        List<FriendRequestItem> list = p.stream().map(f -> {
            var requester = userRepository.findById(f.getRequesterId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
            var recipient = userRepository.findById(f.getRecipientId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
            return new FriendRequestItem(f.getId(), f.getRequesterId(), f.getRecipientId(), requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername(), recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername(), f.getStatus(), f.getCreatedAt());
        }).collect(Collectors.toList());
        return new ApiResponse<>(list);
    }

    @PostMapping("/requests/{requesterId}/accept")
    public ApiResponse<String> accept(@PathVariable Long requesterId, Authentication authentication) {
        Long recipientId = (Long) authentication.getPrincipal();
        friendService.acceptRequest(recipientId, requesterId);
        return new ApiResponse<>("OK");
    }

    @PostMapping("/requests/{requesterId}/reject")
    public ApiResponse<String> reject(@PathVariable Long requesterId, Authentication authentication) {
        Long recipientId = (Long) authentication.getPrincipal();
        friendService.rejectRequest(recipientId, requesterId);
        return new ApiResponse<>("OK");
    }

    @DeleteMapping("/requests/{recipientId}")
    public ApiResponse<String> cancel(@PathVariable Long recipientId, Authentication authentication) {
        Long requesterId = (Long) authentication.getPrincipal();
        friendService.cancelRequest(requesterId, recipientId);
        return new ApiResponse<>("OK");
    }

    @GetMapping
    public ApiResponse<List<FriendItem>> listFriends(Authentication authentication, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) authentication.getPrincipal();
        var p = friendshipRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(page, size));
        List<FriendItem> list = p.stream().map(f -> {
            UserEntity u = userRepository.findById(f.getFriendId()).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
            return new FriendItem(f.getUserId(), f.getFriendId(), u.getUsername(), u.getDisplayName(), u.getPhone());
        }).collect(Collectors.toList());
        return new ApiResponse<>(list);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<String> unfriend(@PathVariable Long friendId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        friendService.unfriend(userId, friendId);
        return new ApiResponse<>("OK");
    }
}
