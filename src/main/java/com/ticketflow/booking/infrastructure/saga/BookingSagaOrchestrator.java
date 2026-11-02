package com.ticketflow.booking.infrastructure.saga;

import com.ticketflow.booking.domain.entity.Booking;
import com.ticketflow.booking.domain.entity.BookingStatus;
import com.ticketflow.booking.domain.repository.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaOrchestrator {

    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-events", groupId = "booking-saga-orchestrator-group")
    @Transactional
    public void handlePaymentResult(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String status = jsonNode.get("status").asText();

            if ("PAYMENT_COMPLETED".equals(status)) {
                bookingRepository.findByStatus(BookingStatus.PENDING).stream().findFirst().ifPresent(booking -> {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                    log.info("Saga: Booking {} confirmed", booking.getId());
                });
            } else if ("PAYMENT_FAILED".equals(status)) {
                bookingRepository.findByStatus(BookingStatus.PENDING).stream().findFirst().ifPresent(booking -> {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    log.info("Saga: Booking {} cancelled due to payment failure", booking.getId());
                });
            }
        } catch (Exception e) {
            log.error("Failed to process saga message: {}", message, e);
        }
    }
}
