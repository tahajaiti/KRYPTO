package com.krypto.coin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinPriceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID coinId;
    private String symbol;
    private BigDecimal currentPrice;
    private boolean active;
}
