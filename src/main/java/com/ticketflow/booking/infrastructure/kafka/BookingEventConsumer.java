package com.ticketflow.booking.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingEventConsumer {

    @KafkaListener(topics = "booking-events", groupId = "booking-service-consumer")
    public void listen(String message) {
        log.info("Kafka Consumer received event: {}", message);
    }
}
