package com.krypto.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeExecutedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String userId;
    private String coinId;
    private Long quantity;
    private Long priceInKryp;
    private Long notionalValue;
    private String side;
    private Long executedAt;
}
