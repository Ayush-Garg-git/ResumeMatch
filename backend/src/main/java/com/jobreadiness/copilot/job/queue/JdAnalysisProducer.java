package com.jobreadiness.copilot.job.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class JdAnalysisProducer {

    private static final Logger log = LoggerFactory.getLogger(JdAnalysisProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.jd-exchange:jd.analysis.exchange}")
    private String jdExchange;

    @Value("${app.rabbitmq.jd-routing-key:jd.analysis.routingKey}")
    private String jdRoutingKey;

    @Value("${app.rabbitmq.enabled:false}")
    private boolean rabbitEnabled;

    public JdAnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendJdAnalysisTask(JdAnalysisMessage message, Consumer<JdAnalysisMessage> fallbackConsumer) {
        if (rabbitEnabled) {
            try {
                log.info("Publishing JD analysis task to RabbitMQ for Job ID: {}", message.getJobId());
                rabbitTemplate.convertAndSend(jdExchange, jdRoutingKey, message);
                return;
            } catch (Exception ex) {
                log.warn("Failed to publish to RabbitMQ broker: {}. Falling back to async background worker.", ex.getMessage());
            }
        }

        // Fallback: Run in background thread pool immediately
        if (fallbackConsumer != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Executing JD analysis via fallback async worker for Job ID: {}", message.getJobId());
                    fallbackConsumer.accept(message);
                } catch (Exception e) {
                    log.error("Error during fallback JD analysis execution for Job ID {}: {}", message.getJobId(), e.getMessage());
                }
            });
        }
    }
}
