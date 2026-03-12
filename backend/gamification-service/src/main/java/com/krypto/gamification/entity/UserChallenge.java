package com.krypto.gamification.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_challenges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "challengeId"})
})
public class UserChallenge extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID challengeId;

    @Column(nullable = false)
    private Long progress;

    @Builder.Default
    @Column(nullable = false)
    private Boolean completed = false;

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Long startedAt;
}
