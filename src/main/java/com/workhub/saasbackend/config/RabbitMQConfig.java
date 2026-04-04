package com.workhub.saasbackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String JOBS_QUEUE = "jobs.queue";
	public static final String JOBS_EXCHANGE = "jobs.exchange";
	public static final String JOBS_ROUTING_KEY = "jobs.created";

	@Bean
	public Queue jobsQueue() {
		return new Queue(JOBS_QUEUE, true);
	}

	@Bean
	public TopicExchange jobsExchange() {
		return new TopicExchange(JOBS_EXCHANGE, true, false);
	}

	@Bean
	public Binding jobsBinding(Queue jobsQueue, TopicExchange jobsExchange) {
		return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(JOBS_ROUTING_KEY);
	}
}

