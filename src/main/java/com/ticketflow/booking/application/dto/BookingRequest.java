package com.ticketflow.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull Long eventId,
    @NotNull Long seatId,
    @NotBlank String userId,
    @NotBlank String idempotencyKey
) {}
