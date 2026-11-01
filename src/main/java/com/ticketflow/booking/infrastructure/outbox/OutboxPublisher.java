package com.ticketflow.booking.infrastructure.outbox;

import com.ticketflow.booking.domain.entity.OutboxEvent;
import com.ticketflow.booking.domain.repository.OutboxEventRepository;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Tracer tracer;

    private static final String TOPIC_NAME = "booking-events";

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void publishUnprocessedEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnprocessedEvents();
        
        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed outbox events. Publishing to Kafka...", events.size());

        for (OutboxEvent event : events) {
            try {
                var span = tracer.spanBuilder()
                        .name("kafka-publish")
                        .tag("kafka.topic", TOPIC_NAME)
                        .tag("kafka.key", String.valueOf(event.getAggregateId()))
                        .start();
                
                try (var scope = tracer.withSpan(span)) {
                    kafkaTemplate.send(TOPIC_NAME, String.valueOf(event.getAggregateId()), event.getPayload())
                            .whenComplete((result, ex) -> {
                                if (ex == null) {
                                    log.info("Successfully published event {} to Kafka", event.getId());
                                    event.setProcessed(true);
                                    outboxEventRepository.save(event);
                                } else {
                                    log.error("Failed to publish event {} to Kafka", event.getId(), ex);
                                    span.error(ex);
                                }
                            });
                } finally {
                    span.end();
                }
            } catch (Exception e) {
                log.error("Error processing outbox event {}", event.getId(), e);
            }
        }
    }
}
