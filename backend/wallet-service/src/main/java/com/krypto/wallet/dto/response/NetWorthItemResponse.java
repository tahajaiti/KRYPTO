package com.krypto.wallet.dto.response;

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
public class NetWorthItemResponse {

    private UUID coinId;
    private String symbol;
    private BigDecimal balance;
    private BigDecimal priceInKryp;
    private BigDecimal valueInKryp;
}
