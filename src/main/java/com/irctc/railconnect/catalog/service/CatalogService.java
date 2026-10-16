package com.irctc.railconnect.catalog.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.irctc.railconnect.catalog.document.CoachClassConfig;
import com.irctc.railconnect.catalog.document.RouteStop;
import com.irctc.railconnect.catalog.document.StationDocument;
import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.dto.ClassFareDto;
import com.irctc.railconnect.catalog.dto.StationRequest;
import com.irctc.railconnect.catalog.dto.StationResponse;
import com.irctc.railconnect.catalog.dto.TrainScheduleRequest;
import com.irctc.railconnect.catalog.dto.TrainSearchResultDto;
import com.irctc.railconnect.catalog.repository.StationRepository;
import com.irctc.railconnect.catalog.repository.TrainScheduleRepository;
import com.irctc.railconnect.common.exception.BusinessRuleException;
import com.irctc.railconnect.common.exception.ResourceConflictException;
import com.irctc.railconnect.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private static final String TRAIN_CACHE_KEY_PREFIX = "catalog:trains:";

    private final StationRepository stationRepository;
    private final TrainScheduleRepository trainScheduleRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogService(
            StationRepository stationRepository,
            TrainScheduleRepository trainScheduleRepository,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.stationRepository = stationRepository;
        this.trainScheduleRepository = trainScheduleRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public StationResponse createStation(StationRequest request) {
        String normalizedCode = request.stationCode().trim().toUpperCase();

        if (stationRepository.existsByStationCodeIgnoreCase(normalizedCode)) {
            throw new ResourceConflictException("Station with code " + normalizedCode + " already exists");
        }

        StationDocument document = StationDocument.builder()
                .stationCode(normalizedCode)
                .stationName(request.stationName().trim())
                .city(request.city().trim())
                .state(request.state().trim())
                .zone(request.zone().trim())
                .numberOfPlatforms(request.numberOfPlatforms())
                .active(true)
                .build();

        StationDocument saved = stationRepository.save(document);
        return mapToStationResponse(saved);
    }

    public List<StationResponse> searchStations(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessRuleException("Search query cannot be null or blank");
        }

        return stationRepository.searchStations(query.trim()).stream()
                .map(this::mapToStationResponse)
                .toList();
    }

    public TrainScheduleDocument createTrainSchedule(TrainScheduleRequest request) {
        String trainNumber = request.trainNumber().trim();

        if (trainScheduleRepository.existsByTrainNumber(trainNumber)) {
            throw new ResourceConflictException("Train schedule with number " + trainNumber + " already exists");
        }

        if (request.routeStops() == null || request.routeStops().size() < 2) {
            throw new BusinessRuleException("Train route must contain at least 2 stops");
        }

        TrainScheduleDocument document = TrainScheduleDocument.builder()
                .trainNumber(trainNumber)
                .trainName(request.trainName().trim())
                .trainType(request.trainType())
                .sourceStationCode(request.sourceStationCode().trim().toUpperCase())
                .destinationStationCode(request.destinationStationCode().trim().toUpperCase())
                .runningDays(request.runningDays())
                .routeStops(request.routeStops())
                .coachClasses(request.coachClasses())
                .active(true)
                .build();

        TrainScheduleDocument saved = trainScheduleRepository.save(document);
        evictCatalogTrainCache();
        return saved;
    }

    public TrainScheduleDocument getTrainByNumber(String trainNumber) {
        if (trainNumber == null || trainNumber.isBlank()) {
            throw new ResourceNotFoundException("Train number cannot be blank");
        }

        return trainScheduleRepository.findByTrainNumber(trainNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Train schedule not found for train number: " + trainNumber.trim()));
    }

    public List<TrainSearchResultDto> searchTrains(String fromStationCode, String toStationCode, LocalDate journeyDate) {
        if (fromStationCode == null || toStationCode == null || journeyDate == null) {
            throw new BusinessRuleException("Source station, destination station, and journey date are required");
        }

        String from = fromStationCode.trim().toUpperCase();
        String to = toStationCode.trim().toUpperCase();

        if (from.equals(to)) {
            throw new BusinessRuleException("Source and destination stations cannot be the same: " + from);
        }

        String cacheKey = String.format("%s%s:%s:%s", TRAIN_CACHE_KEY_PREFIX, from, to, journeyDate);

        // Check Redis cache first
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.debug("Cache hit for train search key: {}", cacheKey);
                return objectMapper.readValue(cachedJson, new TypeReference<List<TrainSearchResultDto>>() {});
            }
        } catch (Exception ex) {
            log.warn("Failed to read from Redis cache for key {}: {}", cacheKey, ex.getMessage());
        }

        // Query candidates from MongoDB
        List<TrainScheduleDocument> candidates = trainScheduleRepository.findActiveTrainsContainingBothStops(from, to);
        List<TrainSearchResultDto> results = new ArrayList<>();

        for (TrainScheduleDocument train : candidates) {
            RouteStop fromStop = null;
            RouteStop toStop = null;

            for (RouteStop stop : train.getRouteStops()) {
                if (stop.getStationCode().equalsIgnoreCase(from)) {
                    fromStop = stop;
                } else if (stop.getStationCode().equalsIgnoreCase(to)) {
                    toStop = stop;
                }
            }

            if (fromStop == null || toStop == null) {
                continue;
            }

            // Forward travel direction validation
            if (fromStop.getStopSequence() >= toStop.getStopSequence()) {
                continue;
            }

            // Overnight running day check based on origin departure date
            LocalDate originDepartureDate = journeyDate.minusDays(fromStop.getDayOffset());
            if (train.getRunningDays() == null || !train.getRunningDays().contains(originDepartureDate.getDayOfWeek())) {
                continue;
            }

            // Distance calculation
            int distanceKm = toStop.getDistanceFromSourceKm() - fromStop.getDistanceFromSourceKm();

            // Duration calculation
            String depTimeStr = "SOURCE".equalsIgnoreCase(fromStop.getDepartureTime())
                    ? fromStop.getArrivalTime() : fromStop.getDepartureTime();
            String arrTimeStr = "DEST".equalsIgnoreCase(toStop.getArrivalTime())
                    ? toStop.getDepartureTime() : toStop.getArrivalTime();

            LocalTime depTime = LocalTime.parse(depTimeStr);
            LocalTime arrTime = LocalTime.parse(arrTimeStr);

            int dayDiff = toStop.getDayOffset() - fromStop.getDayOffset();
            int depMinutes = depTime.getHour() * 60 + depTime.getMinute();
            int arrMinutes = arrTime.getHour() * 60 + arrTime.getMinute();
            int durationMinutes = (dayDiff * 1440) + (arrMinutes - depMinutes);

            // Coach class and dynamic fare calculation
            List<ClassFareDto> availableClasses = new ArrayList<>();
            if (train.getCoachClasses() != null) {
                for (CoachClassConfig coach : train.getCoachClasses()) {
                    int totalSeats = coach.getTotalCoaches() * coach.getSeatsPerCoach();
                    BigDecimal generalFare = coach.getBaseFarePerKm()
                            .multiply(BigDecimal.valueOf(distanceKm))
                            .setScale(0, RoundingMode.HALF_UP);
                    BigDecimal tatkalFare = generalFare
                            .add(coach.getTatkalSurcharge())
                            .setScale(0, RoundingMode.HALF_UP);

                    availableClasses.add(new ClassFareDto(coach.getClassCode(), totalSeats, generalFare, tatkalFare));
                }
            }

            results.add(new TrainSearchResultDto(
                    train.getTrainNumber(),
                    train.getTrainName(),
                    train.getTrainType(),
                    fromStop.getStationCode(),
                    fromStop.getStationName(),
                    fromStop.getDepartureTime(),
                    toStop.getStationCode(),
                    toStop.getStationName(),
                    toStop.getArrivalTime(),
                    durationMinutes,
                    distanceKm,
                    train.getRunningDays(),
                    availableClasses
            ));
        }

        // Cache results in Redis for 15 minutes
        try {
            String json = objectMapper.writeValueAsString(results);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(15));
        } catch (Exception ex) {
            log.warn("Failed to cache search results in Redis for key {}: {}", cacheKey, ex.getMessage());
        }

        return results;
    }

    private void evictCatalogTrainCache() {
        try {
            Set<String> keys = stringRedisTemplate.keys(TRAIN_CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("Evicted {} keys from Redis catalog train cache", keys.size());
            }
        } catch (Exception ex) {
            log.warn("Failed to evict Redis cache for prefix '{}*': {}", TRAIN_CACHE_KEY_PREFIX, ex.getMessage());
        }
    }

    private StationResponse mapToStationResponse(StationDocument doc) {
        return new StationResponse(
                doc.getId(),
                doc.getStationCode(),
                doc.getStationName(),
                doc.getCity(),
                doc.getState(),
                doc.getZone(),
                doc.getNumberOfPlatforms()
        );
    }
}
