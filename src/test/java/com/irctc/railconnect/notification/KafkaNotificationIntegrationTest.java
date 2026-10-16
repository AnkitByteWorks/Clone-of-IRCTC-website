package com.irctc.railconnect.notification;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.booking.domain.PaymentMethod;
import com.irctc.railconnect.booking.dto.BookingResponse;
import com.irctc.railconnect.booking.dto.CreateBookingRequest;
import com.irctc.railconnect.booking.dto.PassengerBookingInput;
import com.irctc.railconnect.booking.repository.BookedPassengerRepository;
import com.irctc.railconnect.booking.repository.BookingRepository;
import com.irctc.railconnect.booking.repository.PaymentTransactionRepository;
import com.irctc.railconnect.booking.service.BookingService;
import com.irctc.railconnect.catalog.config.CatalogDataInitializer;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.dto.QuotaUpdateRequest;
import com.irctc.railconnect.inventory.repository.CoachSeatRepository;
import com.irctc.railconnect.inventory.repository.DailyTrainQuotaRepository;
import com.irctc.railconnect.inventory.service.InventoryService;
import com.irctc.railconnect.notification.document.NotificationLogDocument;
import com.irctc.railconnect.notification.domain.NotificationEventType;
import com.irctc.railconnect.notification.event.BookingNotificationEvent;
import com.irctc.railconnect.notification.repository.NotificationLogRepository;
import com.irctc.railconnect.notification.service.BookingNotificationConsumer;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.UserRepository;
import com.irctc.railconnect.user.security.JwtService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KafkaNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookedPassengerRepository bookedPassengerRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private DailyTrainQuotaRepository dailyTrainQuotaRepository;

    @Autowired
    private CoachSeatRepository coachSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CatalogDataInitializer catalogDataInitializer;

    @Autowired
    private BookingNotificationConsumer bookingNotificationConsumer;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        notificationLogRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        bookedPassengerRepository.deleteAll();
        bookingRepository.deleteAll();
        dailyTrainQuotaRepository.deleteAll();
        coachSeatRepository.deleteAll();
        userRepository.deleteAll();

        Set<String> lockKeys = stringRedisTemplate.keys("inventory:lock:*");
        if (lockKeys != null && !lockKeys.isEmpty()) {
            stringRedisTemplate.delete(lockKeys);
        }

        catalogDataInitializer.seedCatalogIfEmpty();
    }

    private UserEntity createTestUser(String fullName, String email, String phone) {
        UserEntity user = UserEntity.builder()
                .fullName(fullName)
                .email(email)
                .phoneNumber(phone)
                .passwordHash("hashedPass123")
                .role(UserRole.ROLE_PASSENGER)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Test 1: Should publish and consume BOOKING_CONFIRMED event into Mongo notification log and support REST queries")
    void shouldPublishAndConsumeBookingConfirmedEventIntoMongoNotificationLog() throws Exception {
        UserEntity user = createTestUser("Kafka User One", "kafka1@example.com", "9111111101");
        String token = jwtService.generateToken(user);
        LocalDate journeyDate = LocalDate.of(2026, 11, 23); // Monday

        CreateBookingRequest bookingRequest = new CreateBookingRequest(
                "12309",
                "PNBE",
                "NDLS",
                journeyDate,
                "3A",
                QuotaType.GENERAL,
                List.of(new PassengerBookingInput("Rohan Verma", 28, Gender.MALE, BerthPreference.LOWER)),
                PaymentMethod.UPI
        );

        BookingResponse response = bookingService.createBooking(user.getId(), bookingRequest);
        assertThat(response).isNotNull();
        String pnr = response.pnrNumber();

        // Await Kafka consumer to process event and persist in MongoDB
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<NotificationLogDocument> docs = notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(user.getId());
                    assertThat(docs).isNotEmpty();
                    NotificationLogDocument log = docs.get(0);
                    assertThat(log.getEventType()).isEqualTo(NotificationEventType.BOOKING_CONFIRMED);
                    assertThat(log.getPnrNumber()).isEqualTo(pnr);
                    assertThat(log.getRecipientEmail()).isEqualTo("kafka1@example.com");
                    assertThat(log.isReadStatus()).isFalse();
                });

        // Query notifications via REST API
        String notificationId = notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(user.getId()).get(0).getId();

        mockMvc.perform(get("/api/v1/notifications/my")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].pnrNumber").value(pnr))
                .andExpect(jsonPath("$.data[0].eventType").value("BOOKING_CONFIRMED"))
                .andExpect(jsonPath("$.data[0].readStatus").value(false));

        // Mark notification as read via REST API
        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notificationId))
                .andExpect(jsonPath("$.data.readStatus").value(true));

        NotificationLogDocument updated = notificationLogRepository.findById(notificationId).orElseThrow();
        assertThat(updated.isReadStatus()).isTrue();
    }

    @Test
    @DisplayName("Test 2: Should publish PASSENGER_PROMOTED and BOOKING_CANCELLED events on cascade promotion")
    void shouldPublishPassengerPromotedAndBookingCancelledEventsOnCascadePromotion() {
        UserEntity user1 = createTestUser("User One", "user1.promotion@example.com", "9111111102");
        UserEntity user2 = createTestUser("User Two", "user2.promotion@example.com", "9111111103");
        LocalDate journeyDate = LocalDate.of(2026, 11, 23); // Monday

        // Setup 1 CNF seat, 1 RAC slot
        inventoryService.ensureDailyInventoryInitialized("12309", journeyDate);
        inventoryService.upsertQuotaConfig(new QuotaUpdateRequest(
                "12309",
                journeyDate,
                "3A",
                QuotaType.GENERAL,
                1,
                1,
                10
        ));

        // User 1 books -> gets CNF
        CreateBookingRequest req1 = new CreateBookingRequest(
                "12309", "PNBE", "NDLS", journeyDate, "3A", QuotaType.GENERAL,
                List.of(new PassengerBookingInput("Confirmed Passenger", 35, Gender.MALE, BerthPreference.LOWER)),
                PaymentMethod.CREDIT_CARD
        );
        BookingResponse booking1 = bookingService.createBooking(user1.getId(), req1);
        String pnr1 = booking1.pnrNumber();

        // User 2 books -> gets RAC/1
        CreateBookingRequest req2 = new CreateBookingRequest(
                "12309", "PNBE", "NDLS", journeyDate, "3A", QuotaType.GENERAL,
                List.of(new PassengerBookingInput("RAC Passenger", 30, Gender.FEMALE, BerthPreference.SIDE_LOWER)),
                PaymentMethod.UPI
        );
        BookingResponse booking2 = bookingService.createBooking(user2.getId(), req2);
        String pnr2 = booking2.pnrNumber();

        // Wait for initial bookings to be consumed
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(notificationLogRepository.findByPnrNumberOrderByOccurredAtDesc(pnr1)).isNotEmpty();
                    assertThat(notificationLogRepository.findByPnrNumberOrderByOccurredAtDesc(pnr2)).isNotEmpty();
                });

        // User 1 cancels their booking -> User 2 should be promoted from RAC to CNF!
        bookingService.cancelBooking(user1.getId(), pnr1);

        // Verify async notification arrivals in MongoDB via Awaitility
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    // a) User 1 receives BOOKING_CANCELLED notification mentioning refund amount
                    List<NotificationLogDocument> user1Logs = notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(user1.getId());
                    boolean hasCancelledLog = user1Logs.stream().anyMatch(l ->
                            l.getEventType() == NotificationEventType.BOOKING_CANCELLED &&
                            l.getPnrNumber().equals(pnr1) &&
                            l.getMessage().contains("cancelled") &&
                            l.getMessage().contains("Refund amount")
                    );
                    assertThat(hasCancelledLog).isTrue();

                    // b) User 2 receives PASSENGER_PROMOTED notification stating upgrade from RAC to CNF
                    List<NotificationLogDocument> user2Logs = notificationLogRepository.findByRecipientUserIdOrderByOccurredAtDesc(user2.getId());
                    boolean hasPromotedLog = user2Logs.stream().anyMatch(l ->
                            l.getEventType() == NotificationEventType.PASSENGER_PROMOTED &&
                            l.getPnrNumber().equals(pnr2) &&
                            l.getMessage().contains("upgraded from RAC to CNF")
                    );
                    assertThat(hasPromotedLog).isTrue();
                });
    }

    @Test
    @DisplayName("Test 3: Should ensure idempotent consumption when duplicate eventId is received")
    void shouldEnsureIdempotentConsumptionWhenDuplicateEventIdIsReceived() {
        String duplicateEventId = "evt-idempotent-test-unique-uuid-999";

        BookingNotificationEvent event = new BookingNotificationEvent(
                duplicateEventId,
                NotificationEventType.BOOKING_CONFIRMED,
                200L,
                "idempotent@example.com",
                "9876543210",
                "12309",
                "Patna Tejas Rajdhani",
                LocalDate.of(2026, 11, 23),
                "Test Subject",
                "Test Message",
                Instant.now()
        );

        // First execution -> saves to MongoDB
        bookingNotificationConsumer.consumeBookingEvent(event);
        assertThat(notificationLogRepository.existsByEventId(duplicateEventId)).isTrue();
        assertThat(notificationLogRepository.findByPnrNumberOrderByOccurredAtDesc("9876543210")).hasSize(1);

        // Second execution with same eventId -> duplicate detected and skipped
        bookingNotificationConsumer.consumeBookingEvent(event);
        assertThat(notificationLogRepository.findByPnrNumberOrderByOccurredAtDesc("9876543210")).hasSize(1);
    }
}
