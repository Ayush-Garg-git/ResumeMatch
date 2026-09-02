package com.jobreadiness.copilot.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${app.rabbitmq.jd-queue:jd.analysis.queue}")
    private String jdQueue;

    @Value("${app.rabbitmq.jd-exchange:jd.analysis.exchange}")
    private String jdExchange;

    @Value("${app.rabbitmq.jd-routing-key:jd.analysis.routingKey}")
    private String jdRoutingKey;

    @Bean
    public Queue jdAnalysisQueue() {
        return QueueBuilder.durable(jdQueue).build();
    }

    @Bean
    public DirectExchange jdAnalysisExchange() {
        return new DirectExchange(jdExchange);
    }

    @Bean
    public Binding jdAnalysisBinding(Queue jdAnalysisQueue, DirectExchange jdAnalysisExchange) {
        return BindingBuilder.bind(jdAnalysisQueue).to(jdAnalysisExchange).with(jdRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
