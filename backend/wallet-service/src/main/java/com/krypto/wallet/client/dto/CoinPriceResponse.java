package com.krypto.wallet.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinPriceResponse {

    private UUID coinId;
    private String symbol;
    private BigDecimal currentPrice;
}
