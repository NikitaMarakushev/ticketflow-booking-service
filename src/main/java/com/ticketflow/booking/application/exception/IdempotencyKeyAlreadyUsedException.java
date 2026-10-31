package com.ticketflow.booking.application.exception;

public class IdempotencyKeyAlreadyUsedException extends RuntimeException {
    public IdempotencyKeyAlreadyUsedException(String key) {
        super("Request with idempotency key " + key + " has already been processed");
    }
}
