package org.aoizora.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "auth.exchange";

    public static final String REGISTER_QUEUE = "auth.register.queue";
    public static final String LOGIN_QUEUE = "auth.login.queue";

    public static final String REGISTER_ROUTING_KEY = "auth.register";
    public static final String LOGIN_ROUTING_KEY = "auth.login";

    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue registerQueue() {
        return QueueBuilder.durable(REGISTER_QUEUE).build();
    }

    @Bean
    public Queue loginQueue() {
        return QueueBuilder.durable(LOGIN_QUEUE).build();
    }

    @Bean
    public Binding registerBinding() {
        return BindingBuilder
                .bind(registerQueue())
                .to(authExchange())
                .with(REGISTER_ROUTING_KEY);
    }

    @Bean
    public Binding loginBinding() {
        return BindingBuilder
                .bind(loginQueue())
                .to(authExchange())
                .with(LOGIN_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setExchange(EXCHANGE_NAME);
        return template;
    }
}
