package com.irctc.railconnect.catalog.controller;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.catalog.config.CatalogDataInitializer;
import com.irctc.railconnect.catalog.repository.StationRepository;
import com.irctc.railconnect.catalog.repository.TrainScheduleRepository;
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

import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CatalogDataInitializer catalogDataInitializer;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
        trainScheduleRepository.deleteAll();

        Set<String> keys = stringRedisTemplate.keys("catalog:trains:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        // Seed fresh data from external JSON files for every test
        catalogDataInitializer.seedCatalogIfEmpty();
    }

    @Test
    @DisplayName("Should load stations from external JSON and search stations by query")
    void shouldLoadStationsAndTrainsFromExternalJsonAndSearchStations() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/stations/search")
                        .param("q", "Patna")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].stationCode").value("PNBE"))
                .andExpect(jsonPath("$.data[0].stationName").value("Patna Junction"))
                .andExpect(jsonPath("$.data[0].city").value("Patna"));
    }

    @Test
    @DisplayName("Should search trains between stations via GET endpoint and return calculated fares")
    void shouldSearchTrainsBetweenStationsViaGetEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/trains/search")
                        .param("from", "PNBE")
                        .param("to", "NDLS")
                        .param("date", "2026-10-19")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].trainNumber").value("12309"))
                .andExpect(jsonPath("$.data[0].trainName").value("Patna New Delhi Tejas Rajdhani Express"))
                .andExpect(jsonPath("$.data[0].fromStationCode").value("PNBE"))
                .andExpect(jsonPath("$.data[0].toStationCode").value("NDLS"))
                .andExpect(jsonPath("$.data[0].distanceKm").value(998))
                .andExpect(jsonPath("$.data[0].availableClasses", hasSize(3)))
                .andExpect(jsonPath("$.data[0].availableClasses[0].classCode").value("1A"))
                .andExpect(jsonPath("$.data[0].availableClasses[0].generalFare").isNotEmpty())
                .andExpect(jsonPath("$.data[0].availableClasses[0].tatkalFare").isNotEmpty());
    }

    @Test
    @DisplayName("Should get train schedule by number and return 404 for unknown train")
    void shouldGetTrainScheduleByNumberAndReturn404ForUnknownTrain() throws Exception {
        // Known train 12301 with 7 route stops
        mockMvc.perform(get("/api/v1/catalog/trains/12301")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("12301"))
                .andExpect(jsonPath("$.data.trainName").value("Howrah New Delhi Rajdhani Express"))
                .andExpect(jsonPath("$.data.sourceStationCode").value("HWH"))
                .andExpect(jsonPath("$.data.destinationStationCode").value("NDLS"))
                .andExpect(jsonPath("$.data.routeStops", hasSize(7)));

        // Unknown train 99999 -> 404 NOT_FOUND
        mockMvc.perform(get("/api/v1/catalog/trains/99999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should create station via POST and reject invalid payload with 400")
    void shouldCreateStationViaPostAndRejectInvalidPayloadWith400() throws Exception {
        // Valid POST
        String validStationJson = """
                {
                  "stationCode": "BPL",
                  "stationName": "Bhopal Junction",
                  "city": "Bhopal",
                  "state": "Madhya Pradesh",
                  "zone": "WCR",
                  "numberOfPlatforms": 6
                }
                """;

        mockMvc.perform(post("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validStationJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stationCode").value("BPL"))
                .andExpect(jsonPath("$.data.stationName").value("Bhopal Junction"))
                .andExpect(jsonPath("$.data.numberOfPlatforms").value(6));

        // Invalid POST with blank fields -> 400 Bad Request
        String invalidStationJson = """
                {
                  "stationCode": "",
                  "stationName": "",
                  "city": "",
                  "state": "",
                  "zone": "",
                  "numberOfPlatforms": -1
                }
                """;

        mockMvc.perform(post("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStationJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
