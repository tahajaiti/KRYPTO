package com.krypto.wallet.entity;

import com.krypto.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallet_balances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_symbol", columnNames = {"wallet_id", "symbol"})
})
public class WalletBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "coin_id")
    private UUID coinId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal balance = BigDecimal.ZERO;
}
