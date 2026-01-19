package com.example.hungdt2.rt;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.voice.dto.VoiceJoinResponse;
import com.example.hungdt2.rt.dto.CreateRtRoomRequest;
import com.example.hungdt2.rt.dto.CreateRtRoomResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rt-rooms")
public class RtRoomController {

    private final RtRoomService service;

    public RtRoomController(RtRoomService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateRtRoomResponse>> create(@RequestBody CreateRtRoomRequest req, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        var r = service.createRoom(me, req);
        return ResponseEntity.status(201).body(new ApiResponse<>(r));
    }

    @GetMapping
    public ApiResponse<List<com.example.hungdt2.rt.entity.RtRoomEntity>> list() {
        return new ApiResponse<>(service.listPublicRooms());
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<java.util.List<com.example.hungdt2.rt.entity.RtRoomEntity>>> mine(Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(service.listMyRooms(me)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<com.example.hungdt2.rt.entity.RtRoomEntity>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(service.getRoomById(id)));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<VoiceJoinResponse>> join(@PathVariable Long id, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        var resp = service.joinRoom(id, me);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long id, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.leaveRoom(id, me);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/{id}/kick")
    public ResponseEntity<ApiResponse<Void>> kick(@PathVariable Long id, @RequestParam Long userId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.kickMember(id, me, userId);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<Void>> invite(@PathVariable Long id, @RequestParam Long userId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.inviteUser(id, me, userId);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @GetMapping("/{id}/invites")
    public ResponseEntity<ApiResponse<java.util.List<com.example.hungdt2.rt.entity.RtInviteEntity>>> invites(@PathVariable Long id, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        var room = service.getRoomById(id);
        if (!room.getOwnerId().equals(me)) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_ROOM_OWNER", "Only room owner can list invites");
        return ResponseEntity.ok(new ApiResponse<>(service.listInvitesForRoom(id)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<java.util.List<com.example.hungdt2.rt.dto.RtMemberItem>>> members(@PathVariable Long id, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        var room = service.getRoomById(id);
        if (Boolean.FALSE.equals(room.getIsPublic())) {
            // private room: only owner or a member can view
            var members = service.listMembers(id);
            boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(me));
            if (!room.getOwnerId().equals(me) && !isMember) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_ALLOWED", "Not allowed to view members of a private room");
            return ResponseEntity.ok(new ApiResponse<>(members));
        }
        return ResponseEntity.ok(new ApiResponse<>(service.listMembers(id)));
    }

    @PostMapping("/{id}/members/{userId}/mute")
    public ResponseEntity<ApiResponse<Void>> mute(@PathVariable Long id, @PathVariable Long userId, @RequestParam boolean muted, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.setMemberMute(id, me, userId, muted);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/{id}/members/{userId}/volume")
    public ResponseEntity<ApiResponse<Void>> volume(@PathVariable Long id, @PathVariable Long userId, @RequestParam Integer volume, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.setMemberVolume(id, me, userId, volume);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @GetMapping("/invites/mine")
    public ResponseEntity<ApiResponse<java.util.List<com.example.hungdt2.rt.entity.RtInviteEntity>>> myInvites(Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(new ApiResponse<>(service.listInvitesForUser(me)));
    }

    @PostMapping("/{roomId}/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<VoiceJoinResponse>> acceptInvite(@PathVariable Long roomId, @PathVariable Long inviteId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        var resp = service.acceptInvite(inviteId, me);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @PostMapping("/{roomId}/invites/{inviteId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvite(@PathVariable Long roomId, @PathVariable Long inviteId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        service.rejectInvite(inviteId, me);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

}
