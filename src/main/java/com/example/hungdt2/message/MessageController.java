package com.example.hungdt2.message;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.message.dto.CreateMessageRequest;
import com.example.hungdt2.message.dto.MessageItem;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<MessageItem>> listMessages(@PathVariable Long roomId,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "50") int limit,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Instant b = null;
        Instant a = null;
        if (before != null)
            b = Instant.ofEpochMilli(before);
        if (after != null)
            a = Instant.ofEpochMilli(after);
        List<MessageItem> list;
        if (a != null) {
            list = messageService.listMessagesAfter(roomId, a, limit, userId);
        } else {
            list = messageService.listMessages(roomId, b, limit, userId);
        }
        return new ApiResponse<>(list);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageItem>> postMessage(@PathVariable Long roomId,
            Authentication authentication, @Valid @RequestBody CreateMessageRequest req) {
        Long userId = (Long) authentication.getPrincipal();
        MessageItem item = messageService.sendMessage(roomId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(item));
    }

    @PostMapping(path = "/audio", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MessageItem>> postAudioMessage(@PathVariable Long roomId,
            Authentication authentication,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "durationMs", required = false) Long durationMs) {
        Long userId = (Long) authentication.getPrincipal();
        // save file to uploads
        try {
            String baseDir = System.getProperty("user.dir") + "/uploads/rooms/" + roomId;
            java.nio.file.Path dir = java.nio.file.Paths.get(baseDir);
            java.nio.file.Files.createDirectories(dir);
            String fn = java.util.UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            java.nio.file.Path dest = dir.resolve(fn);
            try (java.io.InputStream is = file.getInputStream()) {
                java.nio.file.Files.copy(is, dest);
            }
            String url = "/files/rooms/" + roomId + "/" + fn;
            MessageItem item = messageService.sendAudioMessage(roomId, userId, url,
                    durationMs == null ? null : durationMs);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(item));
        } catch (Exception ex) {
            throw new com.example.hungdt2.exceptions.BadRequestException("UPLOAD_FAILED", "Failed to save audio file");
        }
    }

    @PostMapping(path = "/image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MessageItem>> postImageMessage(@PathVariable Long roomId,
            Authentication authentication,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        // save file to uploads
        try {
            String baseDir = System.getProperty("user.dir") + "/uploads/rooms/" + roomId;
            java.nio.file.Path dir = java.nio.file.Paths.get(baseDir);
            java.nio.file.Files.createDirectories(dir);
            String fn = java.util.UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            java.nio.file.Path dest = dir.resolve(fn);
            try (java.io.InputStream is = file.getInputStream()) {
                java.nio.file.Files.copy(is, dest);
            }
            String url = "/files/rooms/" + roomId + "/" + fn;
            MessageItem item = messageService.sendImageMessage(roomId, userId, url);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(item));
        } catch (Exception ex) {
            throw new com.example.hungdt2.exceptions.BadRequestException("UPLOAD_FAILED", "Failed to save image file");
        }
    }

    @PutMapping("/read")
    public ApiResponse<Void> markRead(@PathVariable Long roomId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        messageService.markRead(roomId, userId);
        return new ApiResponse<>(null);
    }
}
