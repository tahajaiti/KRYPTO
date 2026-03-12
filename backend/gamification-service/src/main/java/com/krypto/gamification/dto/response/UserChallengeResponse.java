package com.krypto.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallengeResponse {
    private UUID id;
    private UUID challengeId;
    private String challengeName;
    private Long progress;
    private Long targetValue;
    private Boolean completed;
    private LocalDateTime completedAt;
    private Integer rewardPoints;
}
