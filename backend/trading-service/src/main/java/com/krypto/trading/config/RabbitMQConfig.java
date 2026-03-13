package com.krypto.trading.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRADING_EXCHANGE = "trading.exchange";
    public static final String GAMIFICATION_EXCHANGE = "gamification.exchange";
    public static final String TRADE_EXECUTED_ROUTING_KEY = "trade.executed";

    @Bean
    public TopicExchange tradingExchange() {
        return new TopicExchange(TRADING_EXCHANGE);
    }

    @Bean
    public TopicExchange gamificationExchange() {
        return new TopicExchange(GAMIFICATION_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
