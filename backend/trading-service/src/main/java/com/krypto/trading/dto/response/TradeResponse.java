package com.krypto.trading.dto.response;

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
public class TradeResponse {

    private UUID id;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private UUID coinId;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fee;
    private Instant executedAt;
}
