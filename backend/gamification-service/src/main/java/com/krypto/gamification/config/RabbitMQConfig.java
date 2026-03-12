package com.krypto.gamification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRADING_EXCHANGE = "trading.exchange";
    public static final String MARKET_EXCHANGE = "market.exchange";
    public static final String TRADE_EXECUTED_ROUTING_KEY = "trade.executed";
    public static final String MARKET_SIMULATED_ROUTING_KEY = "market.simulated";

    public static final String GAMIFICATION_QUEUE = "gamification.events.queue";

    @Bean
    public Queue gamificationQueue() {
        return new Queue(GAMIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange tradingExchange() {
        return new TopicExchange(TRADING_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange marketExchange() {
        return new TopicExchange(MARKET_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindTradingEventsToGamification(Queue gamificationQueue, TopicExchange tradingExchange) {
        return BindingBuilder.bind(gamificationQueue)
                .to(tradingExchange)
                .with(TRADE_EXECUTED_ROUTING_KEY);
    }

    @Bean
    public Binding bindMarketEventsToGamification(Queue gamificationQueue, TopicExchange marketExchange) {
        return BindingBuilder.bind(gamificationQueue)
                .to(marketExchange)
                .with(MARKET_SIMULATED_ROUTING_KEY);
    }
}
