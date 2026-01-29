package com.example.hungdt2.user;

import com.example.hungdt2.common.ApiResponse;
import com.example.hungdt2.user.dto.CreateStatusRequest;
import com.example.hungdt2.user.dto.StatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/statuses")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @PostMapping
    public ResponseEntity<ApiResponse<StatusResponse>> createStatus(
            Authentication auth,
            @Valid @RequestBody CreateStatusRequest req) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(statusService.createStatus(userId, req)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<StatusResponse>>> getUserStatuses(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(statusService.getUserStatuses(userId, pageable)));
    }

    @DeleteMapping("/{statusId}")
    public ResponseEntity<ApiResponse<String>> deleteStatus(
            @PathVariable Long statusId,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        statusService.deleteStatus(statusId, userId);
        return ResponseEntity.ok(new ApiResponse<>("Status deleted"));
    }
}
