package com.krypto.gamification.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.gamification.dto.response.LeaderboardEntryResponse;
import com.krypto.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class GamificationLeaderboardController {

    private final GamificationService gamificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        List<LeaderboardEntryResponse> leaderboard = gamificationService.getLeaderboard(limit);
        return ResponseEntity.ok(ApiResponse.<List<LeaderboardEntryResponse>>builder()
                .success(true)
                .data(leaderboard)
                .message("Leaderboard retrieved successfully")
                .build());
    }
}
