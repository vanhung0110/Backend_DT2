package com.example.hungdt2.voice;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.voice.dto.VoiceJoinResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/{roomId}/voice/join")
    public ResponseEntity<ApiResponse<VoiceJoinResponse>> join(@PathVariable Long roomId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        VoiceJoinResponse resp = voiceService.joinVoiceRoom(roomId, me);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @PostMapping("/{roomId}/voice/leave")
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable Long roomId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        voiceService.leaveVoiceRoom(roomId, me);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @PostMapping("/{roomId}/voice/kick")
    public ResponseEntity<ApiResponse<Void>> kick(@PathVariable Long roomId, @RequestParam Long userId, Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        voiceService.kickUser(roomId, me, userId);
        return ResponseEntity.ok(new ApiResponse<>(null));
    }

    @GetMapping("/{roomId}/voice/members")
    public ApiResponse<List<com.example.hungdt2.voice.dto.VoiceMemberItem>> members(@PathVariable Long roomId) {
        return new ApiResponse<>(voiceService.listMembers(roomId));
    }
}