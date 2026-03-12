package com.krypto.blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    private long index;
    private String hash;
    private String previousHash;
    private Instant timestamp;
    private long nonce;

    @Builder.Default
    private List<ChainTransaction> transactions = new ArrayList<>();
}
