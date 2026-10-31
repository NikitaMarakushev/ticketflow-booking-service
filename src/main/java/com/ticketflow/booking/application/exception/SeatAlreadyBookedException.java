package com.ticketflow.booking.application.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(Long seatId) {
        super("Seat with ID " + seatId + " is already booked or reserved");
    }
}
