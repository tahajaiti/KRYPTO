package com.krypto.trading.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trades")
public class Trade extends BaseEntity {

    @Column(nullable = false)
    private UUID buyOrderId;

    @Column(nullable = false)
    private UUID sellOrderId;

    @Column(nullable = false)
    private UUID coinId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant executedAt;
}
