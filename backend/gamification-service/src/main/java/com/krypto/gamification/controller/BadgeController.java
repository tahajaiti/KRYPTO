package com.krypto.gamification.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.gamification.dto.response.BadgeResponse;
import com.krypto.gamification.dto.response.UserBadgeResponse;
import com.krypto.gamification.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getActiveBadges() {
        List<BadgeResponse> badges = badgeService.getActiveBadges();
        return ResponseEntity.ok(ApiResponse.<List<BadgeResponse>>builder()
                .success(true)
                .data(badges)
                .message("Badges retrieved successfully")
                .build());
    }

    @GetMapping("/{badgeId}")
    public ResponseEntity<ApiResponse<BadgeResponse>> getBadgeById(@PathVariable UUID badgeId) {
        BadgeResponse badge = badgeService.getBadgeById(badgeId);
        return ResponseEntity.ok(ApiResponse.<BadgeResponse>builder()
                .success(true)
                .data(badge)
                .message("Badge retrieved successfully")
                .build());
    }

    @GetMapping("/user/my-badges")
    public ResponseEntity<ApiResponse<List<UserBadgeResponse>>> getUserBadges() {
        String userId = AuthorizationUtils.requireUsername();
        List<UserBadgeResponse> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserBadgeResponse>>builder()
                .success(true)
                .data(badges)
                .message("User badges retrieved successfully")
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserBadgeResponse>>> getUserBadgesByUserId(
            @PathVariable String userId) {
        List<UserBadgeResponse> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserBadgeResponse>>builder()
                .success(true)
                .data(badges)
                .message("User badges retrieved successfully")
                .build());
    }
}
