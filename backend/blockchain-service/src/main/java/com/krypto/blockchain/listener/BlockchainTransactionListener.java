package com.krypto.blockchain.listener;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.config.RabbitMQConfig;
import com.krypto.blockchain.service.BlockchainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainTransactionListener {

    private final BlockchainService blockchainService;

    @RabbitListener(queues = RabbitMQConfig.BLOCKCHAIN_TRANSACTION_QUEUE)
    public void onTransaction(AddTransactionRequest request) {
        try {
            blockchainService.addTransaction(request);
        } catch (Exception ex) {
            log.error("failed to process blockchain transaction event: {}", ex.getMessage(), ex);
        }
    }
}
