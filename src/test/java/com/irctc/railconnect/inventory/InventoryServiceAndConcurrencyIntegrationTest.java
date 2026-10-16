package com.irctc.railconnect.inventory;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.catalog.config.CatalogDataInitializer;
import com.irctc.railconnect.common.exception.BusinessRuleException;
import com.irctc.railconnect.common.exception.ResourceConflictException;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import com.irctc.railconnect.inventory.dto.QuotaUpdateRequest;
import com.irctc.railconnect.inventory.dto.SeatLockRequest;
import com.irctc.railconnect.inventory.dto.SeatLockResponse;
import com.irctc.railconnect.inventory.dto.TrainAvailabilityResponse;
import com.irctc.railconnect.inventory.entity.CoachSeatEntity;
import com.irctc.railconnect.inventory.entity.DailyTrainQuotaEntity;
import com.irctc.railconnect.inventory.repository.CoachSeatRepository;
import com.irctc.railconnect.inventory.repository.DailyTrainQuotaRepository;
import com.irctc.railconnect.inventory.service.InventoryService;
import com.irctc.railconnect.inventory.service.SeatLockService;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.UserRepository;
import com.irctc.railconnect.user.security.JwtService;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InventoryServiceAndConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SeatLockService seatLockService;

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
        dailyTrainQuotaRepository.deleteAll();
        coachSeatRepository.deleteAll();
        userRepository.deleteAll();

        Set<String> lockKeys = stringRedisTemplate.keys("inventory:lock:*");
        if (lockKeys != null && !lockKeys.isEmpty()) {
            stringRedisTemplate.delete(lockKeys);
        }

        // Seed catalog stations and trains from JSON if empty
        catalogDataInitializer.seedCatalogIfEmpty();
    }

    @Test
    @DisplayName("Should initialize inventory from Mongo blueprint and external YAML properties without hardcoded values")
    void shouldInitializeInventoryFromMongoBlueprintAndExternalYamlProperties() {
        String trainNumber = "12309";
        LocalDate journeyDate = LocalDate.of(2026, 11, 20);

        TrainAvailabilityResponse response = inventoryService.getTrainAvailability(trainNumber, journeyDate);
        assertThat(response).isNotNull();
        assertThat(response.trainNumber()).isEqualTo("12309");
        assertThat(response.quotas()).isNotEmpty();

        // 12309 has 1A (1 coach x 8 seats = 8), 2A (1 coach x 8 seats = 8), 3A (2 coaches x 8 seats = 16)
        // Check 3A GENERAL (16 * 0.80 = 13 seats) and TATKAL (16 - 13 = 3 seats)
        DailyTrainQuotaEntity quota3aGeneral = dailyTrainQuotaRepository
                .findByTrainNumberAndJourneyDateAndClassCodeAndQuotaType(trainNumber, journeyDate, "3A", QuotaType.GENERAL)
                .orElseThrow();
        assertThat(quota3aGeneral.getTotalConfirmedSeats()).isEqualTo(13);
        assertThat(quota3aGeneral.getAvailableConfirmedSeats()).isEqualTo(13);
        assertThat(quota3aGeneral.getTotalRacSlots()).isEqualTo(4);
        assertThat(quota3aGeneral.getMaxWaitlistLimit()).isEqualTo(15);

        DailyTrainQuotaEntity quota3aTatkal = dailyTrainQuotaRepository
                .findByTrainNumberAndJourneyDateAndClassCodeAndQuotaType(trainNumber, journeyDate, "3A", QuotaType.TATKAL)
                .orElseThrow();
        assertThat(quota3aTatkal.getTotalConfirmedSeats()).isEqualTo(3);
        assertThat(quota3aTatkal.getAvailableConfirmedSeats()).isEqualTo(3);
        assertThat(quota3aTatkal.getTotalRacSlots()).isEqualTo(0);
        assertThat(quota3aTatkal.getMaxWaitlistLimit()).isEqualTo(10);

        // Verify physical seats in MySQL:
        // 3A: 2 coaches (B1 and B2) x 8 seats = 16 seats
        List<CoachSeatEntity> seats3A = coachSeatRepository
                .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(trainNumber, journeyDate, "3A");
        assertThat(seats3A).hasSize(16);
        assertThat(seats3A.stream().filter(s -> "B1".equals(s.getCoachNumber())).count()).isEqualTo(8);
        assertThat(seats3A.stream().filter(s -> "B2".equals(s.getCoachNumber())).count()).isEqualTo(8);

        // Total physical seats for train = 8 (1A) + 8 (2A) + 16 (3A) = 32
        List<CoachSeatEntity> allSeats = coachSeatRepository.findAll();
        assertThat(allSeats).hasSize(32);
    }

    @Test
    @DisplayName("Should allow only one winner when 20 virtual threads concurrently lock the same seat")
    void shouldAllowOnlyOneWinnerWhen20VirtualThreadsConcurrentlyLockSameSeat() throws InterruptedException {
        String trainNumber = "12309";
        LocalDate journeyDate = LocalDate.of(2026, 11, 20);
        inventoryService.ensureDailyInventoryInitialized(trainNumber, journeyDate);

        SeatLockRequest lockRequest = new SeatLockRequest(trainNumber, journeyDate, "3A", "B1", 1);

        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<String> winnerEmail = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= threadCount; i++) {
                final String userEmail = "passenger" + i + "@example.com";
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        SeatLockResponse resp = inventoryService.lockSpecificSeat(lockRequest, userEmail);
                        successCount.incrementAndGet();
                        winnerEmail.set(resp.lockedByEmail());
                    } catch (ResourceConflictException ex) {
                        conflictCount.incrementAndGet();
                    } catch (Exception ex) {
                        // unexpected
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Unleash all 20 virtual threads simultaneously
            startLatch.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(19);
        assertThat(winnerEmail.get()).isNotNull();

        // Verify Redis lock belongs strictly to the winning virtual thread
        Optional<String> lockOwner = seatLockService.getLockOwner(trainNumber, journeyDate, "B1", 1);
        assertThat(lockOwner).isPresent().contains(winnerEmail.get());

        // Verify MySQL seat status is LOCKED by the winning user
        CoachSeatEntity lockedSeat = coachSeatRepository
                .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(trainNumber, journeyDate, "3A")
                .stream()
                .filter(s -> "B1".equals(s.getCoachNumber()) && s.getSeatNumber() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(lockedSeat.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(lockedSeat.getLockedByUserEmail()).isEqualTo(winnerEmail.get());
    }

    @Test
    @DisplayName("Should prevent other user from unlocking seat and allow dynamic quota update via API")
    void shouldPreventOtherUserFromUnlockingSeatAndAllowDynamicQuotaUpdateViaApi() throws Exception {
        String trainNumber = "12309";
        LocalDate journeyDate = LocalDate.of(2026, 11, 20);
        inventoryService.ensureDailyInventoryInitialized(trainNumber, journeyDate);

        // 1. Create User A and User B
        UserEntity userA = UserEntity.builder()
                .fullName("User Alpha")
                .email("alpha@example.com")
                .phoneNumber("9900000001")
                .passwordHash("hash")
                .role(UserRole.ROLE_PASSENGER)
                .active(true)
                .build();
        userRepository.save(userA);

        UserEntity userB = UserEntity.builder()
                .fullName("User Beta")
                .email("beta@example.com")
                .phoneNumber("9900000002")
                .passwordHash("hash")
                .role(UserRole.ROLE_PASSENGER)
                .active(true)
                .build();
        userRepository.save(userB);

        String tokenA = jwtService.generateToken(userA);
        String tokenB = jwtService.generateToken(userB);

        // 2. Public GET train availability works without JWT
        mockMvc.perform(get("/api/v1/inventory/trains/12309/availability")
                        .param("date", "2026-11-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12309"));

        // 3. User A locks seat B1-1
        SeatLockRequest lockRequest = new SeatLockRequest(trainNumber, journeyDate, "3A", "B1", 1);
        mockMvc.perform(post("/api/v1/inventory/seats/lock")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lockedByEmail").value("alpha@example.com"));

        // 4. User B attempts to unlock B1-1 -> should be rejected with 400 BusinessRuleException
        mockMvc.perform(post("/api/v1/inventory/seats/unlock")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        // Verify Redis lock is STILL held by User A
        assertThat(seatLockService.getLockOwner(trainNumber, journeyDate, "B1", 1))
                .isPresent().contains("alpha@example.com");

        // 5. User A unlocks seat B1-1 -> succeeds
        mockMvc.perform(post("/api/v1/inventory/seats/unlock")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify Redis lock is released
        assertThat(seatLockService.getLockOwner(trainNumber, journeyDate, "B1", 1)).isEmpty();

        // 6. Dynamic Quota update via API PUT /api/v1/inventory/quotas
        QuotaUpdateRequest updateRequest = new QuotaUpdateRequest(
                trainNumber,
                journeyDate,
                "3A",
                QuotaType.GENERAL,
                25,
                6,
                30
        );

        mockMvc.perform(put("/api/v1/inventory/quotas")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableConfirmedSeats").value(13)) // min(13, 25)
                .andExpect(jsonPath("$.data.availableRacSlots").value(4))        // min(4, 6)
                .andExpect(jsonPath("$.data.maxWaitlistLimit").value(30));
    }
}
