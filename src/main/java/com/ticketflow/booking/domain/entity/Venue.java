package com.ticketflow.booking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private Integer capacity;
    
    @Version
    private Integer version;
}
