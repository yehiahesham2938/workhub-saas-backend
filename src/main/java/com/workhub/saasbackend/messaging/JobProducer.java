package com.workhub.saasbackend.messaging;

import java.util.UUID;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.workhub.saasbackend.config.RabbitMQConfig;

@Component
public class JobProducer {

	private final RabbitTemplate rabbitTemplate;
	private final TopicExchange jobsExchange;

	public JobProducer(RabbitTemplate rabbitTemplate, TopicExchange jobsExchange) {
		this.rabbitTemplate = rabbitTemplate;
		this.jobsExchange = jobsExchange;
	}

	public void send(UUID jobId) {
		rabbitTemplate.convertAndSend(jobsExchange.getName(), RabbitMQConfig.JOBS_ROUTING_KEY, jobId.toString());
	}
}

