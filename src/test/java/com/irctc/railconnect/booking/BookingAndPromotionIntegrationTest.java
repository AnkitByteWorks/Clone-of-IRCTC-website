package com.irctc.railconnect.booking;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.booking.domain.BookingStatus;
import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.booking.domain.PaymentMethod;
import com.irctc.railconnect.booking.dto.BookingResponse;
import com.irctc.railconnect.booking.dto.CreateBookingRequest;
import com.irctc.railconnect.booking.dto.PassengerBookingInput;
import com.irctc.railconnect.inventory.dto.QuotaUpdateRequest;
import com.irctc.railconnect.booking.repository.BookedPassengerRepository;
import com.irctc.railconnect.booking.repository.BookingRepository;
import com.irctc.railconnect.booking.repository.PaymentTransactionRepository;
import com.irctc.railconnect.booking.service.BookingService;
import com.irctc.railconnect.catalog.config.CatalogDataInitializer;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.repository.CoachSeatRepository;
import com.irctc.railconnect.inventory.repository.DailyTrainQuotaRepository;
import com.irctc.railconnect.inventory.service.InventoryService;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.UserRepository;
import com.irctc.railconnect.user.security.JwtService;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingAndPromotionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private InventoryService inventoryService;

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

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
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
    @DisplayName("Should book confirmed GENERAL and TATKAL tickets with correct fares and public PNR lookup")
    void shouldBookConfirmedGeneralAndTatkalTicketsWithCorrectFaresAndPublicPnrLookup() throws Exception {
        UserEntity userA = createTestUser("User Alpha", "alpha@example.com", "9900000011");
        UserEntity userB = createTestUser("User Beta", "beta@example.com", "9900000012");

        String tokenA = jwtService.generateToken(userA);
        String tokenB = jwtService.generateToken(userB);

        LocalDate journeyDate = LocalDate.of(2026, 11, 23); // A Monday

        // 1. User A books GENERAL on 12309 (PNBE -> NDLS, 3A)
        CreateBookingRequest generalBookingReq = new CreateBookingRequest(
                "12309",
                "PNBE",
                "NDLS",
                journeyDate,
                "3A",
                QuotaType.GENERAL,
                PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Rahul Sharma", 28, Gender.MALE, BerthPreference.LOWER, null, null))
        );

        MvcResult generalResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generalBookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pnrNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalFare").value(1497)) // 998 km * 1.50 = 1497
                .andExpect(jsonPath("$.data.passengers[0].bookingStatus").value("CNF"))
                .andReturn();

        JsonNode generalNode = objectMapper.readTree(generalResult.getResponse().getContentAsString());
        String generalPnr = generalNode.path("data").path("pnrNumber").asText();

        // 2. User B books TATKAL on 12309 (PNBE -> NDLS, 3A)
        CreateBookingRequest tatkalBookingReq = new CreateBookingRequest(
                "12309",
                "PNBE",
                "NDLS",
                journeyDate,
                "3A",
                QuotaType.TATKAL,
                PaymentMethod.CREDIT_CARD,
                List.of(new PassengerBookingInput("Ananya Verma", 26, Gender.FEMALE, BerthPreference.SIDE_LOWER, null, null))
        );

        MvcResult tatkalResult = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tatkalBookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pnrNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalFare").value(1797)) // 1497 + 300 surcharge = 1797
                .andExpect(jsonPath("$.data.passengers[0].bookingStatus").value("CNF"))
                .andReturn();

        JsonNode tatkalNode = objectMapper.readTree(tatkalResult.getResponse().getContentAsString());
        String tatkalPnr = tatkalNode.path("data").path("pnrNumber").asText();

        // 3. Verify public PNR lookup without JWT returns the complete booking details
        mockMvc.perform(get("/api/v1/bookings/pnr/" + generalPnr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pnrNumber").value(generalPnr))
                .andExpect(jsonPath("$.data.trainNumber").value("12309"))
                .andExpect(jsonPath("$.data.passengers", hasSize(1)))
                .andExpect(jsonPath("$.data.passengers[0].passengerName").value("Rahul Sharma"));

        mockMvc.perform(get("/api/v1/bookings/pnr/" + tatkalPnr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pnrNumber").value(tatkalPnr))
                .andExpect(jsonPath("$.data.totalFare").value(1797));
    }

    @Test
    @DisplayName("Should cascade from Confirmed to RAC to Waitlist and auto-promote chain on cancellation")
    void shouldCascadeFromConfirmedToRacToWaitlistAndAutoPromoteChainOnCancellation() throws Exception {
        LocalDate journeyDate = LocalDate.of(2026, 11, 23);
        String trainNumber = "12309";
        String classCode = "3A";

        // Initialize inventory
        inventoryService.ensureDailyInventoryInitialized(trainNumber, journeyDate);

        // Configure a compact quota: 1 CNF, 1 RAC, 2 WL
        inventoryService.upsertQuotaConfig(new QuotaUpdateRequest(
                trainNumber,
                journeyDate,
                classCode,
                QuotaType.GENERAL,
                1,
                1,
                2
        ));

        // Create 4 users
        UserEntity u1 = createTestUser("User One", "user1@example.com", "9900000101");
        UserEntity u2 = createTestUser("User Two", "user2@example.com", "9900000102");
        UserEntity u3 = createTestUser("User Three", "user3@example.com", "9900000103");
        UserEntity u4 = createTestUser("User Four", "user4@example.com", "9900000104");

        String t1 = jwtService.generateToken(u1);
        String t2 = jwtService.generateToken(u2);
        String t3 = jwtService.generateToken(u3);
        String t4 = jwtService.generateToken(u4);

        // 1. User 1 books -> gets CNF
        BookingResponse b1 = bookingService.createBooking(u1.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.GENERAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Passenger 1", 30, Gender.MALE, BerthPreference.LOWER, null, null))
        ));
        assertThat(b1.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b1.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.CNF);
        String u1Seat = b1.passengers().get(0).currentCoach() + "-" + b1.passengers().get(0).currentSeatNumber();

        // 2. User 2 books -> gets RAC/1
        BookingResponse b2 = bookingService.createBooking(u2.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.GENERAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Passenger 2", 28, Gender.FEMALE, BerthPreference.LOWER, null, null))
        ));
        assertThat(b2.bookingStatus()).isEqualTo(BookingStatus.RAC);
        assertThat(b2.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.RAC);
        assertThat(b2.passengers().get(0).currentStatusDetails()).isEqualTo("RAC/1");

        // 3. User 3 books -> gets WL/1
        BookingResponse b3 = bookingService.createBooking(u3.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.GENERAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Passenger 3", 35, Gender.MALE, BerthPreference.NO_PREFERENCE, null, null))
        ));
        assertThat(b3.bookingStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(b3.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.WL);
        assertThat(b3.passengers().get(0).currentStatusDetails()).isEqualTo("WL/1");

        // 4. User 4 books -> gets WL/2
        BookingResponse b4 = bookingService.createBooking(u4.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.GENERAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Passenger 4", 42, Gender.MALE, BerthPreference.NO_PREFERENCE, null, null))
        ));
        assertThat(b4.bookingStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(b4.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.WL);
        assertThat(b4.passengers().get(0).currentStatusDetails()).isEqualTo("WL/2");

        // 5. User 1 cancels CNF ticket
        mockMvc.perform(post("/api/v1/bookings/" + b1.pnrNumber() + "/cancel")
                        .header("Authorization", "Bearer " + t1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(1122.75)); // 1497 * 0.75 = 1122.75

        // 6. Verify cascade promotion chain:
        // User 2: RAC/1 -> promoted to CNF with User 1's berth!
        BookingResponse u2After = bookingService.getBookingByPnr(b2.pnrNumber());
        assertThat(u2After.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(u2After.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.CNF);
        String u2Seat = u2After.passengers().get(0).currentCoach() + "-" + u2After.passengers().get(0).currentSeatNumber();
        assertThat(u2Seat).isEqualTo(u1Seat);

        // User 3: WL/1 -> promoted to RAC/1!
        BookingResponse u3After = bookingService.getBookingByPnr(b3.pnrNumber());
        assertThat(u3After.bookingStatus()).isEqualTo(BookingStatus.RAC);
        assertThat(u3After.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.RAC);
        assertThat(u3After.passengers().get(0).currentStatusDetails()).isEqualTo("RAC/1");

        // User 4: WL/2 -> promoted to WL/1!
        BookingResponse u4After = bookingService.getBookingByPnr(b4.pnrNumber());
        assertThat(u4After.bookingStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(u4After.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.WL);
        assertThat(u4After.passengers().get(0).currentStatusDetails()).isEqualTo("WL/1");
    }

    @Test
    @DisplayName("Should promote Tatkal waitlist directly to confirmed and enforce 4 passenger Tatkal limit")
    void shouldPromoteTatkalWaitlistDirectlyToConfirmedAndEnforce4PassengerTatkalLimit() throws Exception {
        LocalDate journeyDate = LocalDate.of(2026, 11, 23);
        String trainNumber = "12309";
        String classCode = "3A";

        inventoryService.ensureDailyInventoryInitialized(trainNumber, journeyDate);

        // Configure Tatkal quota: 1 CNF, 0 RAC, 2 WL
        inventoryService.upsertQuotaConfig(new QuotaUpdateRequest(
                trainNumber,
                journeyDate,
                classCode,
                QuotaType.TATKAL,
                1,
                0,
                2
        ));

        UserEntity user1 = createTestUser("Tatkal User 1", "tatkal1@example.com", "9900000201");
        UserEntity user2 = createTestUser("Tatkal User 2", "tatkal2@example.com", "9900000202");
        String t1 = jwtService.generateToken(user1);
        String t2 = jwtService.generateToken(user2);

        // 1. User 1 books -> gets CNF
        BookingResponse b1 = bookingService.createBooking(user1.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.TATKAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Tatkal Passenger 1", 30, Gender.MALE, BerthPreference.LOWER, null, null))
        ));
        assertThat(b1.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b1.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.CNF);

        // 2. User 2 books -> gets WL/1 (directly to WL since totalRacSlots = 0 in Tatkal)
        BookingResponse b2 = bookingService.createBooking(user2.getId(), new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.TATKAL, PaymentMethod.UPI,
                List.of(new PassengerBookingInput("Tatkal Passenger 2", 28, Gender.FEMALE, BerthPreference.UPPER, null, null))
        ));
        assertThat(b2.bookingStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(b2.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.WL);
        assertThat(b2.passengers().get(0).currentStatusDetails()).isEqualTo("WL/1");

        // 3. User 1 cancels CNF ticket
        mockMvc.perform(post("/api/v1/bookings/" + b1.pnrNumber() + "/cancel")
                        .header("Authorization", "Bearer " + t1))
                .andExpect(status().isOk());

        // 4. Verify User 2 was promoted DIRECTLY from WL/1 to CNF!
        BookingResponse u2After = bookingService.getBookingByPnr(b2.pnrNumber());
        assertThat(u2After.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(u2After.passengers().get(0).currentStatus()).isEqualTo(PassengerTicketStatus.CNF);

        // 5. Verify 4-passenger limit on Tatkal: attempting to book 5 passengers in TATKAL is rejected
        List<PassengerBookingInput> fivePassengers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fivePassengers.add(new PassengerBookingInput("P" + i, 20 + i, Gender.MALE, BerthPreference.NO_PREFERENCE, null, null));
        }

        CreateBookingRequest fivePassengerTatkalReq = new CreateBookingRequest(
                trainNumber, "PNBE", "NDLS", journeyDate, classCode, QuotaType.TATKAL, PaymentMethod.UPI, fivePassengers
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + t2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fivePassengerTatkalReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }
}
