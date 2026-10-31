package com.ticketflow.booking.domain.repository;

import com.ticketflow.booking.domain.entity.Booking;
import com.ticketflow.booking.domain.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(String userId);
    List<Booking> findByEventId(Long eventId);
    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.seat.id = :seatId AND b.status IN :statuses")
    List<Booking> findActiveBookingsBySeatId(@Param("seatId") Long seatId, 
                                             @Param("statuses") List<BookingStatus> statuses);
}
