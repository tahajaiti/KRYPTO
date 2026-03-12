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
@Table(name = "user_badges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "badgeId"})
})
public class UserBadge extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID badgeId;

    @Column(nullable = false)
    private Long awardedAt;
}
