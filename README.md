# TicketFlow Booking Service

A high-load, fault-tolerant booking microservice designed to handle flash-sale scenarios without data integrity issues, double bookings, or system desynchronization.

## Technology Stack

- **Java 21** with Virtual Threads support
- **Spring Boot 3.3.4** (Web, Data JPA, Kafka, Scheduling, Actuator)
- **PostgreSQL 16** with Flyway migrations
- **Apache Kafka 3.7** for event-driven architecture
- **Redis 7** (infrastructure ready)
- **Docker & Docker Compose** for local development
- **JUnit 5** with multi-threaded integration tests
- **Gradle** as build tool

## Architecture Overview

### Domain Model

The system manages the following entities:
- **Venue**: Physical location where events take place
- **Event**: Scheduled performance/show at a venue
- **Seat**: Individual bookable seat within a venue
- **Booking**: Reservation of a seat for a specific event
- **Payment**: Payment processing for bookings (schema ready)
- **IdempotencyKey**: Tracks processed requests to prevent duplicates
- **OutboxEvent**: Stores domain events for reliable Kafka delivery