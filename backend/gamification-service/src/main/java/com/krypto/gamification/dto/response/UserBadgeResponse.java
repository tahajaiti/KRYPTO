package com.krypto.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadgeResponse {
    private UUID id;
    private UUID badgeId;
    private String badgeName;
    private String icon;
    private Integer points;
    private Long awardedAt;
}
