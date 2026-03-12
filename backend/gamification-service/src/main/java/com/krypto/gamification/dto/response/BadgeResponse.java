package com.krypto.gamification.dto.response;

import com.krypto.gamification.entity.Badge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponse {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private Integer points;
    private Badge.BadgeTier tier;
    private Boolean active;
}
