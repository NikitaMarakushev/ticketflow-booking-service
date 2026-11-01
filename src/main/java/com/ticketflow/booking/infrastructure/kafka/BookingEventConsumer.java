package com.ticketflow.booking.infrastructure.kafka;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final Tracer tracer;

    @KafkaListener(topics = "booking-events", groupId = "booking-service-consumer")
    public void listen(String message) {
        var span = tracer.spanBuilder()
                .name("kafka-consume")
                .tag("kafka.topic", "booking-events")
                .start();
        
        try (var scope = tracer.withSpan(span)) {
            log.info("Kafka Consumer received event: {}", message);
            Thread.sleep(100);
            
            span.tag("processing.status", "success");
        } catch (InterruptedException e) {
            span.error(e);
            Thread.currentThread().interrupt();
        } finally {
            span.end();
        }
    }
}
