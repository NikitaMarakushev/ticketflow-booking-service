package com.ticketflow.booking.application.service;

import com.ticketflow.booking.application.dto.BookingRequest;
import com.ticketflow.booking.application.exception.SeatAlreadyBookedException;
import com.ticketflow.booking.domain.entity.*;
import com.ticketflow.booking.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class BookingServiceConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketflow")
            .withUsername("ticketflow")
            .withPassword("ticketflow");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private VenueRepository venueRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        venueRepository.deleteAll();

        Venue venue = venueRepository.save(Venue.builder()
                .name("Test Arena").address("Test St").city("Test City").capacity(100).version(0).build());

        Event event = eventRepository.save(Event.builder()
                .venue(venue).title("Test Concert").eventDate(OffsetDateTime.now().plusDays(10))
                .status(EventStatus.ON_SALE).version(0).build());

        seatRepository.save(Seat.builder()
                .venue(venue).rowNumber("A").seatNumber("1").category("VIP")
                .price(BigDecimal.valueOf(10000.00)).version(0).build());
    }

    @Test
    void shouldAllowOnlyOneBookingForTheSameSeatUnderHighConcurrency() throws InterruptedException {
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    BookingRequest request = new BookingRequest(
                            1L,
                            1L,
                            "user-" + threadId,
                            UUID.randomUUID().toString()
                    );

                    bookingService.createBooking(request);
                    successCount.incrementAndGet();

                } catch (SeatAlreadyBookedException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executorService.shutdown();

        assertTrue(finished, "The test shouldn't have frozen");

        assertEquals(1, successCount.get(), "Only ONE request must be successful");
        assertEquals(numberOfThreads - 1, conflictCount.get(), "The remaining requests should receive a conflict");
        assertEquals(0, unexpectedErrorCount.get(), "There should be no unexpected errors (deadlock, etc.)");
        assertEquals(1, bookingRepository.count(), "There must be exactly 1 booking record in the database");
    }
}
