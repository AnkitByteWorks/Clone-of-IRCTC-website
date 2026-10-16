package com.irctc.railconnect.catalog.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.catalog.document.CoachClassConfig;
import com.irctc.railconnect.catalog.document.RouteStop;
import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.document.TrainType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TrainScheduleRepositoryTest {

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @BeforeEach
    void setUp() {
        trainScheduleRepository.deleteAll();

        // 1. Train 12309: Patna - New Delhi Tejas Rajdhani Express (Runs Daily)
        TrainScheduleDocument rajdhani = TrainScheduleDocument.builder()
                .trainNumber("12309")
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .trainType(TrainType.RAJDHANI)
                .sourceStationCode("PNBE")
                .destinationStationCode("NDLS")
                .runningDays(Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                ))
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

        // 2. Train 22436: New Delhi - Varanasi Vande Bharat Express (Runs all days except Thursday)
        TrainScheduleDocument vandeBharat = TrainScheduleDocument.builder()
                .trainNumber("22436")
                .trainName("New Delhi Varanasi Vande Bharat Express")
                .trainType(TrainType.VANDE_BHARAT)
                .sourceStationCode("NDLS")
                .destinationStationCode("BSB")
                .runningDays(Set.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                ))
                .routeStops(List.of(
                        RouteStop.builder()
                                .stopSequence(1)
                                .stationCode("NDLS")
                                .stationName("New Delhi")
                                .arrivalTime("SOURCE")
                                .departureTime("06:00")
                                .haltMinutes(0)
                                .dayOffset(0)
                                .distanceFromSourceKm(0)
                                .platformNumber("16")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(2)
                                .stationCode("CNB")
                                .stationName("Kanpur Central")
                                .arrivalTime("10:08")
                                .departureTime("10:10")
                                .haltMinutes(2)
                                .dayOffset(0)
                                .distanceFromSourceKm(440)
                                .platformNumber("1")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(3)
                                .stationCode("PRYJ")
                                .stationName("Prayagraj Junction")
                                .arrivalTime("12:08")
                                .departureTime("12:10")
                                .haltMinutes(2)
                                .dayOffset(0)
                                .distanceFromSourceKm(634)
                                .platformNumber("6")
                                .build(),
                        RouteStop.builder()
                                .stopSequence(4)
                                .stationCode("BSB")
                                .stationName("Varanasi Junction")
                                .arrivalTime("14:00")
                                .departureTime("DEST")
                                .haltMinutes(0)
                                .dayOffset(0)
                                .distanceFromSourceKm(759)
                                .platformNumber("1")
                                .build()
                ))
                .coachClasses(List.of(
                        CoachClassConfig.builder()
                                .classCode("CC")
                                .totalCoaches(14)
                                .seatsPerCoach(78)
                                .baseFarePerKm(new BigDecimal("1.80"))
                                .tatkalSurcharge(new BigDecimal("250.00"))
                                .build(),
                        CoachClassConfig.builder()
                                .classCode("EC")
                                .totalCoaches(2)
                                .seatsPerCoach(52)
                                .baseFarePerKm(new BigDecimal("3.00"))
                                .tatkalSurcharge(new BigDecimal("450.00"))
                                .build()
                ))
                .active(true)
                .build();

        trainScheduleRepository.saveAll(List.of(rajdhani, vandeBharat));
    }

    @Test
    @DisplayName("Should find train schedule by train number with all route stops and coach classes")
    void shouldFindTrainByTrainNumber() {
        Optional<TrainScheduleDocument> result = trainScheduleRepository.findByTrainNumber("12309");

        assertThat(result).isPresent();
        TrainScheduleDocument train = result.get();
        assertThat(train.getTrainName()).isEqualTo("Patna New Delhi Tejas Rajdhani Express");
        assertThat(train.getTrainType()).isEqualTo(TrainType.RAJDHANI);
        assertThat(train.getSourceStationCode()).isEqualTo("PNBE");
        assertThat(train.getDestinationStationCode()).isEqualTo("NDLS");

        // Verify 5 route stops
        assertThat(train.getRouteStops()).hasSize(5);
        assertThat(train.getRouteStops().getFirst().getStationCode()).isEqualTo("PNBE");
        assertThat(train.getRouteStops().getLast().getStationCode()).isEqualTo("NDLS");

        // Verify coach classes: 1A, 2A, 3A
        assertThat(train.getCoachClasses()).hasSize(3);
        assertThat(train.getCoachClasses())
                .extracting(CoachClassConfig::getClassCode)
                .containsExactly("1A", "2A", "3A");
    }

    @Test
    @DisplayName("Should find trains containing intermediate stops on a running day")
    void shouldFindTrainsContainingIntermediateStopsOnRunningDay() {
        // DDU and CNB are intermediate stops for train 12309
        List<TrainScheduleDocument> trainsOnMonday = trainScheduleRepository
                .findActiveTrainsContainingBothStopsOnDay("DDU", "CNB", DayOfWeek.MONDAY);

        assertThat(trainsOnMonday).hasSize(1);
        assertThat(trainsOnMonday.getFirst().getTrainNumber()).isEqualTo("12309");
    }

    @Test
    @DisplayName("Should not return train on non-running day")
    void shouldNotReturnTrainOnNonRunningDay() {
        // Query between NDLS and CNB on THURSDAY:
        // 12309 runs on Thursday, but 22436 does NOT run on Thursday
        List<TrainScheduleDocument> thursdayTrains = trainScheduleRepository
                .findActiveTrainsContainingBothStopsOnDay("NDLS", "CNB", DayOfWeek.THURSDAY);

        assertThat(thursdayTrains).hasSize(1);
        assertThat(thursdayTrains.getFirst().getTrainNumber()).isEqualTo("12309");

        // Query between NDLS and CNB on FRIDAY:
        // Both 12309 and 22436 run on Friday
        List<TrainScheduleDocument> fridayTrains = trainScheduleRepository
                .findActiveTrainsContainingBothStopsOnDay("NDLS", "CNB", DayOfWeek.FRIDAY);

        assertThat(fridayTrains).hasSize(2);
        assertThat(fridayTrains)
                .extracting(TrainScheduleDocument::getTrainNumber)
                .containsExactlyInAnyOrder("12309", "22436");
    }
}
