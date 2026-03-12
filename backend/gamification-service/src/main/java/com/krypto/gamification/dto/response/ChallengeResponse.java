package com.krypto.gamification.dto.response;

import com.krypto.gamification.entity.Challenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeResponse {
    private UUID id;
    private String name;
    private String description;
    private Challenge.ChallengeType type;
    private Long targetValue;
    private Integer rewardPoints;
    private UUID rewardBadgeId;
    private Challenge.ChallengeMetric metric;
    private Boolean active;
}
