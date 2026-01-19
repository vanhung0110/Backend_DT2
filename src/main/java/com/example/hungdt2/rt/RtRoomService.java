package com.example.hungdt2.rt;

import com.example.hungdt2.exceptions.ForbiddenException;
import com.example.hungdt2.room.RoomService;
import com.example.hungdt2.voice.AgoraTokenService;
import com.example.hungdt2.voice.dto.VoiceJoinResponse;
import com.example.hungdt2.rt.dto.CreateRtRoomRequest;
import com.example.hungdt2.rt.dto.CreateRtRoomResponse;
import com.example.hungdt2.rt.dto.RtMemberItem;
import com.example.hungdt2.rt.entity.RtMemberEntity;
import com.example.hungdt2.rt.entity.RtRoomEntity;
import com.example.hungdt2.rt.entity.RtInviteEntity;
import com.example.hungdt2.rt.repository.RtMemberRepository;
import com.example.hungdt2.rt.repository.RtRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RtRoomService {

    private final RtRoomRepository roomRepository;
    private final RtMemberRepository memberRepository;
    private final AgoraTokenService agoraTokenService;
    private final com.example.hungdt2.websocket.RoomWsHandler roomWsHandler;
    private final com.example.hungdt2.websocket.UserWsHandler userWsHandler;
    private final com.example.hungdt2.rt.repository.RtInviteRepository inviteRepository;

    public RtRoomService(RtRoomRepository roomRepository, RtMemberRepository memberRepository, AgoraTokenService agoraTokenService, @Lazy com.example.hungdt2.websocket.RoomWsHandler roomWsHandler, com.example.hungdt2.websocket.UserWsHandler userWsHandler, com.example.hungdt2.rt.repository.RtInviteRepository inviteRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.agoraTokenService = agoraTokenService;
        this.roomWsHandler = roomWsHandler;
        this.userWsHandler = userWsHandler;
        this.inviteRepository = inviteRepository;
    }

    @Transactional
    public CreateRtRoomResponse createRoom(Long ownerId, CreateRtRoomRequest req) {
        RtRoomEntity e = new RtRoomEntity();
        e.setName(req.name);
        e.setIsPublic(req.isPublic == null ? true : req.isPublic);
        e.setOwnerId(ownerId);
        e.setDescription(req.description);
        e.setMaxMembers(req.maxMembers == null ? 50 : req.maxMembers);
        e.setLastActivityAt(Instant.now());
        e.setSleeping(true);
        e.setActive(false);
        roomRepository.save(e);
        CreateRtRoomResponse r = new CreateRtRoomResponse(); r.id = e.getId(); r.name = e.getName(); r.isPublic = e.getIsPublic(); r.ownerId = e.getOwnerId();
        return r;
    }

    @Transactional(readOnly = true)
    public List<RtRoomEntity> listPublicRooms() { return roomRepository.findByIsPublicTrueOrderByLastActivityAtDesc(); }

    @Transactional(readOnly = true)
    public RtRoomEntity getRoomById(Long id) { return roomRepository.findById(id).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found")); }

    @Transactional(readOnly = true)
    public java.util.List<RtRoomEntity> listMyRooms(Long userId) {
        var members = memberRepository.findByUserId(userId);
        var roomIds = members.stream().map(m -> m.getRoomId()).distinct().toList();
        var rooms = roomRepository.findAllById(roomIds);
        var roomMap = new java.util.HashMap<Long, RtRoomEntity>();
        for (RtRoomEntity r : rooms) roomMap.put(r.getId(), r);
        return roomIds.stream().map(id -> roomMap.get(id)).filter(r -> r != null).collect(Collectors.toList());
    }

    @Transactional
    public VoiceJoinResponse joinRoom(Long roomId, Long userId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found"));

        // If room is private, allow join only if owner, already member, or invited and accepted
        if (Boolean.FALSE.equals(room.getIsPublic())) {
            if (!room.getOwnerId().equals(userId)) {
                var invitedOpt = inviteRepository.findByRoomIdAndInvitedUserId(roomId, userId);
                var already = memberRepository.findByRoomIdAndUserId(roomId, userId);
                if (already.isEmpty()) {
                    if (invitedOpt.isEmpty() || !"ACCEPTED".equals(invitedOpt.get().getStatus())) {
                        throw new ForbiddenException("RT_PRIVATE", "Room is private");
                    }
                }
            }
        }

        var opt = memberRepository.findByRoomIdAndUserId(roomId, userId);
        RtMemberEntity me;
        if (opt.isPresent()) {
            me = opt.get();
            if (Boolean.TRUE.equals(me.getKicked())) throw new ForbiddenException("RT_KICKED", "You are kicked from this room");
            me.setLastSeen(Instant.now());
            me.setOnline(true);
            memberRepository.save(me);
        } else {
            // enforce maxMembers
            int current = (int) memberRepository.findByRoomId(roomId).stream().filter(m -> !Boolean.TRUE.equals(m.getKicked())).map(m -> m.getUserId()).distinct().count();
            if (current >= room.getMaxMembers()) throw new ForbiddenException("ROOM_FULL", "Room is full");
            me = new RtMemberEntity(); me.setRoomId(roomId); me.setUserId(userId); me.setRole("member"); me.setJoinedAt(Instant.now()); me.setOnline(true);
            try {
                memberRepository.save(me);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // possible race: another thread inserted the member concurrently; treat as success and load the existing member
                var existing = memberRepository.findByRoomIdAndUserId(roomId, userId);
                if (existing.isPresent()) {
                    me = existing.get();
                    me.setLastSeen(Instant.now());
                    me.setOnline(true);
                    memberRepository.save(me);
                } else throw e;
            }
        }
        // update room active state after join
        updateRoomActiveState(roomId);
        int ttl = 60 * 5;
        String token = agoraTokenService.generateToken(roomId, userId, ttl);
        if (token != null && (token.startsWith("stub-") || token.startsWith("agora-fallback:"))) token = null;
        Long expiresAt = Instant.now().plusSeconds(ttl).toEpochMilli();
        String channel = "rt-room-" + roomId;
        String appId = agoraTokenService.getAppId();
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.joined", "data", java.util.Map.of("userId", userId)));
        emitMembersUpdate(roomId);
        return new VoiceJoinResponse(token, channel, expiresAt, appId);
    }

    @Transactional
    public void inviteUser(Long roomId, Long ownerId, Long invitedUserId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        if (!room.getOwnerId().equals(ownerId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        var exists = inviteRepository.findByRoomIdAndInvitedUserId(roomId, invitedUserId);
        if (exists.isPresent()) {
            var inv = exists.get();
            // if previously rejected, allow reinvite (set to PENDING)
            if ("REJECTED".equals(inv.getStatus())) {
                inv.setStatus("PENDING");
                inv.setInvitedBy(ownerId);
                inviteRepository.save(inv);
                try {
                    userWsHandler.sendToUser(invitedUserId, java.util.Map.of("type", "rt.invite.created", "data", java.util.Map.of("id", inv.getId(), "roomId", roomId, "invitedUserId", invitedUserId, "invitedBy", ownerId)));
                } catch (Exception e) { /* best-effort */ }
            }
            return; // idempotent otherwise
        }
        RtInviteEntity invite = new RtInviteEntity(); invite.setRoomId(roomId); invite.setInvitedUserId(invitedUserId); invite.setInvitedBy(ownerId); inviteRepository.save(invite);
        // notify invited user via user-level WS
        try {
            userWsHandler.sendToUser(invitedUserId, java.util.Map.of("type", "rt.invite.created", "data", java.util.Map.of("id", invite.getId(), "roomId", roomId, "invitedUserId", invitedUserId, "invitedBy", ownerId)));
        } catch (Exception e) { /* best-effort */ }

    }

    @Transactional(readOnly = true)
    public java.util.List<RtInviteEntity> listInvitesForRoom(Long roomId) {
        return inviteRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public java.util.List<RtInviteEntity> listInvitesForUser(Long userId) {
        return inviteRepository.findByInvitedUserId(userId);
    }

    @Transactional
    public VoiceJoinResponse acceptInvite(Long inviteId, Long userId) {
        var invite = inviteRepository.findById(inviteId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("INVITE_NOT_FOUND", "Invite not found"));
        if (!invite.getInvitedUserId().equals(userId)) throw new ForbiddenException("NOT_INVITED_USER", "Not invited user");
        if ("REJECTED".equals(invite.getStatus())) throw new ForbiddenException("INVITE_REJECTED", "Invite already rejected");
        if ("ACCEPTED".equals(invite.getStatus())) {
            // idempotent: already accepted, just return join response
            return joinRoom(invite.getRoomId(), userId);
        }
        invite.setStatus("ACCEPTED"); inviteRepository.save(invite);
        // notify owner (accepted)
        try { userWsHandler.sendToUser(invite.getInvitedBy(), java.util.Map.of("type", "rt.invite.accepted", "data", java.util.Map.of("id", invite.getId(), "roomId", invite.getRoomId(), "invitedUserId", invite.getInvitedUserId()))); } catch (Exception e) { }
        // auto-join user on accept
        return joinRoom(invite.getRoomId(), userId);
    }

    @Transactional
    public void rejectInvite(Long inviteId, Long userId) {
        var invite = inviteRepository.findById(inviteId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("INVITE_NOT_FOUND", "Invite not found"));
        if (!invite.getInvitedUserId().equals(userId)) throw new ForbiddenException("NOT_INVITED_USER", "Not invited user");
        if ("REJECTED".equals(invite.getStatus())) return; // idempotent
        if ("ACCEPTED".equals(invite.getStatus())) throw new ForbiddenException("INVITE_ACCEPTED", "Invite already accepted");
        invite.setStatus("REJECTED"); inviteRepository.save(invite);
        // notify owner (rejected)
        try { userWsHandler.sendToUser(invite.getInvitedBy(), java.util.Map.of("type", "rt.invite.rejected", "data", java.util.Map.of("id", invite.getId(), "roomId", invite.getRoomId(), "invitedUserId", invite.getInvitedUserId()))); } catch (Exception e) { }
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        var opt = memberRepository.findByRoomIdAndUserId(roomId, userId);
        if (opt.isPresent()) { var m = opt.get(); m.setLastSeen(Instant.now()); m.setOnline(false); memberRepository.save(m); }
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.left", "data", java.util.Map.of("userId", userId)));
        emitMembersUpdate(roomId);
        updateRoomActiveState(roomId);
    }

    @Transactional
    public void kickMember(Long roomId, Long ownerId, Long targetUserId) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        if (!room.getOwnerId().equals(ownerId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        var opt = memberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (opt.isEmpty()) throw new com.example.hungdt2.exceptions.NotFoundException("RT_MEMBER_NOT_FOUND","Member not found");
        var m = opt.get(); m.setKicked(true); m.setOnline(false); memberRepository.save(m);
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.kicked", "data", java.util.Map.of("userId", targetUserId)));
        emitMembersUpdate(roomId);
        updateRoomActiveState(roomId);
    }

    @Transactional
    public void setMemberMute(Long roomId, Long actorUserId, Long targetUserId, boolean muted) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        // allow self mute or owner mute others
        if (!actorUserId.equals(targetUserId) && !room.getOwnerId().equals(actorUserId)) throw new ForbiddenException("NOT_ALLOWED", "Not allowed to mute user");
        var opt = memberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (opt.isEmpty()) throw new com.example.hungdt2.exceptions.NotFoundException("RT_MEMBER_NOT_FOUND","Member not found");
        var m = opt.get(); m.setMuted(muted); memberRepository.save(m);
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.member.muted", "data", java.util.Map.of("userId", targetUserId, "muted", muted)));
        emitMembersUpdate(roomId);
    }

    @Transactional
    public void setMemberVolume(Long roomId, Long actorUserId, Long targetUserId, Integer volume) {
        var room = roomRepository.findById(roomId).orElseThrow(() -> new com.example.hungdt2.exceptions.NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        // allow self volume change or owner change others
        if (!actorUserId.equals(targetUserId) && !room.getOwnerId().equals(actorUserId)) throw new ForbiddenException("NOT_ALLOWED", "Not allowed to change volume");
        var opt = memberRepository.findByRoomIdAndUserId(roomId, targetUserId);
        if (opt.isEmpty()) throw new com.example.hungdt2.exceptions.NotFoundException("RT_MEMBER_NOT_FOUND","Member not found");
        var m = opt.get(); m.setVolume(volume); memberRepository.save(m);
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.member.volume", "data", java.util.Map.of("userId", targetUserId, "volume", volume)));
        emitMembersUpdate(roomId);
    }

    @Transactional(readOnly = true)
    public java.util.List<RtMemberItem> listMembers(Long roomId) {
        return memberRepository.findByRoomId(roomId).stream().map(m -> new RtMemberItem(m.getUserId(), m.getRole(), m.getJoinedAt()==null?null:m.getJoinedAt().toEpochMilli(), m.getMuted(), m.getVolume(), m.getKicked(), m.getOnline())).collect(Collectors.toList());
    }

    private void emitMembersUpdate(Long roomId) {
        var members = listMembers(roomId);
        roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.members.updated", "data", java.util.Map.of("members", members)));
    }

    private void updateRoomActiveState(Long roomId) {
        var roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return;
        var room = roomOpt.get();
        var onlineCount = (int) memberRepository.findByRoomId(roomId).stream().filter(m -> !Boolean.TRUE.equals(m.getKicked()) && Boolean.TRUE.equals(m.getOnline())).count();
        boolean wasActive = Boolean.TRUE.equals(room.getActive());
        boolean shouldActive = onlineCount >= 2;
        room.setActive(shouldActive);
        room.setSleeping(!shouldActive);
        roomRepository.save(room);
        if (wasActive != shouldActive) {
            roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.state", "data", java.util.Map.of("active", shouldActive, "onlineCount", onlineCount)));
        }
    }

    @Transactional
    public void onWsDisconnect(Long roomId, Long userId) {
        if (roomId == null || userId == null) return;
        var opt = memberRepository.findByRoomIdAndUserId(roomId, userId);
        if (opt.isPresent()) {
            var m = opt.get();
            m.setOnline(false);
            m.setLastSeen(Instant.now());
            memberRepository.save(m);
            roomWsHandler.sendToRoom(String.valueOf(roomId), java.util.Map.of("type", "rt.room.disconnected", "data", java.util.Map.of("userId", userId)));
            emitMembersUpdate(roomId);
            updateRoomActiveState(roomId);
        }
    }
}
