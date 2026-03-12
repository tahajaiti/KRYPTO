package com.krypto.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryResponse {
    private String userId;
    private Integer totalBadges;
    private Integer totalPoints;
    private Long totalTrades;
    private Long totalNotionalValue;
    private Integer rank;
}
