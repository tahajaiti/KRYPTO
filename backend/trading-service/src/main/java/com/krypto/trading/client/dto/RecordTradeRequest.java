package com.krypto.trading.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordTradeRequest {

    private BigDecimal price;
    private BigDecimal volume;
}
