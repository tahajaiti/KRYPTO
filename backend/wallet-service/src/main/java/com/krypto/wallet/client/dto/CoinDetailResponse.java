package com.krypto.wallet.client.dto;

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
public class CoinDetailResponse {

    private UUID id;
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal marketCap;
}
