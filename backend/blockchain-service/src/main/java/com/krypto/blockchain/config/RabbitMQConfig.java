package com.krypto.blockchain.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BLOCKCHAIN_TRANSACTION_QUEUE = "blockchain.transaction.queue";
    public static final String TRADING_EXCHANGE = "trading.exchange";
    public static final String MARKET_EXCHANGE = "market.exchange";
    public static final String TRADE_EXECUTED_ROUTING_KEY = "trade.executed";
    public static final String MARKET_SIMULATED_ROUTING_KEY = "market.simulated";

    @Bean
    public Queue blockchainTransactionQueue() {
        return new Queue(BLOCKCHAIN_TRANSACTION_QUEUE, true);
    }

    @Bean
    public TopicExchange tradingExchange() {
        return new TopicExchange(TRADING_EXCHANGE);
    }

    @Bean
    public TopicExchange marketExchange() {
        return new TopicExchange(MARKET_EXCHANGE);
    }

    @Bean
    public Binding tradeExecutedBinding(Queue blockchainTransactionQueue, TopicExchange tradingExchange) {
        return BindingBuilder.bind(blockchainTransactionQueue).to(tradingExchange).with(TRADE_EXECUTED_ROUTING_KEY);
    }

    @Bean
    public Binding marketSimulatedBinding(Queue blockchainTransactionQueue, TopicExchange marketExchange) {
        return BindingBuilder.bind(blockchainTransactionQueue).to(marketExchange).with(MARKET_SIMULATED_ROUTING_KEY);
    }
}
