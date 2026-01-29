package com.example.hungdt2.room;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.room.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateRoomResponse>> createRoom(Authentication authentication, @Valid @RequestBody CreateRoomRequest req) {
        Long userId = (Long) authentication.getPrincipal();
        CreateRoomResponse data = roomService.createRoom(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(data));
    }

    @PostMapping("/join")
    public ApiResponse<JoinRoomResponse> joinRoom(Authentication authentication, @Valid @RequestBody JoinRoomRequest req) {
        Long userId = (Long) authentication.getPrincipal();
        JoinRoomResponse data = roomService.joinByCode(userId, req.code());
        return new ApiResponse<>(data);
    }

    @GetMapping("/my")
    public ApiResponse<List<MyRoomItem>> myRooms(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<MyRoomItem> list = roomService.listMyRooms(userId);
        return new ApiResponse<>(list);
    }

    @GetMapping("/{roomId}/requests")
    public ApiResponse<List<PendingRequestItem>> listRequests(@PathVariable Long roomId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<PendingRequestItem> list = roomService.listPending(roomId, userId);
        return new ApiResponse<>(list);
    }

    @GetMapping("/{roomId}")
    public ApiResponse<CreateRoomResponse> getRoom(@PathVariable Long roomId) {
        CreateRoomResponse data = roomService.getRoomItem(roomId);
        return new ApiResponse<>(data);
    }

    @PostMapping("/direct/{userId}")
    public ResponseEntity<ApiResponse<CreateRoomResponse>> createDirectRoom(@PathVariable Long userId, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        CreateRoomResponse data = roomService.getOrCreateDirectRoom(ownerId, userId);
        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @PostMapping("/{roomId}/requests/{userId}/approve")
    public ResponseEntity<Object> approve(@PathVariable Long roomId, @PathVariable Long userId, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        roomService.approve(roomId, ownerId, userId);
        return ResponseEntity.ok(new ApiResponse<>("OK"));
    }

    @PostMapping("/{roomId}/requests/{userId}/reject")
    public ResponseEntity<Object> reject(@PathVariable Long roomId, @PathVariable Long userId, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        roomService.reject(roomId, ownerId, userId);
        return ResponseEntity.ok(new ApiResponse<>("OK"));
    }
}
