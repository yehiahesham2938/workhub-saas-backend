package com.workhub.saasbackend.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String JOBS_QUEUE = "jobs.queue";
	public static final String JOBS_EXCHANGE = "jobs.exchange";
	public static final String JOBS_ROUTING_KEY = "jobs.created";

	public static final String JOBS_DLX = "jobs.dlx";
	public static final String JOBS_DLQ = "jobs.dlq";
	public static final String JOBS_DLQ_ROUTING_KEY = "jobs.dead";

	@Bean
	public Queue jobsQueue() {
		return QueueBuilder.durable(JOBS_QUEUE)
				.withArgument("x-dead-letter-exchange", JOBS_DLX)
				.withArgument("x-dead-letter-routing-key", JOBS_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public TopicExchange jobsExchange() {
		return new TopicExchange(JOBS_EXCHANGE, true, false);
	}

	@Bean
	public Binding jobsBinding(Queue jobsQueue, TopicExchange jobsExchange) {
		return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(JOBS_ROUTING_KEY);
	}

	@Bean
	public TopicExchange jobsDeadLetterExchange() {
		return new TopicExchange(JOBS_DLX, true, false);
	}

	@Bean
	public Queue jobsDeadLetterQueue() {
		return QueueBuilder.durable(JOBS_DLQ).build();
	}

	@Bean
	public Binding jobsDeadLetterBinding(Queue jobsDeadLetterQueue, TopicExchange jobsDeadLetterExchange) {
		return BindingBuilder.bind(jobsDeadLetterQueue).to(jobsDeadLetterExchange).with(JOBS_DLQ_ROUTING_KEY);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter);
		return template;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
																			   MessageConverter jsonMessageConverter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(jsonMessageConverter);
		factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
		factory.setDefaultRequeueRejected(false);
		return factory;
	}
}
