package com.ticketflow.booking.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.booking.application.dto.BookingRequest;
import com.ticketflow.booking.application.exception.IdempotencyKeyAlreadyUsedException;
import com.ticketflow.booking.application.exception.SeatAlreadyBookedException;
import com.ticketflow.booking.domain.entity.*;
import com.ticketflow.booking.domain.repository.*;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    private static final int RESERVATION_DURATION_MINUTES = 10;

    @Transactional
    public Booking createBooking(BookingRequest request) {
        log.info("Processing booking request for user: {}, seat: {}", request.userId(), request.seatId());

        Span idempotencySpan = tracer.spanBuilder().name("check-idempotency").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(idempotencySpan)) {
            if (idempotencyKeyRepository.findByKey(request.idempotencyKey()).isPresent()) {
                throw new IdempotencyKeyAlreadyUsedException(request.idempotencyKey());
            }
        } finally {
            idempotencySpan.end();
        }

        Span lockSpan = tracer.spanBuilder().name("lock-seat").start();
        Seat seat;
        try (Tracer.SpanInScope scope = tracer.withSpan(lockSpan)) {
            seat = seatRepository.findByIdForUpdate(request.seatId())
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + request.seatId()));
            lockSpan.tag("seat.id", String.valueOf(seat.getId()));
            lockSpan.tag("seat.row", seat.getRowNumber());
            lockSpan.tag("seat.number", seat.getSeatNumber());
        } finally {
            lockSpan.end();
        }

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + request.eventId()));

        List<Booking> activeBookings = bookingRepository.findActiveBookingsBySeatId(
                seat.getId(), 
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );

        if (!activeBookings.isEmpty()) {
            throw new SeatAlreadyBookedException(seat.getId());
        }

        Booking newBooking = Booking.builder()
                .event(event)
                .seat(seat)
                .userId(request.userId())
                .status(BookingStatus.PENDING)
                .reservedUntil(OffsetDateTime.now().plusMinutes(RESERVATION_DURATION_MINUTES))
                .build();

        Booking savedBooking = bookingRepository.save(newBooking);

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .key(request.idempotencyKey())
                .responseStatus(201)
                .responseBody("{\"bookingId\":" + savedBooking.getId() + "}")
                .createdAt(OffsetDateTime.now())
                .build());

        Span outboxSpan = tracer.spanBuilder().name("create-outbox-event").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(outboxSpan)) {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "bookingId", savedBooking.getId(),
                    "eventId", event.getId(),
                    "seatId", seat.getId(),
                    "userId", request.userId(),
                    "status", savedBooking.getStatus().name(),
                    "reservedUntil", savedBooking.getReservedUntil()
            ));

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("BOOKING")
                    .aggregateId(savedBooking.getId())
                    .eventType("BOOKING_CREATED")
                    .payload(payload)
                    .createdAt(OffsetDateTime.now())
                    .processed(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            outboxSpan.tag("outbox.eventId", outboxEvent.getId().toString());
            log.info("Outbox event created for booking: {}", savedBooking.getId());
        } catch (Exception e) {
            outboxSpan.error(e);
            log.error("Failed to serialize outbox payload", e);
            throw new RuntimeException("Failed to create outbox event", e);
        } finally {
            outboxSpan.end();
        }

        return savedBooking;
    }
}
