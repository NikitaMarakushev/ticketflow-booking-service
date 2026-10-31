package com.ticketflow.booking.domain.repository;

import com.ticketflow.booking.domain.entity.Event;
import com.ticketflow.booking.domain.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatus(EventStatus status);
    List<Event> findByEventDateAfter(OffsetDateTime dateTime);
    List<Event> findByVenueId(Long venueId);
}
