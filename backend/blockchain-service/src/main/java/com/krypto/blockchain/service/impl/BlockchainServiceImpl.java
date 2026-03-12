package com.krypto.blockchain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.dto.response.ChainValidationResponse;
import com.krypto.blockchain.dto.response.TransactionResponse;
import com.krypto.blockchain.mapper.BlockchainMapper;
import com.krypto.blockchain.model.Block;
import com.krypto.blockchain.model.ChainTransaction;
import com.krypto.blockchain.service.BlockchainService;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockchainServiceImpl implements BlockchainService {

    private final BlockchainMapper blockchainMapper;
    private final ObjectMapper objectMapper;

    private final List<Block> chain = new ArrayList<>();
    private final List<ChainTransaction> pendingTransactions = new ArrayList<>();

    @Value("${blockchain.difficulty:3}")
    private int difficulty;

    @Value("${blockchain.max-transactions-per-block:5}")
    private int maxTransactionsPerBlock;

    @Override
    public synchronized TransactionResponse addTransaction(AddTransactionRequest request) {
        ChainTransaction tx = ChainTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(request.getType())
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .coinSymbol(request.getCoinSymbol())
                .amount(request.getAmount())
                .fee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO)
                .timestamp(Instant.now())
                .build();

        tx.setHash(calculateTransactionHash(tx));
        pendingTransactions.add(tx);

        if (pendingTransactions.size() >= Math.max(1, maxTransactionsPerBlock)) {
            mineNextBlock();
        }

        return blockchainMapper.toTransactionResponse(tx);
    }

    @Override
    public synchronized BlockResponse minePendingTransactions() {
        if (pendingTransactions.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "no pending transactions to mine");
        }

        Block block = mineNextBlock();
        return blockchainMapper.toBlockResponse(block);
    }

    @Override
    public synchronized BlockResponse getLatestBlock() {
        return blockchainMapper.toBlockResponse(getLastBlock());
    }

    @Override
    public synchronized PageResponse<BlockResponse> getBlocks(Pageable pageable) {
        List<Block> snapshot = new ArrayList<>(chain);
        int totalElements = snapshot.size();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<BlockResponse> content = snapshot.subList(fromIndex, toIndex).stream()
                .map(blockchainMapper::toBlockResponse)
                .toList();

        String sortBy = pageable.getSort().stream().findFirst().map(o -> o.getProperty()).orElse(null);
        String sortDirection = pageable.getSort().stream().findFirst().map(o -> o.getDirection().name()).orElse(null);

        return PageResponse.of(content, page, size, totalElements, content.size(), sortBy, sortDirection);
    }

    @Override
    public synchronized ChainValidationResponse verifyChain() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getPreviousHash().equals(previous.getHash())) {
                return ChainValidationResponse.builder()
                        .valid(false)
                        .blockCount(chain.size())
                        .message("invalid link between block " + previous.getIndex() + " and block " + current.getIndex())
                        .build();
            }

            String recalculatedHash = calculateBlockHash(current.getIndex(), current.getPreviousHash(), current.getTimestamp(), current.getNonce(), current.getTransactions());
            if (!current.getHash().equals(recalculatedHash)) {
                return ChainValidationResponse.builder()
                        .valid(false)
                        .blockCount(chain.size())
                        .message("hash mismatch at block " + current.getIndex())
                        .build();
            }
        }

        return ChainValidationResponse.builder()
                .valid(true)
                .blockCount(chain.size())
                .message("chain is valid")
                .build();
    }

    private Block mineNextBlock() {
        Block previous = getLastBlock();
        List<ChainTransaction> txs = new ArrayList<>(pendingTransactions.subList(0, Math.min(maxTransactionsPerBlock, pendingTransactions.size())));

        long index = previous.getIndex() + 1;
        String previousHash = previous.getHash();
        Instant timestamp = Instant.now();

        long nonce = 0;
        String hash;
        String prefix = "0".repeat(Math.max(0, difficulty));
        do {
            hash = calculateBlockHash(index, previousHash, timestamp, nonce, txs);
            nonce++;
        } while (!hash.startsWith(prefix));

        Block mined = Block.builder()
                .index(index)
                .previousHash(previousHash)
                .timestamp(timestamp)
                .nonce(nonce - 1)
                .transactions(txs)
                .hash(hash)
                .build();

        chain.add(mined);
        pendingTransactions.removeAll(txs);
        return mined;
    }

    private Block getLastBlock() {
        if (chain.isEmpty()) {
            chain.add(createGenesisBlock());
        }
        return chain.get(chain.size() - 1);
    }

    private Block createGenesisBlock() {
        Instant timestamp = Instant.now();
        List<ChainTransaction> txs = List.of();
        String hash = calculateBlockHash(0, "0", timestamp, 0, txs);

        return Block.builder()
                .index(0)
                .previousHash("0")
                .timestamp(timestamp)
                .nonce(0)
                .transactions(new ArrayList<>())
                .hash(hash)
                .build();
    }

    private String calculateTransactionHash(ChainTransaction tx) {
        String payload = tx.getId() + "|" + tx.getType() + "|" + tx.getFromUserId() + "|" + tx.getToUserId()
                + "|" + tx.getCoinSymbol() + "|" + tx.getAmount() + "|" + tx.getFee() + "|" + tx.getTimestamp();
        return sha256(payload);
    }

    private String calculateBlockHash(long index, String previousHash, Instant timestamp, long nonce, List<ChainTransaction> txs) {
        String txJson;
        try {
            txJson = objectMapper.writeValueAsString(txs);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to serialize transactions", e);
        }

        String payload = index + "|" + previousHash + "|" + timestamp + "|" + nonce + "|" + txJson;
        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to hash data", e);
        }
    }
}
