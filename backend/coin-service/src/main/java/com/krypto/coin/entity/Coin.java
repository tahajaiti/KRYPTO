package com.krypto.coin.entity;

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
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coins")
public class Coin extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    private String image;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal initialSupply;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal currentSupply;

    @Column(nullable = false)
    private UUID creatorId;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal creationFee;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal marketCap;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
