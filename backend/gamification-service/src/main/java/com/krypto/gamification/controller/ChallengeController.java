package com.krypto.gamification.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.gamification.dto.response.ChallengeResponse;
import com.krypto.gamification.dto.response.UserChallengeResponse;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getActiveChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(ApiResponse.<List<ChallengeResponse>>builder()
                .success(true)
                .data(challenges)
                .message("Challenges retrieved successfully")
                .build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getChallengesByType(
            @PathVariable Challenge.ChallengeType type) {
        List<ChallengeResponse> challenges = challengeService.getChallengesByType(type);
        return ResponseEntity.ok(ApiResponse.<List<ChallengeResponse>>builder()
                .success(true)
                .data(challenges)
                .message("Challenges retrieved successfully")
                .build());
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getChallengeById(@PathVariable UUID challengeId) {
        ChallengeResponse challenge = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(ApiResponse.<ChallengeResponse>builder()
                .success(true)
                .data(challenge)
                .message("Challenge retrieved successfully")
                .build());
    }

    @GetMapping("/user/my-challenges")
    public ResponseEntity<ApiResponse<List<UserChallengeResponse>>> getUserChallenges() {
                String userId = AuthorizationUtils.requireUsername();
        List<UserChallengeResponse> challenges = challengeService.getUserChallenges(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserChallengeResponse>>builder()
                .success(true)
                .data(challenges)
                .message("User challenges retrieved successfully")
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserChallengeResponse>>> getUserChallengesByUserId(
            @PathVariable String userId) {
        List<UserChallengeResponse> challenges = challengeService.getUserChallenges(userId);
        return ResponseEntity.ok(ApiResponse.<List<UserChallengeResponse>>builder()
                .success(true)
                .data(challenges)
                .message("User challenges retrieved successfully")
                .build());
    }

    @GetMapping("/{challengeId}/user/my-progress")
    public ResponseEntity<ApiResponse<UserChallengeResponse>> getUserChallengeProgress(
            @PathVariable UUID challengeId) {
                String userId = AuthorizationUtils.requireUsername();
        UserChallengeResponse userChallenge = challengeService.getUserChallenge(userId, challengeId);
        return ResponseEntity.ok(ApiResponse.<UserChallengeResponse>builder()
                .success(true)
                .data(userChallenge)
                .message("User challenge progress retrieved successfully")
                .build());
    }
}
