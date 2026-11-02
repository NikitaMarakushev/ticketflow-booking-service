package com.ticketflow.booking.infrastructure.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSimulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    @KafkaListener(topics = "booking-events", groupId = "payment-simulator-group")
    public void handleBookingCreated(String message) {
        log.info("Payment Simulator received booking event: {}", message);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isSuccess = random.nextInt(100) < 90;
        
        String eventType = isSuccess ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";

        String responsePayload = String.format("{\"status\": \"%s\", \"message\": \"%s\"}", 
                eventType, isSuccess ? "Payment successful" : "Insufficient funds");

        log.info("Payment Simulator sending {} event", eventType);
        kafkaTemplate.send("booking-events", responsePayload);
    }
}
