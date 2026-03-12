package com.krypto.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private UUID fromUserId;
    private UUID toUserId;
    private BigDecimal amount;
    private Instant transferredAt;
}
