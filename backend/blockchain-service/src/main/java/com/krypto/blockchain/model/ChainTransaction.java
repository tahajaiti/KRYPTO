package com.krypto.blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainTransaction {

    private String id;
    private TransactionType type;
    private String fromUserId;
    private String toUserId;
    private String coinSymbol;
    private BigDecimal amount;
    private BigDecimal fee;
    private Instant timestamp;
    private String hash;
}
