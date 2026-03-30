package com.krypto.blockchain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.mapper.BlockchainMapper;
import com.krypto.blockchain.model.Block;
import com.krypto.blockchain.model.ChainTransaction;
import com.krypto.blockchain.model.TransactionStatus;
import com.krypto.blockchain.model.TransactionType;
import com.krypto.blockchain.repository.BlockRepository;
import com.krypto.blockchain.repository.ChainTransactionRepository;
import com.krypto.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class BlockchainServiceImplTest {

    private BlockchainServiceImpl service;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private ChainTransactionRepository chainTransactionRepository;

    private BlockchainMapper blockchainMapper = new BlockchainMapper();

    @BeforeEach
    void setUp() {
        service = new BlockchainServiceImpl(blockchainMapper, blockRepository, chainTransactionRepository);
        ReflectionTestUtils.setField(service, "difficulty", 1);
        ReflectionTestUtils.setField(service, "maxTransactionsPerBlock", 5);
    }

    @Test
    void shouldAddTransactionAndNotMineWhenUnderThreshold() {
        AddTransactionRequest request = new AddTransactionRequest(
                TransactionType.TRADE, "u1", "u2", "KRYP", new BigDecimal("10"), BigDecimal.ZERO, "event-1", System.currentTimeMillis()
        );

        when(chainTransactionRepository.findTopBySourceEventId(anyString())).thenReturn(Optional.empty());
        when(chainTransactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(1L);

        var response = service.addTransaction(request);

        assertNotNull(response);
        verify(chainTransactionRepository, times(1)).save(any(ChainTransaction.class));
        verify(blockRepository, never()).save(any(Block.class));
    }

    @Test
    void shouldDeduplicateBySourceEventId() {
        String eventId = "dup-event-1";
        AddTransactionRequest request = new AddTransactionRequest(
                TransactionType.TRADE, "u1", "u2", "KRYP", new BigDecimal("15"), BigDecimal.ZERO, eventId, System.currentTimeMillis()
        );

        ChainTransaction existingTx = ChainTransaction.builder()
                .id(java.util.UUID.randomUUID())
                .sourceEventId(eventId)
                .build();

        when(chainTransactionRepository.findTopBySourceEventId(eventId)).thenReturn(Optional.of(existingTx));

        var response = service.addTransaction(request);

        assertNotNull(response);
        assertEquals(existingTx.getId().toString(), response.getId());
        verify(chainTransactionRepository, never()).save(any(ChainTransaction.class));
    }

    @Test
    void shouldSortBlocksByIndexDescWhenRequested() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "index"));
        List<Block> blocks = List.of(
                Block.builder().index(1L).transactions(new ArrayList<>()).build(),
                Block.builder().index(0L).transactions(new ArrayList<>()).build()
        );
        Page<Block> page = new PageImpl<>(blocks, pageable, 2);

        when(blockRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<BlockResponse> result = service.getBlocks(pageable);

        assertFalse(result.getContent().isEmpty());
        assertEquals(2, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getIndex());
    }

    @Test
    void shouldMineWhenThresholdReached() {
        AddTransactionRequest request = new AddTransactionRequest(
                TransactionType.TRADE, "u1", "u2", "KRYP", new BigDecimal("1"), BigDecimal.ZERO, "evt-5", System.currentTimeMillis()
        );

        when(chainTransactionRepository.findTopBySourceEventId(anyString())).thenReturn(Optional.empty());
        when(chainTransactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(5L);
        
        Block lastBlock = Block.builder().index(0L).hash("0").transactions(new ArrayList<>()).build();
        when(blockRepository.findTopByOrderByIndexDesc()).thenReturn(Optional.of(lastBlock));
        
        List<ChainTransaction> pending = new ArrayList<>();
        for(int i=0; i<5; i++) pending.add(ChainTransaction.builder().status(TransactionStatus.PENDING).build());
        when(chainTransactionRepository.findByStatusOrderByTimestampAscIdAsc(TransactionStatus.PENDING)).thenReturn(pending);
        when(blockRepository.save(any(Block.class))).thenAnswer(i -> i.getArgument(0));

        service.addTransaction(request);

        verify(blockRepository, times(1)).save(any(Block.class));
        verify(chainTransactionRepository, times(1)).saveAll(anyList());
    }
}
