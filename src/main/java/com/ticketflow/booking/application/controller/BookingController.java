package com.ticketflow.booking.application.controller;

import com.ticketflow.booking.application.dto.BookingRequest;
import com.ticketflow.booking.application.service.BookingService;
import com.ticketflow.booking.domain.entity.Booking;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody BookingRequestDto dto) {

        BookingRequest request = new BookingRequest(dto.eventId(), dto.seatId(), dto.userId(), idempotencyKey);
        
        Booking booking = bookingService.createBooking(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BookingResponse(booking.getId(), booking.getStatus().name(), booking.getReservedUntil()));
    }

    public record BookingRequestDto(Long eventId, Long seatId, String userId) {}
    public record BookingResponse(Long bookingId, String status, java.time.OffsetDateTime reservedUntil) {}
}
