package com.workhub.saasbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class SaasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasBackendApplication.class, args);
    }
}
