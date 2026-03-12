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
public class MarketSimulatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String coinId;
    private Long newPrice;
    private String regime;
    private Double volatilityPercent;
    private Long volumeFactor;
    private Long simulatedAt;
}
