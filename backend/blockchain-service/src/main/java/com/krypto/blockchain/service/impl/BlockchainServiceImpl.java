package com.krypto.blockchain.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.dto.response.ChainValidationResponse;
import com.krypto.blockchain.dto.response.TransactionResponse;
import com.krypto.blockchain.mapper.BlockchainMapper;
import com.krypto.blockchain.model.Block;
import com.krypto.blockchain.model.ChainTransaction;
import com.krypto.blockchain.model.TransactionStatus;
import com.krypto.blockchain.repository.BlockRepository;
import com.krypto.blockchain.repository.ChainTransactionRepository;
import com.krypto.blockchain.service.BlockchainService;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainServiceImpl implements BlockchainService {

    private final BlockchainMapper blockchainMapper;
    private final BlockRepository blockRepository;
    private final ChainTransactionRepository chainTransactionRepository;

    @Value("${blockchain.difficulty:3}")
    private int difficulty;

    @Value("${blockchain.max-transactions-per-block:5}")
    private int maxTransactionsPerBlock;

    @PostConstruct
    @Transactional
    public synchronized void ensureGenesisBlock() {
        if (blockRepository.count() == 0) {
            blockRepository.save(createGenesisBlock());
            log.info("genesis block initialized in database");
        }
    }

    @Override
    @Transactional
    public synchronized TransactionResponse addTransaction(AddTransactionRequest request) {
        validateRequest(request);

        if (request.getSourceEventId() != null && !request.getSourceEventId().isBlank()) {
            ChainTransaction existing = chainTransactionRepository.findTopBySourceEventId(request.getSourceEventId()).orElse(null);
            if (existing != null) return blockchainMapper.toTransactionResponse(existing);
        }

        Instant txTimestamp = request.getEventTimestamp() != null
                ? Instant.ofEpochMilli(request.getEventTimestamp())
                : Instant.now();

        ChainTransaction tx = ChainTransaction.builder()
                .type(request.getType())
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .coinSymbol(request.getCoinSymbol())
                .amount(request.getAmount())
                .fee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO)
                .timestamp(txTimestamp)
                .sourceEventId(request.getSourceEventId())
                .positionInBlock(-1)
                .status(TransactionStatus.PENDING)
                .build();

        tx.setHash(calculateTransactionHash(tx));
        chainTransactionRepository.save(tx);

        if (chainTransactionRepository.countByStatus(TransactionStatus.PENDING) >= Math.max(1, maxTransactionsPerBlock)) {
            mineNextBlock();
        }

        return blockchainMapper.toTransactionResponse(tx);
    }

    @Override
    @Transactional
    public synchronized BlockResponse minePendingTransactions() {
        if (chainTransactionRepository.countByStatus(TransactionStatus.PENDING) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "no pending transactions to mine");
        }

        Block block = mineNextBlock();
        return blockchainMapper.toBlockResponse(block);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public synchronized BlockResponse getLatestBlock() {
        return blockchainMapper.toBlockResponse(getLastBlock());
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public synchronized PageResponse<BlockResponse> getBlocks(Pageable pageable) {
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
            : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "index"));

        Page<Block> pageData = blockRepository.findAll(effectivePageable);
        List<BlockResponse> content = pageData.getContent().stream()
                .map(blockchainMapper::toBlockResponse)
                .toList();

        String sortBy = effectivePageable.getSort().stream().findFirst().map(o -> o.getProperty()).orElse(null);
        String sortDirection = effectivePageable.getSort().stream().findFirst().map(o -> o.getDirection().name()).orElse(null);

        return PageResponse.of(
                content,
                pageData.getNumber(),
                pageData.getSize(),
                (int) pageData.getTotalElements(),
                pageData.getNumberOfElements(),
                sortBy,
                sortDirection
        );
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public synchronized ChainValidationResponse verifyChain() {
        List<Block> chain = blockRepository.findAll(Sort.by(Sort.Direction.ASC, "index"));
        if (chain.isEmpty()) {
            return ChainValidationResponse.builder()
                    .valid(false)
                    .blockCount(0)
                    .message("chain is empty")
                    .build();
        }

        Block genesis = chain.get(0);
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

    @Transactional
    private Block mineNextBlock() {
        Block previous = getLastBlock();
        List<ChainTransaction> pending = chainTransactionRepository.findByStatusOrderByTimestampAscIdAsc(TransactionStatus.PENDING);
        List<ChainTransaction> txs = new ArrayList<>(pending.subList(0, Math.min(maxTransactionsPerBlock, pending.size())));

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

        Block saved = blockRepository.save(mined);

        for (int i = 0; i < txs.size(); i++) {
            ChainTransaction tx = txs.get(i);
            tx.setStatus(TransactionStatus.MINED);
            tx.setPositionInBlock(i);
            tx.setBlock(saved);
        }
        chainTransactionRepository.saveAll(txs);

        saved.setTransactions(txs);
        return mined;
    }

    @Transactional
    private Block getLastBlock() {
        Block last = blockRepository.findTopByOrderByIndexDesc().orElse(null);
        if (last == null) {
            last = blockRepository.save(createGenesisBlock());
        }
        return last;
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
        String amount = tx.getAmount() == null ? "0" : tx.getAmount().stripTrailingZeros().toPlainString();
        String fee = tx.getFee() == null ? "0" : tx.getFee().stripTrailingZeros().toPlainString();
        String timestamp = tx.getTimestamp() == null ? "0" : String.valueOf(tx.getTimestamp().toEpochMilli());

        String payload = tx.getType() + "|" + tx.getFromUserId() + "|" + tx.getToUserId()
                + "|" + tx.getCoinSymbol() + "|" + amount + "|" + fee + "|" + timestamp
                + "|" + tx.getSourceEventId();
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

    private String calculateBlockHash(long index, String previousHash, Instant timestamp, long nonce, List<ChainTransaction> txs) {
        String txDigest = txs.stream()
                .sorted(Comparator.comparingInt(ChainTransaction::getPositionInBlock))
                .map(ChainTransaction::getHash)
                .collect(Collectors.joining(";"));

        String timeStr = timestamp == null ? "0" : String.valueOf(timestamp.toEpochMilli());
        String payload = index + "|" + previousHash + "|" + timeStr + "|" + nonce + "|" + txDigest;
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
