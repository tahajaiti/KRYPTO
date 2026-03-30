package com.krypto.blockchain.listener;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.config.RabbitMQConfig;
import com.krypto.blockchain.model.TransactionType;
import com.krypto.blockchain.service.BlockchainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainTransactionListener {

    private final BlockchainService blockchainService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.BLOCKCHAIN_TRANSACTION_QUEUE)
    public void onTransaction(Message message) {
        try {
            Map<String, Object> map = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            AddTransactionRequest request = mapToRequest(map, message.getMessageProperties().getMessageId());
            blockchainService.addTransaction(request);
        } catch (Exception e) {
            log.error("failed to process blockchain transaction: {}", e.getMessage(), e);
            throw new RuntimeException("failed to process blockchain transaction", e);
        }
    }

    private AddTransactionRequest mapToRequest(Map<String, Object> map, String messageId) {
        AddTransactionRequest request = new AddTransactionRequest();
        request.setType(resolveType(map));
        request.setFromUserId(asString(map.get("fromUserId")));
        request.setToUserId(asString(map.get("toUserId")));
        request.setCoinSymbol(asString(map.get("coinSymbol")));
        request.setAmount(asBigDecimal(map.get("amount")));
        request.setFee(asBigDecimalOrZero(map.get("fee")));

        String sourceEventId = firstNonBlank(
                asString(map.get("sourceEventId")),
                asString(map.get("tradeId")),
                messageId
        );
        request.setSourceEventId(sourceEventId);

        Long eventTimestamp = asLong(map.get("eventTimestamp"));
        if (eventTimestamp == null) {
            eventTimestamp = asLong(map.get("executedAt"));
        }
        if (eventTimestamp == null) {
            eventTimestamp = asLong(map.get("simulatedAt"));
        }
        request.setEventTimestamp(eventTimestamp);

        return request;
    }

    private TransactionType resolveType(Map<String, Object> map) {
        String raw = asString(map.get("type"));
        if (raw == null || raw.isBlank()) {
            return TransactionType.TRADE;
        }
        try {
            return TransactionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TransactionType.TRADE;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal asBigDecimalOrZero(Object value) {
        BigDecimal parsed = asBigDecimal(value);
        return parsed != null ? parsed : BigDecimal.ZERO;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
