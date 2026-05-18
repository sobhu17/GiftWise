package com.giftwise.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling is needed for the outbox relay —
// a @Scheduled method that polls the outbox table every N seconds
// and publishes unpublished events to Kafka.
@SpringBootApplication
@EnableScheduling
public class RecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationServiceApplication.class, args);
    }
}