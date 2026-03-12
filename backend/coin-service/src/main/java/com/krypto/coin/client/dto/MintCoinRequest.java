package com.krypto.coin.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MintCoinRequest {

    private UUID userId;
    private UUID coinId;
    private String symbol;
    private BigDecimal amount;
}
