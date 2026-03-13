package com.krypto.blockchain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.mapper.BlockchainMapper;
import com.krypto.blockchain.model.TransactionType;
import com.krypto.common.dto.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BlockchainServiceImplTest {

    private static final String STATE_FILE = "build/test-data/blockchain-state-unit.json";

    private BlockchainServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        Files.deleteIfExists(Path.of(STATE_FILE));
        service = createService(STATE_FILE);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(STATE_FILE));
    }

    @Test
    void shouldPersistAndRestoreStateAcrossInstanceRestart() {
        AddTransactionRequest request = new AddTransactionRequest(
                TransactionType.TRADE,
                "user-a",
                "user-b",
                "KRYP",
                new BigDecimal("10"),
                BigDecimal.ZERO,
                "evt-1",
                System.currentTimeMillis()
        );

        service.addTransaction(request);
        assertTrue(Files.exists(Path.of(STATE_FILE)));

        BlockchainServiceImpl restoredService = createService(STATE_FILE);
        var latest = restoredService.getLatestBlock();
        assertNotNull(latest);

        var mined = restoredService.minePendingTransactions();
        assertEquals(1L, mined.getIndex());
    }

    @Test
    void shouldDeduplicateBySourceEventId() {
        AddTransactionRequest request = new AddTransactionRequest(
                TransactionType.TRADE,
                "user-a",
                "user-b",
                "KRYP",
                new BigDecimal("15"),
                BigDecimal.ZERO,
                "dup-event-1",
                System.currentTimeMillis()
        );

        var first = service.addTransaction(request);
        var second = service.addTransaction(request);

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void shouldSortBlocksByIndexDescWhenRequested() {
        for (int i = 0; i < 5; i++) {
            service.addTransaction(new AddTransactionRequest(
                    TransactionType.TRADE,
                    "u1",
                    "u2",
                    "KRYP",
                    new BigDecimal("1"),
                    BigDecimal.ZERO,
                    "sort-event-" + i,
                    System.currentTimeMillis() + i
            ));
        }

        PageResponse<?> page = service.getBlocks(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "index")));
        assertFalse(page.getContent().isEmpty());
    }

    private BlockchainServiceImpl createService(String stateFile) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        BlockchainServiceImpl impl = new BlockchainServiceImpl(new BlockchainMapper(), objectMapper);
        ReflectionTestUtils.setField(impl, "difficulty", 1);
        ReflectionTestUtils.setField(impl, "maxTransactionsPerBlock", 5);
        ReflectionTestUtils.setField(impl, "idempotencyWindowSize", 1000);
        ReflectionTestUtils.setField(impl, "persistenceEnabled", true);
        ReflectionTestUtils.setField(impl, "persistenceFilePath", stateFile);
        impl.restoreState();
        return impl;
    }
}
