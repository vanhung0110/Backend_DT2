package com.example.hungdt2.friend;

import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.friend.entity.FriendRoomEntity;
import com.example.hungdt2.friend.entity.FriendMessageEntity;
import com.example.hungdt2.friend.repository.FriendRoomRepository;
import com.example.hungdt2.friend.repository.FriendMessageRepository;
import com.example.hungdt2.room.RoomService;
import com.example.hungdt2.message.MessageService;
import com.example.hungdt2.message.dto.MessageItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendChatService {

    private final FriendRoomRepository friendRoomRepository;
    private final FriendMessageRepository friendMessageRepository;
    private final RoomService roomService;
    private final MessageService messageService;

    public FriendChatService(FriendRoomRepository friendRoomRepository, FriendMessageRepository friendMessageRepository, RoomService roomService, MessageService messageService) {
        this.friendRoomRepository = friendRoomRepository;
        this.friendMessageRepository = friendMessageRepository;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    @Transactional
    public com.example.hungdt2.room.dto.CreateRoomResponse getOrCreateFriendRoom(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) throw new NotFoundException("INVALID_USER", "Cannot create friend room with self");
        var opt = friendRoomRepository.findBetweenUsers(userId, otherUserId);
        if (opt.isPresent()) {
            var fr = opt.get();
            var r = roomService.getRoomItem(fr.getRoomId());
            return r;
        }
        // create underlying room as private and add both users
        var createdRoom = roomService.createRoomWithMembers(userId, "", "PRIVATE", java.util.List.of(otherUserId));
        // enable voice for friend rooms
        roomService.setRoomVoiceEnabled(createdRoom.id(), true);
        FriendRoomEntity f = new FriendRoomEntity();
        f.setRoomId(createdRoom.id());
        // normalize order for unique constraint (store smaller id in userA)
        if (userId < otherUserId) { f.setUserA(userId); f.setUserB(otherUserId); } else { f.setUserA(otherUserId); f.setUserB(userId); }
        friendRoomRepository.save(f);
        // return fresh DTO with voice enabled
        return new com.example.hungdt2.room.dto.CreateRoomResponse(createdRoom.id(), createdRoom.code(), createdRoom.name(), createdRoom.type(), createdRoom.ownerId(), true);
    }

    public List<com.example.hungdt2.message.dto.MessageItem> listMessages(Long friendRoomId, java.time.Instant before, int limit, Long requesterId) {
        FriendRoomEntity fr = friendRoomRepository.findById(friendRoomId).orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found"));
        if (!(fr.getUserA().equals(requesterId) || fr.getUserB().equals(requesterId))) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_PARTICIPANT", "Not a participant");
        // delegate to messageService with underlying room id
        return messageService.listMessages(fr.getRoomId(), before, limit, requesterId);
    }

    @Transactional
    public MessageItem sendMessage(Long friendRoomId, Long senderId, com.example.hungdt2.message.dto.CreateMessageRequest req) {
        FriendRoomEntity fr = friendRoomRepository.findById(friendRoomId).orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found"));
        if (!(fr.getUserA().equals(senderId) || fr.getUserB().equals(senderId))) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_PARTICIPANT", "Not a participant");
        MessageItem m = messageService.sendMessage(fr.getRoomId(), senderId, req);
        FriendMessageEntity fm = new FriendMessageEntity();
        fm.setFriendRoomId(friendRoomId);
        fm.setMessageId(m.id());
        friendMessageRepository.save(fm);
        return m;
    }

    @Transactional
    public MessageItem sendAudioMessage(Long friendRoomId, Long senderId, String url, Long durationMs) {
        FriendRoomEntity fr = friendRoomRepository.findById(friendRoomId).orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found"));
        if (!(fr.getUserA().equals(senderId) || fr.getUserB().equals(senderId))) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_PARTICIPANT", "Not a participant");
        MessageItem m = messageService.sendAudioMessage(fr.getRoomId(), senderId, url, durationMs);
        FriendMessageEntity fm = new FriendMessageEntity();
        fm.setFriendRoomId(friendRoomId);
        fm.setMessageId(m.id());
        friendMessageRepository.save(fm);
        return m;
    }

    public java.util.List<com.example.hungdt2.room.dto.CreateRoomResponse> listFriendRooms(Long userId) {
        var list = friendRoomRepository.findByUserAOrUserB(userId, userId);
        return list.stream().map(fr -> roomService.getRoomItem(fr.getRoomId())).collect(Collectors.toList());
    }

    /**
     * Return underlying roomId for a friendRoom after validating the requester is a participant.
     */
    public Long getUnderlyingRoomIdForParticipant(Long friendRoomId, Long requesterId) {
        FriendRoomEntity fr = friendRoomRepository.findById(friendRoomId).orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found"));
        if (!(fr.getUserA().equals(requesterId) || fr.getUserB().equals(requesterId))) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_PARTICIPANT", "Not a participant");
        return fr.getRoomId();
    }

    /**
     * Return the other participant's userId for a friend room after validation.
     */
    public Long getOtherParticipant(Long friendRoomId, Long requesterId) {
        FriendRoomEntity fr = friendRoomRepository.findById(friendRoomId).orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found"));
        if (!(fr.getUserA().equals(requesterId) || fr.getUserB().equals(requesterId))) throw new com.example.hungdt2.exceptions.ForbiddenException("NOT_PARTICIPANT", "Not a participant");
        return fr.getUserA().equals(requesterId) ? fr.getUserB() : fr.getUserA();
    }

    /**
     * Ensure a friend room exists between the users and return the friend_room.id
     */
    @Transactional
    public Long createOrGetFriendRoomId(Long userId, Long otherUserId) {
        // ensure room exists (this will create underlying room and friend entry if needed)
        getOrCreateFriendRoom(userId, otherUserId);
        var opt = friendRoomRepository.findBetweenUsers(userId, otherUserId);
        return opt.orElseThrow(() -> new NotFoundException("FRIEND_ROOM_NOT_FOUND", "Friend room not found")).getId();
    }
}