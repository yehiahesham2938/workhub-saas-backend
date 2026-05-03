package com.workhub.saasbackend.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.workhub.saasbackend.config.RabbitMQConfig;

@Component
public class JobProducer {

	private static final Logger log = LoggerFactory.getLogger(JobProducer.class);

	private final RabbitTemplate rabbitTemplate;
	private final TopicExchange jobsExchange;

	public JobProducer(RabbitTemplate rabbitTemplate, TopicExchange jobsExchange) {
		this.rabbitTemplate = rabbitTemplate;
		this.jobsExchange = jobsExchange;
	}

	public void send(UUID jobId) {
		try {
			rabbitTemplate.convertAndSend(jobsExchange.getName(), RabbitMQConfig.JOBS_ROUTING_KEY, jobId.toString());
		} catch (AmqpException ex) {
			log.warn("Failed to publish job {} to RabbitMQ, keeping job record saved", jobId, ex);
		}
	}
}

