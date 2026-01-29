package com.example.hungdt2.room;

import com.example.hungdt2.exceptions.ConflictException;
import com.example.hungdt2.exceptions.ForbiddenException;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.room.dto.*;
import com.example.hungdt2.room.entity.RoomEntity;
import com.example.hungdt2.room.entity.RoomMemberEntity;
import com.example.hungdt2.room.repository.RoomMemberRepository;
import com.example.hungdt2.room.repository.RoomRepository;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository roomRepository, RoomMemberRepository memberRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    private String generateCode() {
        int n = random.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    @Transactional
    public CreateRoomResponse createRoom(Long ownerId, CreateRoomRequest req) {
        // validate type
        String type = req.type().toUpperCase();
        if (!"PUBLIC".equals(type) && !"PRIVATE".equals(type)) {
            throw new ConflictException("VALIDATION_ERROR", "Type must be PUBLIC or PRIVATE");
        }

        // generate unique code with retries
        String code = null;
        int attempts = 0;
        while (attempts < 10) {
            attempts++;
            String c = generateCode();
            try {
                RoomEntity r = new RoomEntity();
                r.setCode(c);
                r.setName(req.name());
                r.setType(type);
                r.setOwnerId(ownerId);
                roomRepository.save(r);
                code = c;

                // create owner member
                RoomMemberEntity m = new RoomMemberEntity();
                m.setRoomId(r.getId());
                m.setUserId(ownerId);
                m.setRole("OWNER");
                m.setStatus("APPROVED");
                memberRepository.save(m);

                // if members provided, add them as APPROVED members (owner invited)
                if (req.members() != null && !req.members().isEmpty()) {
                    for (Long uid : req.members()) {
                        if (uid == null || uid.equals(ownerId)) continue;
                        // ensure user exists
                        userRepository.findById(uid).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
                        RoomMemberEntity mm = new RoomMemberEntity();
                        mm.setRoomId(r.getId());
                        mm.setUserId(uid);
                        mm.setRole("MEMBER");
                        mm.setStatus("APPROVED");
                        try {
                            memberRepository.save(mm);
                        } catch (DataIntegrityViolationException ex) {
                            // ignore duplicate member
                        }
                    }
                }

                return new CreateRoomResponse(r.getId(), r.getCode(), r.getName(), r.getType(), r.getOwnerId(), r.getVoiceEnabled()==null?false:r.getVoiceEnabled());
            } catch (DataIntegrityViolationException ex) {
                // assume code conflict, retry
            }
        }

        throw new ConflictException("ROOM_CODE_GENERATION_FAILED", "Failed to generate unique room code");
    }

    @Transactional
    public CreateRoomResponse getOrCreateDirectRoom(Long ownerId, Long otherUserId) {
        // validate other user exists
        userRepository.findById(otherUserId).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        // try find existing private room with exactly these two users
        var opt = roomRepository.findDirectRoomBetweenUsers(ownerId, otherUserId);
        if (opt.isPresent()) {
            RoomEntity r = opt.get();
            return new CreateRoomResponse(r.getId(), r.getCode(), r.getName(), r.getType(), r.getOwnerId(), r.getVoiceEnabled()==null?false:r.getVoiceEnabled());
        }

        // otherwise create a new private room and add the other user
        CreateRoomRequest req = new CreateRoomRequest("", "PRIVATE", java.util.List.of(otherUserId), false);
        CreateRoomResponse cr = createRoom(ownerId, req);
        // ensure direct rooms have voice enabled by default
        setRoomVoiceEnabled(cr.id(), true);
        return new CreateRoomResponse(cr.id(), cr.code(), cr.name(), cr.type(), cr.ownerId(), true);
    }

    @Transactional
    public void setRoomVoiceEnabled(Long roomId, boolean enabled) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        room.setVoiceEnabled(enabled);
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public CreateRoomResponse getRoomItem(Long roomId) {
        RoomEntity r = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        return new CreateRoomResponse(r.getId(), r.getCode(), r.getName(), r.getType(), r.getOwnerId(), r.getVoiceEnabled()==null?false:r.getVoiceEnabled());
    }

    @Transactional
    public CreateRoomResponse createRoomWithMembers(Long ownerId, String name, String type, java.util.List<Long> members) {
        CreateRoomRequest req = new CreateRoomRequest(name == null ? "" : name, type, members, false);
        return createRoom(ownerId, req);
    }

    @Transactional
    public JoinRoomResponse joinByCode(Long userId, String code) {
        RoomEntity room = roomRepository.findByCode(code).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        // check existing membership
        Optional<RoomMemberEntity> existing = memberRepository.findByRoomIdAndUserId(room.getId(), userId);
        if (existing.isPresent()) {
            String st = existing.get().getStatus();
            if ("APPROVED".equals(st)) throw new ConflictException("ALREADY_JOINED", "User already joined");
            if ("PENDING".equals(st)) throw new ConflictException("JOIN_ALREADY_PENDING", "Join already pending");
            // if REJECTED, we allow re-request
        }

        RoomMemberEntity member = new RoomMemberEntity();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRole("MEMBER");
        if ("PUBLIC".equals(room.getType())) {
            member.setStatus("APPROVED");
            memberRepository.save(member);
            return new JoinRoomResponse(room.getId(), "APPROVED");
        } else {
            member.setStatus("PENDING");
            memberRepository.save(member);
            return new JoinRoomResponse(room.getId(), "PENDING");
        }
    }

    @Transactional(readOnly = true)
    public List<MyRoomItem> listMyRooms(Long userId) {
        List<RoomMemberEntity> memberships = memberRepository.findByUserId(userId);
        return memberships.stream().map(m -> {
            RoomEntity r = roomRepository.findById(m.getRoomId()).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
            return new MyRoomItem(r.getId(), r.getCode(), r.getName(), r.getType(), m.getRole(), m.getStatus());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PendingRequestItem> listPending(Long roomId, Long requesterId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        if (!room.getOwnerId().equals(requesterId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        List<RoomMemberEntity> pendings = memberRepository.findByRoomIdAndStatus(roomId, "PENDING");
        return pendings.stream().map(m -> {
            UserEntity u = userRepository.findById(m.getUserId()).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
            return new PendingRequestItem(u.getId(), u.getUsername(), u.getDisplayName(), m.getStatus());
        }).collect(Collectors.toList());
    }

    @Transactional
    public void approve(Long roomId, Long ownerId, Long targetUserId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        if (!room.getOwnerId().equals(ownerId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        RoomMemberEntity req = memberRepository.findByRoomIdAndUserId(roomId, targetUserId).orElseThrow(() -> new NotFoundException("REQUEST_NOT_FOUND", "Request not found"));
        if (!"PENDING".equals(req.getStatus())) throw new ConflictException("REQUEST_NOT_PENDING", "Request not pending");
        req.setStatus("APPROVED");
        memberRepository.save(req);
    }

    @Transactional
    public void reject(Long roomId, Long ownerId, Long targetUserId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));
        if (!room.getOwnerId().equals(ownerId)) throw new ForbiddenException("NOT_ROOM_OWNER", "Not room owner");
        RoomMemberEntity req = memberRepository.findByRoomIdAndUserId(roomId, targetUserId).orElseThrow(() -> new NotFoundException("REQUEST_NOT_FOUND", "Request not found"));
        if (!"PENDING".equals(req.getStatus())) throw new ConflictException("REQUEST_NOT_PENDING", "Request not pending");
        req.setStatus("REJECTED");
        memberRepository.save(req);
    }
}