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
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainServiceImpl implements BlockchainService {

    private final BlockchainMapper blockchainMapper;
    private final ObjectMapper objectMapper;

    private final List<Block> chain = new ArrayList<>();
    private final List<ChainTransaction> pendingTransactions = new ArrayList<>();

    @Value("${blockchain.difficulty:3}")
    private int difficulty;

    @Value("${blockchain.max-transactions-per-block:5}")
    private int maxTransactionsPerBlock;

    @Value("${blockchain.idempotency-window-size:10000}")
    private int idempotencyWindowSize;

    @Value("${blockchain.persistence.enabled:true}")
    private boolean persistenceEnabled;

    @Value("${blockchain.persistence.file-path:data/blockchain/state.json}")
    private String persistenceFilePath;

    private final Map<String, ChainTransaction> recentTransactionsBySourceEventId = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ChainTransaction> eldest) {
            return size() > Math.max(100, idempotencyWindowSize);
        }
    };

    @PostConstruct
    public synchronized void restoreState() {
        if (!persistenceEnabled || !StringUtils.hasText(persistenceFilePath)) {
            return;
        }

        Path path = Paths.get(persistenceFilePath);
        if (!Files.exists(path)) {
            return;
        }

        try {
            BlockchainState state = objectMapper.readValue(path.toFile(), BlockchainState.class);

            chain.clear();
            if (state.chain() != null) {
                chain.addAll(state.chain());
            }

            pendingTransactions.clear();
            if (state.pendingTransactions() != null) {
                pendingTransactions.addAll(state.pendingTransactions());
            }

            recentTransactionsBySourceEventId.clear();
            if (state.recentTransactionsBySourceEventId() != null) {
                recentTransactionsBySourceEventId.putAll(state.recentTransactionsBySourceEventId());
            }

            log.info("restored blockchain state from {} (blocks={}, pending={})", path, chain.size(), pendingTransactions.size());
        } catch (Exception ex) {
            log.error("failed to restore blockchain state from {}", path, ex);
        }
    }

    @Override
    public synchronized TransactionResponse addTransaction(AddTransactionRequest request) {
        validateRequest(request);

        if (request.getSourceEventId() != null && !request.getSourceEventId().isBlank()) {
            ChainTransaction existing = recentTransactionsBySourceEventId.get(request.getSourceEventId());
            if (existing != null) {
                return blockchainMapper.toTransactionResponse(existing);
            }
        }

        Instant txTimestamp = request.getEventTimestamp() != null
                ? Instant.ofEpochMilli(request.getEventTimestamp())
                : Instant.now();

        ChainTransaction tx = ChainTransaction.builder()
                .id(UUID.randomUUID().toString())
                .type(request.getType())
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .coinSymbol(request.getCoinSymbol())
                .amount(request.getAmount())
                .fee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO)
                .timestamp(txTimestamp)
                .build();

        tx.setHash(calculateTransactionHash(tx));
        pendingTransactions.add(tx);

        if (request.getSourceEventId() != null && !request.getSourceEventId().isBlank()) {
            recentTransactionsBySourceEventId.put(request.getSourceEventId(), tx);
        }

        if (pendingTransactions.size() >= Math.max(1, maxTransactionsPerBlock)) {
            mineNextBlock();
        } else {
            persistState();
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
        sortBlocks(snapshot, pageable.getSort());
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
        Block genesis = getGenesisBlock();
        if (genesis.getIndex() != 0 || !"0".equals(genesis.getPreviousHash())) {
            return ChainValidationResponse.builder()
                    .valid(false)
                    .blockCount(chain.size())
                    .message("invalid genesis block")
                    .build();
        }

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

            String prefix = "0".repeat(Math.max(0, difficulty));
            if (!current.getHash().startsWith(prefix)) {
                return ChainValidationResponse.builder()
                        .valid(false)
                        .blockCount(chain.size())
                        .message("proof-of-work mismatch at block " + current.getIndex())
                        .build();
            }

            for (ChainTransaction tx : current.getTransactions()) {
                String txHash = calculateTransactionHash(tx);
                if (!txHash.equals(tx.getHash())) {
                    return ChainValidationResponse.builder()
                            .valid(false)
                            .blockCount(chain.size())
                            .message("transaction hash mismatch in block " + current.getIndex())
                            .build();
                }
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
        persistState();
        return mined;
    }

    private Block getLastBlock() {
        if (chain.isEmpty()) {
            chain.add(createGenesisBlock());
        }
        return chain.get(chain.size() - 1);
    }

    private Block getGenesisBlock() {
        if (chain.isEmpty()) {
            chain.add(createGenesisBlock());
        }
        return chain.get(0);
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

    private void validateRequest(AddTransactionRequest request) {
        if (request.getType() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "type is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "amount must be positive");
        }
        if (request.getFee() != null && request.getFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "fee cannot be negative");
        }
        if (request.getEventTimestamp() != null && request.getEventTimestamp() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "eventTimestamp must be valid epoch millis");
        }
    }

    private void sortBlocks(List<Block> blocks, Sort sort) {
        Sort.Order firstOrder = sort.stream().findFirst().orElse(Sort.Order.desc("index"));
        String property = firstOrder.getProperty().toLowerCase(Locale.ROOT);

        Comparator<Block> comparator = switch (property) {
            case "timestamp" -> Comparator.comparing(Block::getTimestamp);
            case "index" -> Comparator.comparingLong(Block::getIndex);
            default -> Comparator.comparingLong(Block::getIndex);
        };

        if (firstOrder.getDirection() == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }
        blocks.sort(comparator);
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

    private void persistState() {
        if (!persistenceEnabled || !StringUtils.hasText(persistenceFilePath)) {
            return;
        }

        Path path = Paths.get(persistenceFilePath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            BlockchainState state = new BlockchainState(
                    new ArrayList<>(chain),
                    new ArrayList<>(pendingTransactions),
                    new LinkedHashMap<>(recentTransactionsBySourceEventId)
            );
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), state);
        } catch (IOException ex) {
            log.error("failed to persist blockchain state to {}", path, ex);
        }
    }

    private record BlockchainState(
            List<Block> chain,
            List<ChainTransaction> pendingTransactions,
            Map<String, ChainTransaction> recentTransactionsBySourceEventId
    ) {
    }
}
