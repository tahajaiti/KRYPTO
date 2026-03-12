package com.krypto.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private UUID id;
    private UUID userId;
    private List<BalanceItemResponse> balances;
    private Instant createdAt;
    private Instant updatedAt;
}
