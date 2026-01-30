package com.example.hungdt2.files;

import com.example.hungdt2.room.repository.RoomMemberRepository;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.exceptions.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.security.core.Authentication;
import com.example.hungdt2.auth.JwtService;
import io.jsonwebtoken.JwtException;

@RestController
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final RoomMemberRepository memberRepository;
    private final JwtService jwtService;

    public FileController(RoomMemberRepository memberRepository, JwtService jwtService) {
        this.memberRepository = memberRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/files/rooms/{roomId}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long roomId, @PathVariable String filename,
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestParam(value = "token", required = false) String tokenParam,
            Authentication authentication) {
        Long userId = null;
        if (authentication != null) {
            try {
                userId = (Long) authentication.getPrincipal();
            } catch (Exception ex) {
                userId = null;
            }
        }

        String rawToken = accessToken != null ? accessToken : tokenParam;
        if (rawToken != null && rawToken.startsWith("Bearer "))
            rawToken = rawToken.substring(7);

        // If no Authentication object (e.g., request from audio player without
        // Authorization header), try access_token or token param
        if (userId == null && rawToken != null && !rawToken.isBlank()) {
            try {
                final String masked = rawToken.length() > 8 ? rawToken.substring(0, 8) + "..." : rawToken;
                log.info("FileController: validate token (masked={}) for room={} file={}", masked, roomId, filename);
                var claims = jwtService.parseToken(rawToken);
                String sub = claims.getBody().getSubject();
                userId = Long.valueOf(sub);
            } catch (JwtException ex) {
                log.warn("FileController: invalid token for file request room={}, file={}", roomId, filename);
                throw new ForbiddenException("INVALID_TOKEN", "Invalid token");
            }
        }

        if (userId == null) {
            log.warn("FileController: unauthenticated request for file room={}, file={}", roomId, filename);
            throw new ForbiddenException("NOT_AUTH", "Not authenticated");
        }

        final Long uid = userId;
        var member = memberRepository.findByRoomIdAndUserId(roomId, uid).orElseThrow(() -> {
            log.warn("FileController: not a member: userId={} roomId={} file={}", uid, roomId, filename);
            return new ForbiddenException("NOT_ROOM_MEMBER", "Not a member");
        });
        if (!"APPROVED".equals(member.getStatus())) {
            log.warn("FileController: member not approved userId={} roomId={} file={}", userId, roomId, filename);
            throw new ForbiddenException("NOT_APPROVED", "Not approved");
        }

        try {
            Path file = Paths.get(System.getProperty("user.dir"), "uploads", "rooms", String.valueOf(roomId), filename);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                log.warn("FileController: file not found room={} file={}", roomId, filename);
                throw new NotFoundException("FILE_NOT_FOUND", "File not found");
            }

            // determine content type by extension for better client playback support
            String lc = filename.toLowerCase();
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            if (lc.endsWith(".webm"))
                contentType = "audio/webm";
            else if (lc.endsWith(".m4a") || lc.endsWith(".mp4"))
                contentType = "audio/mp4";
            else if (lc.endsWith(".wav"))
                contentType = "audio/wav";
            else if (lc.endsWith(".ogg"))
                contentType = "audio/ogg";
            else if (lc.endsWith(".3gp") || lc.endsWith(".3gpp"))
                contentType = "audio/3gpp";

            log.info("FileController: serving file room={} file={} userId={} type={}", roomId, filename, userId,
                    contentType);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(resource);
        } catch (MalformedURLException e) {
            log.warn("FileController: malformed url room={} file={} err={}", roomId, filename, e.getMessage());
            throw new NotFoundException("FILE_NOT_FOUND", "File not found");
        }
    }

    @GetMapping("/files/users/{targetUserId}/{filename:.+}")
    public ResponseEntity<Resource> serveUserFile(@PathVariable Long targetUserId, @PathVariable String filename) {
        try {
            Path file = Paths.get(System.getProperty("user.dir"), "uploads", "users", String.valueOf(targetUserId),
                    filename);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new NotFoundException("FILE_NOT_FOUND", "File not found");
            }

            String lc = filename.toLowerCase();
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            if (lc.endsWith(".jpg") || lc.endsWith(".jpeg"))
                contentType = "image/jpeg";
            else if (lc.endsWith(".png"))
                contentType = "image/png";

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(resource);
        } catch (MalformedURLException e) {
            throw new NotFoundException("FILE_NOT_FOUND", "File not found");
        }
    }
}
