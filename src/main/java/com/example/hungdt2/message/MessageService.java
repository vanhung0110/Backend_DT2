package com.example.hungdt2.message;

import com.example.hungdt2.exceptions.ForbiddenException;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.message.dto.MessageItem;
import com.example.hungdt2.message.dto.CreateMessageRequest;
import com.example.hungdt2.room.RoomService;
import com.example.hungdt2.room.entity.RoomEntity;
import com.example.hungdt2.room.repository.RoomRepository;
import com.example.hungdt2.room.entity.RoomMemberEntity;
import com.example.hungdt2.room.repository.RoomMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public MessageService(MessageRepository messageRepository, RoomRepository roomRepository, RoomMemberRepository memberRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<MessageItem> listMessages(Long roomId, Instant before, int limit, Long requesterId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));

        // membership check
        RoomMemberEntity m = memberRepository.findByRoomIdAndUserId(roomId, requesterId).orElseThrow(() -> new ForbiddenException("NOT_ROOM_MEMBER", "Not a member of the room"));
        if (!"APPROVED".equals(m.getStatus())) throw new ForbiddenException("NOT_APPROVED", "Member not approved yet");

        if (limit <= 0) limit = 50;
        List<MessageEntity> rows = messageRepository.findMessages(roomId, before, PageRequest.of(0, limit));
        // repository returns newest first; reverse to ascending
        Collections.reverse(rows);
        return rows.stream().map(r -> new MessageItem(r.getId(), r.getRoomId(), r.getSenderId(), r.getContent(), r.getType(), r.getAudioUrl(), r.getAudioDurationMs(), r.getCreatedAt())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageItem> listMessagesAfter(Long roomId, Instant after, int limit, Long requesterId) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));

        // membership check
        RoomMemberEntity m = memberRepository.findByRoomIdAndUserId(roomId, requesterId).orElseThrow(() -> new ForbiddenException("NOT_ROOM_MEMBER", "Not a member of the room"));
        if (!"APPROVED".equals(m.getStatus())) throw new ForbiddenException("NOT_APPROVED", "Member not approved yet");

        if (limit <= 0) limit = 50;
        List<MessageEntity> rows = messageRepository.findMessagesAfter(roomId, after, PageRequest.of(0, limit));
        // repository returns ascending order already
        return rows.stream().map(r -> new MessageItem(r.getId(), r.getRoomId(), r.getSenderId(), r.getContent(), r.getType(), r.getAudioUrl(), r.getAudioDurationMs(), r.getCreatedAt())).collect(Collectors.toList());
    }

    @Transactional
    public MessageItem sendMessage(Long roomId, Long senderId, CreateMessageRequest req) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));

        RoomMemberEntity m = memberRepository.findByRoomIdAndUserId(roomId, senderId).orElseThrow(() -> new ForbiddenException("NOT_ROOM_MEMBER", "Not a member of the room"));
        if (!"APPROVED".equals(m.getStatus())) throw new ForbiddenException("NOT_APPROVED", "Member not approved yet");

        MessageEntity me = new MessageEntity();
        me.setRoomId(roomId);
        me.setSenderId(senderId);
        me.setContent(req.content());
        me.setType("TEXT");
        me.setCreatedAt(Instant.now());
        messageRepository.save(me);
        return new MessageItem(me.getId(), me.getRoomId(), me.getSenderId(), me.getContent(), me.getType(), me.getAudioUrl(), me.getAudioDurationMs(), me.getCreatedAt());
    }

    @Transactional
    public MessageItem sendAudioMessage(Long roomId, Long senderId, String audioUrl, Long durationMs) {
        RoomEntity room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND", "Room not found"));

        RoomMemberEntity m = memberRepository.findByRoomIdAndUserId(roomId, senderId).orElseThrow(() -> new ForbiddenException("NOT_ROOM_MEMBER", "Not a member of the room"));
        if (!"APPROVED".equals(m.getStatus())) throw new ForbiddenException("NOT_APPROVED", "Member not approved yet");

        MessageEntity me = new MessageEntity();
        me.setRoomId(roomId);
        me.setSenderId(senderId);
        // Ensure content is not null to avoid older DB constraints; store placeholder
        me.setContent("");
        me.setType("AUDIO");
        me.setAudioUrl(audioUrl);
        me.setAudioDurationMs(durationMs);
        me.setCreatedAt(Instant.now());
        messageRepository.save(me);
        return new MessageItem(me.getId(), me.getRoomId(), me.getSenderId(), me.getContent(), me.getType(), me.getAudioUrl(), me.getAudioDurationMs(), me.getCreatedAt());
    }
}
