package com.krypto.coin.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_history")
public class PriceHistory extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal volume = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant recordedAt;
}
