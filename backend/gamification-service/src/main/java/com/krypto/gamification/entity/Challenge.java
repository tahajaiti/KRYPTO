package com.krypto.gamification.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "challenges")
public class Challenge extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeType type;

    @Column(nullable = false)
    private Long targetValue;

    @Column(nullable = false)
    private Integer rewardPoints;

    @Column
    private UUID rewardBadgeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeMetric metric;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    public enum ChallengeType {
        DAILY, WEEKLY, SEASONAL
    }

    public enum ChallengeMetric {
        TOTAL_TRADES,
        TOTAL_VOLUME,
        TOTAL_NOTIONAL_VALUE,
        COINS_CREATED,
        WALLET_VALUE
    }
}
