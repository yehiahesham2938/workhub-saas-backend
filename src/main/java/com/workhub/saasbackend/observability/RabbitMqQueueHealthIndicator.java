package com.workhub.saasbackend.observability;

import java.util.Properties;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.workhub.saasbackend.config.RabbitMQConfig;

@Component("rabbitQueues")
public class RabbitMqQueueHealthIndicator implements HealthIndicator {

    private final RabbitAdmin rabbitAdmin;

    public RabbitMqQueueHealthIndicator(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    public Health health() {
        try {
            Properties jobsQueue = rabbitAdmin.getQueueProperties(RabbitMQConfig.JOBS_QUEUE);
            Properties dlq = rabbitAdmin.getQueueProperties(RabbitMQConfig.JOBS_DLQ);
            int jobsDepth = queueDepth(jobsQueue);
            int dlqDepth = queueDepth(dlq);
            return Health.up()
                    .withDetail("jobsQueue", RabbitMQConfig.JOBS_QUEUE)
                    .withDetail("jobsQueueDepth", jobsDepth)
                    .withDetail("deadLetterQueue", RabbitMQConfig.JOBS_DLQ)
                    .withDetail("deadLetterQueueDepth", dlqDepth)
                    .build();
        } catch (Exception ex) {
            return Health.down().withException(ex).build();
        }
    }

    private int queueDepth(Properties properties) {
        if (properties == null) {
            return -1;
        }
        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count instanceof Integer integer ? integer : -1;
    }
}
