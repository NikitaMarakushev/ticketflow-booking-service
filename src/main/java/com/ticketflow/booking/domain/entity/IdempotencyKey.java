package com.ticketflow.booking.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {
    
    @Id
    private String key;
    
    private Integer responseStatus;
    
    @Column(columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
