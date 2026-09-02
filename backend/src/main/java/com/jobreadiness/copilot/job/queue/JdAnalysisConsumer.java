package com.jobreadiness.copilot.job.queue;

import com.jobreadiness.copilot.job.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class JdAnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(JdAnalysisConsumer.class);

    private final JobService jobService;

    public JdAnalysisConsumer(@Lazy JobService jobService) {
        this.jobService = jobService;
    }

    @RabbitListener(queues = "${app.rabbitmq.jd-queue:jd.analysis.queue}")
    public void handleJdAnalysisMessage(JdAnalysisMessage message) {
        log.info("Received JD analysis task from RabbitMQ for Job ID: {}", message.getJobId());
        try {
            jobService.processJdAnalysis(message.getJobId(), message.getRawJdText(), message.getUserId());
            log.info("Successfully completed JD analysis for Job ID: {}", message.getJobId());
        } catch (Exception ex) {
            log.error("Failed to process JD analysis message for Job ID {}: {}", message.getJobId(), ex.getMessage(), ex);
        }
    }
}
