package com.irctc.railconnect.catalog.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.catalog.document.StationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StationRepositoryTest {

    @Autowired
    private StationRepository stationRepository;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();

        // Seed realistic Indian stations
        List<StationDocument> initialStations = List.of(
                StationDocument.builder()
                        .stationCode("PNBE")
                        .stationName("Patna Junction")
                        .city("Patna")
                        .state("Bihar")
                        .zone("ECR")
                        .numberOfPlatforms(10)
                        .active(true)
                        .build(),
                StationDocument.builder()
                        .stationCode("DDU")
                        .stationName("Pt. Deen Dayal Upadhyaya Jn")
                        .city("Mughalsarai")
                        .state("Uttar Pradesh")
                        .zone("ECR")
                        .numberOfPlatforms(8)
                        .active(true)
                        .build(),
                StationDocument.builder()
                        .stationCode("CNB")
                        .stationName("Kanpur Central")
                        .city("Kanpur")
                        .state("Uttar Pradesh")
                        .zone("NCR")
                        .numberOfPlatforms(10)
                        .active(true)
                        .build(),
                StationDocument.builder()
                        .stationCode("NDLS")
                        .stationName("New Delhi")
                        .city("New Delhi")
                        .state("Delhi")
                        .zone("NR")
                        .numberOfPlatforms(16)
                        .active(true)
                        .build(),
                // Inactive station to verify filtering
                StationDocument.builder()
                        .stationCode("TEST_INACTIVE")
                        .stationName("Abandoned Patna Halt")
                        .city("Patna")
                        .state("Bihar")
                        .zone("ECR")
                        .numberOfPlatforms(1)
                        .active(false)
                        .build()
        );

        stationRepository.saveAll(initialStations);
    }

    @Test
    @DisplayName("Should find station by station code case-insensitively")
    void shouldFindStationByCodeIgnoreCase() {
        Optional<StationDocument> result = stationRepository.findByStationCodeIgnoreCase("pnbe");

        assertThat(result).isPresent();
        assertThat(result.get().getStationCode()).isEqualTo("PNBE");
        assertThat(result.get().getStationName()).isEqualTo("Patna Junction");
        assertThat(result.get().getCity()).isEqualTo("Patna");
        assertThat(result.get().getState()).isEqualTo("Bihar");
        assertThat(result.get().getZone()).isEqualTo("ECR");
    }

    @Test
    @DisplayName("Should search stations by partial code, name, or city excluding inactive stations")
    void shouldSearchStationsByPartialCodeNameOrCity() {
        // Query by city name "Patna"
        List<StationDocument> patnaResults = stationRepository.searchStations("Patna");
        assertThat(patnaResults).hasSize(1);
        assertThat(patnaResults.getFirst().getStationCode()).isEqualTo("PNBE");
        // Verify inactive station with "Patna" in city is excluded
        assertThat(patnaResults).noneMatch(s -> "TEST_INACTIVE".equals(s.getStationCode()));

        // Query by partial station code "NDL"
        List<StationDocument> ndlResults = stationRepository.searchStations("NDL");
        assertThat(ndlResults).hasSize(1);
        assertThat(ndlResults.getFirst().getStationCode()).isEqualTo("NDLS");

        // Query by city "Kanpur"
        List<StationDocument> kanpurResults = stationRepository.searchStations("Kanpur");
        assertThat(kanpurResults).hasSize(1);
        assertThat(kanpurResults.getFirst().getStationCode()).isEqualTo("CNB");
    }

    @Test
    @DisplayName("Should enforce unique index on stationCode and throw DuplicateKeyException")
    void shouldEnforceUniqueStationCodeIndex() {
        StationDocument duplicate = StationDocument.builder()
                .stationCode("PNBE")
                .stationName("Duplicate Patna Junction")
                .city("Patna")
                .state("Bihar")
                .zone("ECR")
                .numberOfPlatforms(5)
                .active(true)
                .build();

        assertThrows(DuplicateKeyException.class, () -> stationRepository.save(duplicate));
    }
}
