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
public class CoinCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String coinId;
    private String userId;
    private String symbol;
    private Long initialSupply;
    private Long createdAt;
}
