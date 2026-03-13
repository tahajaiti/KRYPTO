package com.krypto.coin.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MARKET_EXCHANGE = "market.exchange";
    public static final String MARKET_SIMULATED_ROUTING_KEY = "market.simulated";
    public static final String COIN_EXCHANGE = "coin.exchange";
    public static final String COIN_CREATED_ROUTING_KEY = "coin.created";

    @Bean
    public TopicExchange marketExchange() {
        return new TopicExchange(MARKET_EXCHANGE);
    }

    @Bean
    public TopicExchange coinExchange() {
        return new TopicExchange(COIN_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
