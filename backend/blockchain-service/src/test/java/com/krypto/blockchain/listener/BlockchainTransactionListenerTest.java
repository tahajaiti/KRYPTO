package com.krypto.blockchain.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.model.TransactionType;
import com.krypto.blockchain.service.BlockchainService;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class BlockchainTransactionListenerTest {

    @Mock
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BlockchainTransactionListener listener;

    @BeforeEach
    void setUp() {
        listener = new BlockchainTransactionListener(blockchainService, objectMapper);
    }

    @Test
    void shouldMapGenericPayloadAndPassToService() throws Exception {
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

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-1");
        Message message = new Message(objectMapper.writeValueAsBytes(payload), properties);

        listener.onTransaction(message);

        ArgumentCaptor<AddTransactionRequest> captor = ArgumentCaptor.forClass(AddTransactionRequest.class);
        verify(blockchainService).addTransaction(captor.capture());

        AddTransactionRequest request = captor.getValue();
        assertEquals(TransactionType.TRADE, request.getType());
        assertEquals(new BigDecimal("5.5"), request.getAmount());
        assertEquals("trade-123", request.getSourceEventId());
        assertEquals(1000L, request.getEventTimestamp());
    }

    @Test
    void shouldPropagateServiceFailureForRetryPipeline() throws Exception {
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

        MessageProperties properties = new MessageProperties();
        properties.setMessageId("msg-2");
        Message message = new Message(objectMapper.writeValueAsBytes(payload), properties);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> listener.onTransaction(message));
        assertInstanceOf(BusinessException.class, exception.getCause());
    }
}
