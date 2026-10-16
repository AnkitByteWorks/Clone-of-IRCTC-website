package com.irctc.railconnect.catalog.service;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.catalog.document.CoachClassConfig;
import com.irctc.railconnect.catalog.document.RouteStop;
import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.document.TrainType;
import com.irctc.railconnect.catalog.dto.ClassFareDto;
import com.irctc.railconnect.catalog.dto.TrainScheduleRequest;
import com.irctc.railconnect.catalog.dto.TrainSearchResultDto;
import com.irctc.railconnect.catalog.repository.StationRepository;
import com.irctc.railconnect.catalog.repository.TrainScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogServiceIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
        trainScheduleRepository.deleteAll();

        Set<String> keys = stringRedisTemplate.keys("catalog:trains:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private TrainScheduleDocument createTejasRajdhani(Set<DayOfWeek> runningDays) {
        return TrainScheduleDocument.builder()
                .trainNumber("12309")
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .trainType(TrainType.RAJDHANI)
                .sourceStationCode("PNBE")
                .destinationStationCode("NDLS")
                .runningDays(runningDays)
                .routeStops(List.of(
                        RouteStop.builder()
                                .stopSequence(1)
                                .stationCode("PNBE")
                                .stationName("Patna Junction")
                                .arrivalTime("SOURCE")
                                .departureTime("19:10")
                                .haltMinutes(0)
                                .dayOffset(0)
                                .distanceFromSourceKm(0)
                                .platformNumber("1")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(2)
                                .stationCode("DDU")
                                .stationName("Pt. Deen Dayal Upadhyaya Jn")
                                .arrivalTime("22:10")
                                .departureTime("22:20")
                                .haltMinutes(10)
                                .dayOffset(0)
                                .distanceFromSourceKm(212)
                                .platformNumber("4")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(3)
                                .stationCode("PRYJ")
                                .stationName("Prayagraj Junction")
                                .arrivalTime("00:10")
                                .departureTime("00:12")
                                .haltMinutes(2)
                                .dayOffset(1)
                                .distanceFromSourceKm(364)
                                .platformNumber("1")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(4)
                                .stationCode("CNB")
                                .stationName("Kanpur Central")
                                .arrivalTime("02:15")
                                .departureTime("02:20")
                                .haltMinutes(5)
                                .dayOffset(1)
                                .distanceFromSourceKm(558)
                                .platformNumber("1")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(5)
                                .stationCode("NDLS")
                                .stationName("New Delhi")
                                .arrivalTime("07:40")
                                .departureTime("DEST")
                                .haltMinutes(0)
                                .dayOffset(1)
                                .distanceFromSourceKm(998)
                                .platformNumber("12")
                                .build()
                ))
                .coachClasses(List.of(
                        CoachClassConfig.builder()
                                .classCode("1A")
                                .totalCoaches(2)
                                .seatsPerCoach(24)
                                .baseFarePerKm(new BigDecimal("3.50"))
                                .tatkalSurcharge(new BigDecimal("500.00"))
                                .build(),
                        CoachClassConfig.builder()
                                .classCode("2A")
                                .totalCoaches(5)
                                .seatsPerCoach(54)
                                .baseFarePerKm(new BigDecimal("2.20"))
                                .tatkalSurcharge(new BigDecimal("400.00"))
                                .build(),
                        CoachClassConfig.builder()
                                .classCode("3A")
                                .totalCoaches(11)
                                .seatsPerCoach(72)
                                .baseFarePerKm(new BigDecimal("1.50"))
                                .tatkalSurcharge(new BigDecimal("300.00"))
                                .build()
                ))
                .active(true)
                .build();
    }

    private TrainScheduleDocument createReverseRajdhani(Set<DayOfWeek> runningDays) {
        return TrainScheduleDocument.builder()
                .trainNumber("12310")
                .trainName("New Delhi Patna Tejas Rajdhani Express")
                .trainType(TrainType.RAJDHANI)
                .sourceStationCode("NDLS")
                .destinationStationCode("PNBE")
                .runningDays(runningDays)
                .routeStops(List.of(
                        RouteStop.builder()
                                .stopSequence(1)
                                .stationCode("NDLS")
                                .stationName("New Delhi")
                                .arrivalTime("SOURCE")
                                .departureTime("17:10")
                                .haltMinutes(0)
                                .dayOffset(0)
                                .distanceFromSourceKm(0)
                                .platformNumber("12")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(2)
                                .stationCode("CNB")
                                .stationName("Kanpur Central")
                                .arrivalTime("21:52")
                                .departureTime("21:57")
                                .haltMinutes(5)
                                .dayOffset(0)
                                .distanceFromSourceKm(440)
                                .platformNumber("1")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(3)
                                .stationCode("PRYJ")
                                .stationName("Prayagraj Junction")
                                .arrivalTime("00:03")
                                .departureTime("00:05")
                                .haltMinutes(2)
                                .dayOffset(1)
                                .distanceFromSourceKm(634)
                                .platformNumber("4")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(4)
                                .stationCode("DDU")
                                .stationName("Pt. Deen Dayal Upadhyaya Jn")
                                .arrivalTime("02:00")
                                .departureTime("02:10")
                                .haltMinutes(10)
                                .dayOffset(1)
                                .distanceFromSourceKm(786)
                                .platformNumber("2")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(5)
                                .stationCode("PNBE")
                                .stationName("Patna Junction")
                                .arrivalTime("05:15")
                                .departureTime("DEST")
                                .haltMinutes(0)
                                .dayOffset(1)
                                .distanceFromSourceKm(998)
                                .platformNumber("1")
                                .build()
                ))
                .coachClasses(List.of(
                        CoachClassConfig.builder()
                                .classCode("1A")
                                .totalCoaches(2)
                                .seatsPerCoach(24)
                                .baseFarePerKm(new BigDecimal("3.50"))
                                .tatkalSurcharge(new BigDecimal("500.00"))
                                .build(),
                        CoachClassConfig.builder()
                                .classCode("2A")
                                .totalCoaches(5)
                                .seatsPerCoach(54)
                                .baseFarePerKm(new BigDecimal("2.20"))
                                .tatkalSurcharge(new BigDecimal("400.00"))
                                .build(),
                        CoachClassConfig.builder()
                                .classCode("3A")
                                .totalCoaches(11)
                                .seatsPerCoach(72)
                                .baseFarePerKm(new BigDecimal("1.50"))
                                .tatkalSurcharge(new BigDecimal("300.00"))
                                .build()
                ))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should filter out reverse direction trains and return only forward-traveling trains")
    void shouldFilterOutReverseDirectionTrains() {
        Set<DayOfWeek> allDays = Set.of(DayOfWeek.values());
        trainScheduleRepository.saveAll(List.of(createTejasRajdhani(allDays), createReverseRajdhani(allDays)));

        LocalDate searchDate = LocalDate.of(2026, 10, 19); // A Monday

        // Searching PNBE -> NDLS must return 12309 and NOT 12310
        List<TrainSearchResultDto> forwardResults = catalogService.searchTrains("PNBE", "NDLS", searchDate);
        assertThat(forwardResults).hasSize(1);
        assertThat(forwardResults.getFirst().trainNumber()).isEqualTo("12309");

        // Searching NDLS -> PNBE must return 12310 and NOT 12309
        List<TrainSearchResultDto> reverseResults = catalogService.searchTrains("NDLS", "PNBE", searchDate);
        assertThat(reverseResults).hasSize(1);
        assertThat(reverseResults.getFirst().trainNumber()).isEqualTo("12310");
    }

    @Test
    @DisplayName("Should calculate dynamic distance, duration, and fares for intermediate stops")
    void shouldCalculateDynamicDistanceDurationAndFaresForIntermediateStops() {
        trainScheduleRepository.save(createTejasRajdhani(Set.of(DayOfWeek.values())));

        LocalDate journeyDate = LocalDate.of(2026, 10, 19);

        // Searching DDU -> CNB on train 12309
        // DDU: dep 22:20, dayOffset 0, distance 212 km
        // CNB: arr 02:15, dayOffset 1, distance 558 km
        List<TrainSearchResultDto> results = catalogService.searchTrains("DDU", "CNB", journeyDate);

        assertThat(results).hasSize(1);
        TrainSearchResultDto result = results.getFirst();
        assertThat(result.trainNumber()).isEqualTo("12309");
        assertThat(result.fromStationCode()).isEqualTo("DDU");
        assertThat(result.toStationCode()).isEqualTo("CNB");
        assertThat(result.departureTime()).isEqualTo("22:20");
        assertThat(result.arrivalTime()).isEqualTo("02:15");

        // Distance: 558 - 212 = 346 km
        assertThat(result.distanceKm()).isEqualTo(346);

        // Duration: (1 - 0) * 1440 + ((2*60 + 15) - (22*60 + 20)) = 1440 + (135 - 1340) = 235 minutes
        assertThat(result.durationMinutes()).isEqualTo(235);

        // Fares:
        // 1A: 346 * 3.50 = 1211 general, 1211 + 500 = 1711 tatkal, seats = 2 * 24 = 48
        // 2A: 346 * 2.20 = 761 general, 761 + 400 = 1161 tatkal, seats = 5 * 54 = 270
        // 3A: 346 * 1.50 = 519 general, 519 + 300 = 819 tatkal, seats = 11 * 72 = 792
        assertThat(result.availableClasses()).hasSize(3);

        ClassFareDto fare1A = result.availableClasses().stream()
                .filter(c -> "1A".equals(c.classCode())).findFirst().orElseThrow();
        assertThat(fare1A.totalSeats()).isEqualTo(48);
        assertThat(fare1A.generalFare()).isEqualByComparingTo(new BigDecimal("1211"));
        assertThat(fare1A.tatkalFare()).isEqualByComparingTo(new BigDecimal("1711"));

        ClassFareDto fare2A = result.availableClasses().stream()
                .filter(c -> "2A".equals(c.classCode())).findFirst().orElseThrow();
        assertThat(fare2A.totalSeats()).isEqualTo(270);
        assertThat(fare2A.generalFare()).isEqualByComparingTo(new BigDecimal("761"));
        assertThat(fare2A.tatkalFare()).isEqualByComparingTo(new BigDecimal("1161"));

        ClassFareDto fare3A = result.availableClasses().stream()
                .filter(c -> "3A".equals(c.classCode())).findFirst().orElseThrow();
        assertThat(fare3A.totalSeats()).isEqualTo(792);
        assertThat(fare3A.generalFare()).isEqualByComparingTo(new BigDecimal("519"));
        assertThat(fare3A.tatkalFare()).isEqualByComparingTo(new BigDecimal("819"));
    }

    @Test
    @DisplayName("Should handle overnight dayOffset correctly based on train origin departure date")
    void shouldHandleOvernightDayOffsetCorrectly() {
        // Train 12309 runs ONLY on MONDAY from origin (PNBE)
        trainScheduleRepository.save(createTejasRajdhani(Set.of(DayOfWeek.MONDAY)));

        // 2026-10-19 is a MONDAY.
        // 2026-10-20 is a TUESDAY.
        LocalDate monday = LocalDate.of(2026, 10, 19);
        LocalDate tuesday = LocalDate.of(2026, 10, 20);

        // Origin stop PNBE (dayOffset = 0) on MONDAY -> matches Monday run
        List<TrainSearchResultDto> fromOriginMonday = catalogService.searchTrains("PNBE", "NDLS", monday);
        assertThat(fromOriginMonday).hasSize(1);

        // Intermediate stop CNB (dayOffset = 1) on TUESDAY -> matches Monday's origin run (Tuesday - 1 day = Monday)
        List<TrainSearchResultDto> fromCnbTuesday = catalogService.searchTrains("CNB", "NDLS", tuesday);
        assertThat(fromCnbTuesday).hasSize(1);
        assertThat(fromCnbTuesday.getFirst().trainNumber()).isEqualTo("12309");

        // Intermediate stop CNB on MONDAY -> does NOT match because origin would have departed on Sunday
        List<TrainSearchResultDto> fromCnbMonday = catalogService.searchTrains("CNB", "NDLS", monday);
        assertThat(fromCnbMonday).isEmpty();
    }

    @Test
    @DisplayName("Should cache search results in Redis and evict on new train schedule creation")
    void shouldCacheSearchResultsInRedisAndEvictOnNewTrainCreation() {
        trainScheduleRepository.save(createTejasRajdhani(Set.of(DayOfWeek.values())));

        LocalDate journeyDate = LocalDate.of(2026, 10, 20);
        String expectedCacheKey = "catalog:trains:PNBE:NDLS:2026-10-20";

        // Initially no cache
        assertThat(stringRedisTemplate.hasKey(expectedCacheKey)).isFalse();

        // 1. First search -> populates Redis cache
        List<TrainSearchResultDto> initialResults = catalogService.searchTrains("PNBE", "NDLS", journeyDate);
        assertThat(initialResults).hasSize(1);
        assertThat(stringRedisTemplate.hasKey(expectedCacheKey)).isTrue();

        // 2. Second search -> served from cache
        List<TrainSearchResultDto> cachedResults = catalogService.searchTrains("PNBE", "NDLS", journeyDate);
        assertThat(cachedResults).hasSize(1);

        // 3. Create a new train schedule -> should evict the Redis cache
        TrainScheduleRequest newTrain = new TrainScheduleRequest(
                "22436",
                "New Delhi Varanasi Vande Bharat Express",
                TrainType.VANDE_BHARAT,
                "NDLS",
                "BSB",
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                List.of(
                        RouteStop.builder().stopSequence(1).stationCode("NDLS").stationName("New Delhi").arrivalTime("SOURCE").departureTime("06:00").distanceFromSourceKm(0).build(),
                        RouteStop.builder().stopSequence(2).stationCode("BSB").stationName("Varanasi Jn").arrivalTime("14:00").departureTime("DEST").distanceFromSourceKm(759).build()
                ),
                List.of(
                        CoachClassConfig.builder().classCode("CC").totalCoaches(14).seatsPerCoach(78).baseFarePerKm(new BigDecimal("1.80")).tatkalSurcharge(new BigDecimal("250.00")).build()
                )
        );

        catalogService.createTrainSchedule(newTrain);

        // Verify cache key was evicted
        assertThat(stringRedisTemplate.hasKey(expectedCacheKey)).isFalse();
    }
}
