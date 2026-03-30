package com.krypto.coin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinPriceHistoryPointResponse {

    private BigDecimal price;
    private BigDecimal volume;
    private Instant recordedAt;
}
