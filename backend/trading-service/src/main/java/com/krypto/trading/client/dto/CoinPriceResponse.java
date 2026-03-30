package com.krypto.trading.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinPriceResponse {

    private UUID coinId;
    private String symbol;
    private BigDecimal currentPrice;
    private boolean active;
}
