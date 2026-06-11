package com.microservices.cartservice.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CartEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCartEvent(String message) {
        kafkaTemplate.send("cart-events", message);
        System.out.println("Kafka message sent: " + message);
    }
}