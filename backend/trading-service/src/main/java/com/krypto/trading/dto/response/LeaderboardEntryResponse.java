package com.krypto.trading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse implements Serializable {

    private UUID userId;
    private String username;
    private BigDecimal totalVolume;
    private BigDecimal totalNotional;
    private long trades;
}
