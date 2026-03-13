package com.krypto.gamification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GAMIFICATION_EXCHANGE = "gamification.exchange";
    public static final String MARKET_EXCHANGE = "market.exchange";
    public static final String COIN_EXCHANGE = "coin.exchange";
    public static final String TRADE_EXECUTED_ROUTING_KEY = "trade.executed";
    public static final String MARKET_SIMULATED_ROUTING_KEY = "market.simulated";
    public static final String COIN_CREATED_ROUTING_KEY = "coin.created";

    public static final String GAMIFICATION_TRADE_QUEUE = "gamification.trade.events.queue";
    public static final String GAMIFICATION_MARKET_QUEUE = "gamification.market.events.queue";
    public static final String GAMIFICATION_COIN_QUEUE = "gamification.coin.events.queue";

    @Bean
    public Queue gamificationTradeQueue() {
        return new Queue(GAMIFICATION_TRADE_QUEUE, true);
    }

    @Bean
    public Queue gamificationMarketQueue() {
        return new Queue(GAMIFICATION_MARKET_QUEUE, true);
    }

    @Bean
    public Queue gamificationCoinQueue() {
        return new Queue(GAMIFICATION_COIN_QUEUE, true);
    }

    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(GAMIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange marketExchange() {
        return new TopicExchange(MARKET_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange coinExchange() {
        return new TopicExchange(COIN_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindTradingEventsToGamification(Queue gamificationTradeQueue, TopicExchange gamificationExchange) {
        return BindingBuilder.bind(gamificationTradeQueue)
                .to(gamificationExchange)
                .with(TRADE_EXECUTED_ROUTING_KEY);
    }

    @Bean
    public Binding bindMarketEventsToGamification(Queue gamificationMarketQueue, TopicExchange marketExchange) {
        return BindingBuilder.bind(gamificationMarketQueue)
                .to(marketExchange)
                .with(MARKET_SIMULATED_ROUTING_KEY);
    }

    @Bean
    public Binding bindCoinEventsToGamification(Queue gamificationCoinQueue, TopicExchange coinExchange) {
        return BindingBuilder.bind(gamificationCoinQueue)
                .to(coinExchange)
                .with(COIN_CREATED_ROUTING_KEY);
    }
}
