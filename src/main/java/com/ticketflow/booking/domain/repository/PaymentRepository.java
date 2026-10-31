package com.ticketflow.booking.domain.repository;

import com.ticketflow.booking.domain.entity.Payment;
import com.ticketflow.booking.domain.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
    List<Payment> findByStatus(PaymentStatus status);
}
