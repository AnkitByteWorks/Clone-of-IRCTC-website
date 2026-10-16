package com.irctc.railconnect.inventory.service;

import com.irctc.railconnect.catalog.document.CoachClassConfig;
import com.irctc.railconnect.catalog.document.RouteStop;
import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.service.CatalogService;
import com.irctc.railconnect.common.exception.BusinessRuleException;
import com.irctc.railconnect.common.exception.ResourceConflictException;
import com.irctc.railconnect.common.exception.ResourceNotFoundException;
import com.irctc.railconnect.inventory.config.InventoryProperties;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import com.irctc.railconnect.inventory.dto.QuotaAvailabilityDto;
import com.irctc.railconnect.inventory.dto.QuotaUpdateRequest;
import com.irctc.railconnect.inventory.dto.SeatDto;
import com.irctc.railconnect.inventory.dto.SeatLockRequest;
import com.irctc.railconnect.inventory.dto.SeatLockResponse;
import com.irctc.railconnect.inventory.dto.TrainAvailabilityResponse;
import com.irctc.railconnect.inventory.entity.CoachSeatEntity;
import com.irctc.railconnect.inventory.entity.DailyTrainQuotaEntity;
import com.irctc.railconnect.inventory.repository.CoachSeatRepository;
import com.irctc.railconnect.inventory.repository.DailyTrainQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final DailyTrainQuotaRepository dailyTrainQuotaRepository;
    private final CoachSeatRepository coachSeatRepository;
    private final CatalogService catalogService;
    private final SeatLockService seatLockService;
    private final InventoryProperties inventoryProperties;

    public InventoryService(
            DailyTrainQuotaRepository dailyTrainQuotaRepository,
            CoachSeatRepository coachSeatRepository,
            CatalogService catalogService,
            SeatLockService seatLockService,
            InventoryProperties inventoryProperties
    ) {
        this.dailyTrainQuotaRepository = dailyTrainQuotaRepository;
        this.coachSeatRepository = coachSeatRepository;
        this.catalogService = catalogService;
        this.seatLockService = seatLockService;
        this.inventoryProperties = inventoryProperties;
    }

    @Transactional
    public void ensureDailyInventoryInitialized(String trainNumber, LocalDate journeyDate) {
        List<DailyTrainQuotaEntity> existingQuotas = dailyTrainQuotaRepository
                .findByTrainNumberAndJourneyDate(trainNumber, journeyDate);
        if (!existingQuotas.isEmpty()) {
            return;
        }

        TrainScheduleDocument train = catalogService.getTrainByNumber(trainNumber);
        int totalDistanceKm = train.getRouteStops().stream()
                .mapToInt(RouteStop::getDistanceFromSourceKm)
                .max()
                .orElse(1000);

        List<DailyTrainQuotaEntity> quotasToSave = new ArrayList<>();
        List<CoachSeatEntity> seatsToSave = new ArrayList<>();

        if (train.getCoachClasses() != null) {
            for (CoachClassConfig coachClass : train.getCoachClasses()) {
                int totalSeats = coachClass.getTotalCoaches() * coachClass.getSeatsPerCoach();
                int generalSeats = (int) Math.round(totalSeats * inventoryProperties.generalQuotaRatio());
                int tatkalSeats = totalSeats - generalSeats;

                BigDecimal generalFare = coachClass.getBaseFarePerKm()
                        .multiply(BigDecimal.valueOf(totalDistanceKm))
                        .setScale(0, RoundingMode.HALF_UP);
                BigDecimal tatkalFare = generalFare
                        .add(coachClass.getTatkalSurcharge())
                        .setScale(0, RoundingMode.HALF_UP);

                // 1. GENERAL Quota
                quotasToSave.add(DailyTrainQuotaEntity.builder()
                        .trainNumber(trainNumber)
                        .journeyDate(journeyDate)
                        .classCode(coachClass.getClassCode())
                        .quotaType(QuotaType.GENERAL)
                        .totalConfirmedSeats(generalSeats)
                        .availableConfirmedSeats(generalSeats)
                        .totalRacSlots(inventoryProperties.defaultGeneralRacSlots())
                        .availableRacSlots(inventoryProperties.defaultGeneralRacSlots())
                        .maxWaitlistLimit(inventoryProperties.defaultGeneralWaitlistLimit())
                        .currentWaitlistCount(0)
                        .baseFare(generalFare)
                        .build());

                // 2. TATKAL Quota
                quotasToSave.add(DailyTrainQuotaEntity.builder()
                        .trainNumber(trainNumber)
                        .journeyDate(journeyDate)
                        .classCode(coachClass.getClassCode())
                        .quotaType(QuotaType.TATKAL)
                        .totalConfirmedSeats(tatkalSeats)
                        .availableConfirmedSeats(tatkalSeats)
                        .totalRacSlots(0)
                        .availableRacSlots(0)
                        .maxWaitlistLimit(inventoryProperties.defaultTatkalWaitlistLimit())
                        .currentWaitlistCount(0)
                        .baseFare(tatkalFare)
                        .build());

                // 3. Physical Seats
                String prefix = inventoryProperties.coachPrefixByClass()
                        .getOrDefault(coachClass.getClassCode(), coachClass.getClassCode());
                boolean isChairCar = inventoryProperties.chairCarClasses().contains(coachClass.getClassCode());

                for (int coachIdx = 1; coachIdx <= coachClass.getTotalCoaches(); coachIdx++) {
                    String coachNumber = prefix + coachIdx;
                    for (int seatNum = 1; seatNum <= coachClass.getSeatsPerCoach(); seatNum++) {
                        BerthType berthType = determineBerthType(seatNum, isChairCar);
                        seatsToSave.add(CoachSeatEntity.builder()
                                .trainNumber(trainNumber)
                                .journeyDate(journeyDate)
                                .classCode(coachClass.getClassCode())
                                .coachNumber(coachNumber)
                                .seatNumber(seatNum)
                                .berthType(berthType)
                                .status(SeatStatus.AVAILABLE)
                                .build());
                    }
                }
            }
        }

        dailyTrainQuotaRepository.saveAll(quotasToSave);
        coachSeatRepository.saveAll(seatsToSave);
        log.info("Initialized inventory for train {} on {}: {} quotas, {} seats",
                trainNumber, journeyDate, quotasToSave.size(), seatsToSave.size());
    }

    @Transactional
    public TrainAvailabilityResponse getTrainAvailability(String trainNumber, LocalDate journeyDate) {
        ensureDailyInventoryInitialized(trainNumber, journeyDate);
        TrainScheduleDocument train = catalogService.getTrainByNumber(trainNumber);
        List<DailyTrainQuotaEntity> quotas = dailyTrainQuotaRepository.findByTrainNumberAndJourneyDate(trainNumber, journeyDate);

        List<QuotaAvailabilityDto> quotaDtos = quotas.stream()
                .map(this::mapToQuotaAvailabilityDto)
                .toList();

        return new TrainAvailabilityResponse(train.getTrainNumber(), train.getTrainName(), journeyDate, quotaDtos);
    }

    @Transactional
    public List<SeatDto> getCoachSeatMap(String trainNumber, LocalDate journeyDate, String classCode, String currentUserEmail) {
        ensureDailyInventoryInitialized(trainNumber, journeyDate);

        List<CoachSeatEntity> seats = coachSeatRepository
                .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
                        trainNumber, journeyDate, classCode
                );

        return seats.stream().map(seat -> {
            Optional<String> lockOwnerOpt = seatLockService.getLockOwner(
                    trainNumber, journeyDate, seat.getCoachNumber(), seat.getSeatNumber()
            );

            SeatStatus effectiveStatus;
            boolean lockedByCurrentUser = false;

            if (lockOwnerOpt.isPresent()) {
                effectiveStatus = SeatStatus.LOCKED;
                lockedByCurrentUser = currentUserEmail != null && currentUserEmail.equalsIgnoreCase(lockOwnerOpt.get());
            } else if (seat.getStatus() == SeatStatus.BOOKED) {
                effectiveStatus = SeatStatus.BOOKED;
            } else {
                effectiveStatus = SeatStatus.AVAILABLE;
            }

            return new SeatDto(
                    seat.getId(),
                    seat.getCoachNumber(),
                    seat.getSeatNumber(),
                    seat.getBerthType(),
                    effectiveStatus,
                    lockedByCurrentUser
            );
        }).toList();
    }

    @Transactional
    public SeatLockResponse lockSpecificSeat(SeatLockRequest request, String userEmail) {
        ensureDailyInventoryInitialized(request.trainNumber(), request.journeyDate());

        List<CoachSeatEntity> coachSeats = coachSeatRepository
                .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
                        request.trainNumber(), request.journeyDate(), request.classCode()
                );

        CoachSeatEntity seat = coachSeats.stream()
                .filter(s -> s.getCoachNumber().equalsIgnoreCase(request.coachNumber())
                        && s.getSeatNumber() == request.seatNumber())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seat not found: " + request.coachNumber() + "-" + request.seatNumber()));

        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new ResourceConflictException(
                    "Seat " + request.coachNumber() + "-" + request.seatNumber() + " is already booked");
        }

        boolean acquired = seatLockService.acquireSeatLock(
                request.trainNumber(),
                request.journeyDate(),
                request.coachNumber(),
                request.seatNumber(),
                userEmail
        );

        if (!acquired) {
            throw new ResourceConflictException(
                    "Seat " + request.coachNumber() + "-" + request.seatNumber() + " is currently locked by another user");
        }

        Instant expiresAt = Instant.now().plusSeconds(inventoryProperties.seatLockTtlSeconds());
        seat.setStatus(SeatStatus.LOCKED);
        seat.setLockedByUserEmail(userEmail);
        seat.setLockExpiresAt(expiresAt);
        coachSeatRepository.save(seat);

        return new SeatLockResponse(
                request.trainNumber(),
                request.journeyDate(),
                request.coachNumber(),
                request.seatNumber(),
                seat.getBerthType(),
                userEmail,
                inventoryProperties.seatLockTtlSeconds(),
                expiresAt
        );
    }

    @Transactional
    public void unlockSpecificSeat(SeatLockRequest request, String userEmail) {
        Optional<String> lockOwner = seatLockService.getLockOwner(
                request.trainNumber(), request.journeyDate(), request.coachNumber(), request.seatNumber()
        );

        if (lockOwner.isPresent() && !lockOwner.get().equalsIgnoreCase(userEmail)) {
            throw new BusinessRuleException("Cannot unlock seat: locked by another user");
        }

        seatLockService.releaseSeatLock(
                request.trainNumber(), request.journeyDate(), request.coachNumber(), request.seatNumber(), userEmail
        );

        coachSeatRepository.findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
                        request.trainNumber(), request.journeyDate(), request.classCode()
                ).stream()
                .filter(s -> s.getCoachNumber().equalsIgnoreCase(request.coachNumber())
                        && s.getSeatNumber() == request.seatNumber())
                .findFirst()
                .ifPresent(s -> {
                    if (s.getStatus() == SeatStatus.LOCKED && userEmail.equalsIgnoreCase(s.getLockedByUserEmail())) {
                        s.setStatus(SeatStatus.AVAILABLE);
                        s.setLockedByUserEmail(null);
                        s.setLockExpiresAt(null);
                        coachSeatRepository.save(s);
                    }
                });
    }

    @Transactional
    public QuotaAvailabilityDto upsertQuotaConfig(QuotaUpdateRequest request) {
        Optional<DailyTrainQuotaEntity> quotaOpt = dailyTrainQuotaRepository
                .findByTrainNumberAndJourneyDateAndClassCodeAndQuotaType(
                        request.trainNumber(), request.journeyDate(), request.classCode(), request.quotaType()
                );

        DailyTrainQuotaEntity entity;
        if (quotaOpt.isPresent()) {
            entity = quotaOpt.get();
            entity.setTotalConfirmedSeats(request.totalConfirmedSeats());
            entity.setAvailableConfirmedSeats(Math.min(entity.getAvailableConfirmedSeats(), request.totalConfirmedSeats()));
            entity.setTotalRacSlots(request.totalRacSlots());
            entity.setAvailableRacSlots(Math.min(entity.getAvailableRacSlots(), request.totalRacSlots()));
            entity.setMaxWaitlistLimit(request.maxWaitlistLimit());
        } else {
            TrainScheduleDocument train = catalogService.getTrainByNumber(request.trainNumber());
            int totalDistanceKm = train.getRouteStops().stream()
                    .mapToInt(RouteStop::getDistanceFromSourceKm)
                    .max()
                    .orElse(1000);

            CoachClassConfig classConfig = train.getCoachClasses().stream()
                    .filter(c -> c.getClassCode().equalsIgnoreCase(request.classCode()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Class code not found on train: " + request.classCode()));

            BigDecimal fare = classConfig.getBaseFarePerKm()
                    .multiply(BigDecimal.valueOf(totalDistanceKm))
                    .setScale(0, RoundingMode.HALF_UP);

            if (request.quotaType() == QuotaType.TATKAL) {
                fare = fare.add(classConfig.getTatkalSurcharge());
            }

            entity = DailyTrainQuotaEntity.builder()
                    .trainNumber(request.trainNumber())
                    .journeyDate(request.journeyDate())
                    .classCode(request.classCode())
                    .quotaType(request.quotaType())
                    .totalConfirmedSeats(request.totalConfirmedSeats())
                    .availableConfirmedSeats(request.totalConfirmedSeats())
                    .totalRacSlots(request.totalRacSlots())
                    .availableRacSlots(request.totalRacSlots())
                    .maxWaitlistLimit(request.maxWaitlistLimit())
                    .currentWaitlistCount(0)
                    .baseFare(fare)
                    .build();
        }

        DailyTrainQuotaEntity saved = dailyTrainQuotaRepository.save(entity);
        return mapToQuotaAvailabilityDto(saved);
    }

    private BerthType determineBerthType(int seatNum, boolean isChairCar) {
        if (isChairCar) {
            return (seatNum % 2 == 1) ? BerthType.WINDOW : BerthType.AISLE;
        }
        int mod = seatNum % 8;
        return switch (mod) {
            case 1, 4 -> BerthType.LOWER;
            case 2, 5 -> BerthType.MIDDLE;
            case 3, 6 -> BerthType.UPPER;
            case 7 -> BerthType.SIDE_LOWER;
            case 0 -> BerthType.SIDE_UPPER;
            default -> BerthType.LOWER;
        };
    }

    private QuotaAvailabilityDto mapToQuotaAvailabilityDto(DailyTrainQuotaEntity q) {
        return new QuotaAvailabilityDto(
                q.getClassCode(),
                q.getQuotaType(),
                q.getCurrentAvailabilityStatus(),
                q.getDisplayStatusText(),
                q.getAvailableConfirmedSeats(),
                q.getAvailableRacSlots(),
                q.getCurrentWaitlistCount(),
                q.getMaxWaitlistLimit(),
                q.getBaseFare()
        );
    }
}
