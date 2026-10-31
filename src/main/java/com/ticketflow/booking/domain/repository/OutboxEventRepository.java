package com.ticketflow.booking.domain.repository;

import com.ticketflow.booking.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents();
}
