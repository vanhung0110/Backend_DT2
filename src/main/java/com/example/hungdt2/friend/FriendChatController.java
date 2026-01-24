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
    private final com.example.hungdt2.voice.VoiceService voiceService;
    private final com.example.hungdt2.websocket.UserWsHandler userWsHandler;
    private final com.example.hungdt2.user.repository.UserRepository userRepository;

    public FriendChatController(FriendChatService friendChatService, com.example.hungdt2.voice.VoiceService voiceService, com.example.hungdt2.websocket.UserWsHandler userWsHandler, com.example.hungdt2.user.repository.UserRepository userRepository) {
        this.friendChatService = friendChatService;
        this.voiceService = voiceService;
        this.userWsHandler = userWsHandler;
        this.userRepository = userRepository;
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

    // Voice endpoints for friend rooms (delegates to underlying room voice endpoints)
    @PostMapping("/{friendRoomId}/voice/join")
    public ResponseEntity<ApiResponse<com.example.hungdt2.voice.dto.VoiceJoinResponse>> joinVoice(@PathVariable Long friendRoomId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        com.example.hungdt2.voice.dto.VoiceJoinResponse resp = voiceService.joinVoiceRoom(roomId, me);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @PostMapping("/{friendRoomId}/voice/leave")
    public ResponseEntity<ApiResponse<Void>> leaveVoice(@PathVariable Long friendRoomId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        voiceService.leaveVoiceRoom(roomId, me);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @GetMapping("/{friendRoomId}/voice/members")
    public ApiResponse<java.util.List<com.example.hungdt2.voice.dto.VoiceMemberItem>> listVoiceMembers(@PathVariable Long friendRoomId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        return new ApiResponse<>(voiceService.listMembers(roomId));
    }

    @PostMapping("/{friendRoomId}/voice/kick")
    public ResponseEntity<ApiResponse<Void>> kickVoice(@PathVariable Long friendRoomId, @RequestParam Long userId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        voiceService.kickUser(roomId, me, userId);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    // Call flow: send incoming call notification to other participant (caller only)
    @PostMapping("/call")
    public ResponseEntity<ApiResponse<java.util.Map<String,Object>>> callFriend(@RequestParam Long otherUserId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        // ensure friend room exists and get friendRoomId
        Long frId = friendChatService.createOrGetFriendRoomId(me, otherUserId);
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(frId, me);
        // get caller info
        var caller = userRepository.findById(me).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("USER_NOT_FOUND", "User not found"));
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("event", "call.incoming");
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("friendRoomId", frId);
        data.put("roomId", roomId);
        data.put("callerId", me);
        data.put("callerName", caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUsername());
        payload.put("data", data);
        userWsHandler.sendToUser(otherUserId, payload);
        java.util.Map<String,Object> resp = new java.util.HashMap<>(); resp.put("friendRoomId", frId); resp.put("roomId", roomId);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @PostMapping("/{friendRoomId}/call/accept")
    public ResponseEntity<ApiResponse<Void>> acceptCall(@PathVariable Long friendRoomId, @RequestParam Long callerId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        // validate participant
        Long roomId = friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        // notify caller
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("event", "call.accepted");
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("friendRoomId", friendRoomId);
        data.put("roomId", roomId);
        data.put("calleeId", me);
        payload.put("data", data);
        userWsHandler.sendToUser(callerId, payload);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/{friendRoomId}/call/reject")
    public ResponseEntity<ApiResponse<Void>> rejectCall(@PathVariable Long friendRoomId, @RequestParam Long callerId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        // validate participant
        friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("event", "call.rejected");
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("friendRoomId", friendRoomId);
        data.put("calleeId", me);
        payload.put("data", data);
        userWsHandler.sendToUser(callerId, payload);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    // Caller cancels outgoing call
    @PostMapping("/call/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCall(@RequestParam Long friendRoomId, @RequestParam Long otherUserId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        // validate caller is participant
        friendChatService.getUnderlyingRoomIdForParticipant(friendRoomId, me);
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("event", "call.cancelled");
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("friendRoomId", friendRoomId);
        data.put("callerId", me);
        payload.put("data", data);
        userWsHandler.sendToUser(otherUserId, payload);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }
}