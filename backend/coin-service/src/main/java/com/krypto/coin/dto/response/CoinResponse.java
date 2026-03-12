package com.krypto.coin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinResponse {

    private UUID id;
    private String name;
    private String symbol;
    private String image;
    private BigDecimal initialSupply;
    private BigDecimal currentSupply;
    private UUID creatorId;
    private BigDecimal creationFee;
    private BigDecimal currentPrice;
    private BigDecimal marketCap;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
