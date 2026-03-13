package com.krypto.blockchain.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.model.TransactionType;
import com.krypto.blockchain.service.BlockchainService;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockchainTransactionListenerTest {

    @Mock
    private BlockchainService blockchainService;

    private BlockchainTransactionListener listener;

    @BeforeEach
    void setUp() {
        listener = new BlockchainTransactionListener(blockchainService, new ObjectMapper());
    }

    @Test
    void shouldMapGenericPayloadAndPassToService() {
        Map<String, Object> payload = Map.of(
                "type", "TRADE",
                "fromUserId", "u1",
                "toUserId", "u2",
                "coinSymbol", "KRYP",
                "amount", "5.5",
                "fee", "0.1",
                "tradeId", "trade-123",
                "executedAt", 1000L
        );

        listener.onTransaction(payload, "msg-1");

        ArgumentCaptor<AddTransactionRequest> captor = ArgumentCaptor.forClass(AddTransactionRequest.class);
        verify(blockchainService).addTransaction(captor.capture());

        AddTransactionRequest request = captor.getValue();
        assertEquals(TransactionType.TRADE, request.getType());
        assertEquals(new BigDecimal("5.5"), request.getAmount());
        assertEquals("trade-123", request.getSourceEventId());
        assertEquals(1000L, request.getEventTimestamp());
    }

    @Test
    void shouldPropagateServiceFailureForRetryPipeline() {
        Map<String, Object> payload = Map.of(
                "type", "TRADE",
                "fromUserId", "u1",
                "toUserId", "u2",
                "coinSymbol", "KRYP",
                "amount", "1",
                "fee", "0"
        );

        doThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "boom"))
                .when(blockchainService)
                .addTransaction(any(AddTransactionRequest.class));

        assertThrows(BusinessException.class, () -> listener.onTransaction(payload, "msg-2"));
    }
}
