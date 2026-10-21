package com.irctc.railconnect.catalog.config;

import com.irctc.railconnect.catalog.document.StationDocument;
import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.repository.StationRepository;
import com.irctc.railconnect.catalog.repository.TrainScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

@Component
public class CatalogDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataInitializer.class);

    private final StationRepository stationRepository;
    private final TrainScheduleRepository trainScheduleRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.catalog.seed-on-startup:true}")
    private boolean seedOnStartup;

    public CatalogDataInitializer(
            StationRepository stationRepository,
            TrainScheduleRepository trainScheduleRepository,
            ObjectMapper objectMapper
    ) {
        this.stationRepository = stationRepository;
        this.trainScheduleRepository = trainScheduleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedOnStartup) {
            seedCatalogIfEmpty();
        }
    }

    public void seedCatalogIfEmpty() {
        if (stationRepository.count() < 300) {
            stationRepository.deleteAll();
            try (InputStream is = new ClassPathResource("data/stations.json").getInputStream()) {
                List<StationDocument> stations = objectMapper.readValue(is, new TypeReference<List<StationDocument>>() {});
                stationRepository.saveAll(stations);
                log.info("Successfully seeded {} stations from data/stations.json", stations.size());
            } catch (Exception ex) {
                log.error("Failed to seed stations from data/stations.json: {}", ex.getMessage(), ex);
            }
        }

        if (trainScheduleRepository.count() < 1000) {
            trainScheduleRepository.deleteAll();
            try (InputStream is = new ClassPathResource("data/trains.json").getInputStream()) {
                List<TrainScheduleDocument> trains = objectMapper.readValue(is, new TypeReference<List<TrainScheduleDocument>>() {});
                trainScheduleRepository.saveAll(trains);
                log.info("Successfully seeded {} trains from data/trains.json", trains.size());
            } catch (Exception ex) {
                log.error("Failed to seed trains from data/trains.json: {}", ex.getMessage(), ex);
            }
        }
    }
}
