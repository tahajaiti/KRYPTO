package com.krypto.blockchain.mapper;

import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.dto.response.TransactionResponse;
import com.krypto.blockchain.model.Block;
import com.krypto.blockchain.model.ChainTransaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlockchainMapper {

    public BlockResponse toBlockResponse(Block block) {
        List<TransactionResponse> txs = block.getTransactions().stream().map(this::toTransactionResponse).toList();
        return BlockResponse.builder()
                .index(block.getIndex())
                .hash(block.getHash())
                .previousHash(block.getPreviousHash())
                .timestamp(block.getTimestamp())
                .nonce(block.getNonce())
                .transactionCount(txs.size())
                .transactions(txs)
                .build();
    }

    public TransactionResponse toTransactionResponse(ChainTransaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId() != null ? tx.getId().toString() : null)
                .type(tx.getType())
                .fromUserId(tx.getFromUserId())
                .toUserId(tx.getToUserId())
                .coinSymbol(tx.getCoinSymbol())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .timestamp(tx.getTimestamp())
                .hash(tx.getHash())
                .build();
    }
}
