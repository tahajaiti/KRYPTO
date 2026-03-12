package com.krypto.blockchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {

    private long index;
    private String hash;
    private String previousHash;
    private Instant timestamp;
    private long nonce;
    private int transactionCount;
    private List<TransactionResponse> transactions;
}
