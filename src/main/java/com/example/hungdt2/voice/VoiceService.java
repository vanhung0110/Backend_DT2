package com.example.hungdt2.voice;

import com.example.hungdt2.exceptions.ForbiddenException;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.voice.dto.VoiceJoinResponse;
import com.example.hungdt2.voice.entity.VoiceMemberEntity;
import com.example.hungdt2.voice.repository.VoiceMemberRepository;
import com.example.hungdt2.room.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoiceService {

    private final VoiceMemberRepository voiceMemberRepository;
    private final RoomService roomService;
    private final AgoraTokenService agoraTokenService;
    private final com.example.hungdt2.websocket.RoomWsHandler roomWsHandler;

    public VoiceService(VoiceMemberRepository voiceMemberRepository, RoomService roomService, AgoraTokenService agoraTokenService, com.example.hungdt2.websocket.RoomWsHandler roomWsHandler) {
        this.voiceMemberRepository = voiceMemberRepository;
        this.roomService = roomService;
        this.agoraTokenService = agoraTokenService;
        this.roomWsHandler = roomWsHandler;
    }

    @Transactional
    public VoiceJoinResponse joinVoiceRoom(Long roomId, Long userId) {
        // ensure room exists (preserve existing access checks elsewhere) - allow join even if private; membership rules will be checked by room membership
        var room = roomService.getRoomItem(roomId);

        var opt = voiceMemberRepository.findByRoomIdAndUserId(roomId, userId);
        VoiceMemberEntity vm;
        if (opt.isPresent()) {
            vm = opt.get();
            if (Boolean.TRUE.equals(vm.getKicked())) {
                throw new ForbiddenException("VOICE_KICKED", "You are kicked from voice in this room");
            }
            vm.setLastSeen(Instant.now());
        } else {
            vm = new VoiceMemberEntity();
            vm.setRoomId(roomId);
            vm.setUserId(userId);
            vm.setRole("member");
            vm.setJoinedAt(Instant.now());
            voiceMemberRepository.save(vm);
        }

        // create ephemeral token using AgoraTokenService (may be stubbed)
        int ttl = 60 * 5;
        String token = agoraTokenService.generateToken(roomId, userId, ttl);
        // If token is placeholder/stub, return null token so client may attempt tokenless join (if App Certificate not enforced)
        if (token != null && (token.startsWith("stub-") || token.startsWith("agora-fallback:"))) {
            token = null;
        }
        Long expiresAt = Instant.now().plusSeconds(ttl).toEpochMilli();
        String channel = "room-" + roomId;
        String appId = agoraTokenService.getAppId();

        // notify room members that someone joined voice
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "voice.joined", "data", java.util.Map.of("userId", userId, "channel", channel)));
        // also emit members update
        emitMembersUpdate(roomId);
        return new VoiceJoinResponse(token, channel, expiresAt, appId);
    }

    @Transactional
    public void leaveVoiceRoom(Long roomId, Long userId) {
        var opt = voiceMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (opt.isPresent()) {
            VoiceMemberEntity vm = opt.get();
            vm.setLastSeen(Instant.now());
            voiceMemberRepository.save(vm);
        }
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "voice.left", "data", java.util.Map.of("userId", userId)));
        emitMembersUpdate(roomId);
    }

    @Transactional
    public void kickUser(Long roomId, Long ownerId, Long targetUserId) {
        var room = roomService.getRoomItem(roomId);
        if (!room.ownerId().equals(ownerId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        var opt = voiceMemberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (opt.isEmpty()) throw new NotFoundException("VOICE_MEMBER_NOT_FOUND", "Voice member not found");
        VoiceMemberEntity vm = opt.get();
        vm.setKicked(true);
        voiceMemberRepository.save(vm);
        // emit kicked event and members update
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "voice.kicked", "data", java.util.Map.of("userId", targetUserId)));
        emitMembersUpdate(roomId);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.example.hungdt2.voice.dto.VoiceMemberItem> listMembers(Long roomId) {
        List<VoiceMemberEntity> list = voiceMemberRepository.findByRoomId(roomId);
        return list.stream().map(m -> new com.example.hungdt2.voice.dto.VoiceMemberItem(m.getUserId(), m.getRole(), m.getLastSeen()==null?null:m.getLastSeen().toEpochMilli(), m.getKicked())).collect(Collectors.toList());
    }

    private void emitMembersUpdate(Long roomId) {
        var members = listMembers(roomId);
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "voice.members.updated", "data", java.util.Map.of("members", members)));
    }
}