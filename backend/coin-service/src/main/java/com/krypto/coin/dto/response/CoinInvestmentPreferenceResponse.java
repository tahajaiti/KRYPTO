package com.krypto.coin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinInvestmentPreferenceResponse {

    private UUID coinId;
    private boolean investing;
}
