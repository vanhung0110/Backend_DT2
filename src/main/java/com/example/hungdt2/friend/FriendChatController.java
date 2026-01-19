package com.example.hungdt2.friend;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.message.dto.CreateMessageRequest;
import com.example.hungdt2.message.dto.MessageItem;
import com.example.hungdt2.room.dto.CreateRoomResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/friend-rooms")
public class FriendChatController {

    private final FriendChatService friendChatService;

    public FriendChatController(FriendChatService friendChatService) {
        this.friendChatService = friendChatService;
    }

    @PostMapping("/direct/{userId}")
    public ResponseEntity<ApiResponse<CreateRoomResponse>> createDirectFriendRoom(@PathVariable Long userId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        CreateRoomResponse data = friendChatService.getOrCreateFriendRoom(me, userId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping
    public ApiResponse<java.util.List<com.example.hungdt2.room.dto.CreateRoomResponse>> listFriendRooms(Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        return new ApiResponse<>(friendChatService.listFriendRooms(me));
    }

    @GetMapping("/{friendRoomId}/messages")
    public ApiResponse<List<MessageItem>> listMessages(@PathVariable Long friendRoomId, @RequestParam(required = false) Long before, @RequestParam(defaultValue = "200") int limit, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        Instant b = before == null ? null : Instant.ofEpochMilli(before);
        List<MessageItem> list = friendChatService.listMessages(friendRoomId, b, limit, me);
        return new ApiResponse<>(list);
    }

    @PostMapping("/{friendRoomId}/messages")
    public ResponseEntity<ApiResponse<MessageItem>> postMessage(@PathVariable Long friendRoomId, @Valid @RequestBody CreateMessageRequest req, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        MessageItem m = friendChatService.sendMessage(friendRoomId, me, req);
        return ResponseEntity.status(201).body(new ApiResponse<>(m));
    }

    @PostMapping("/{friendRoomId}/messages/audio")
    public ResponseEntity<ApiResponse<MessageItem>> postAudioMessage(@PathVariable Long friendRoomId, @RequestParam String url, @RequestParam(required = false) Long durationMs, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        MessageItem m = friendChatService.sendAudioMessage(friendRoomId, me, url, durationMs);
        return ResponseEntity.status(201).body(new ApiResponse<>(m));
    }
}