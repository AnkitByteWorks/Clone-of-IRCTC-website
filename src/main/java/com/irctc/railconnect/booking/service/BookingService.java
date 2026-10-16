package com.irctc.railconnect.booking.service;

import com.irctc.railconnect.booking.config.BookingProperties;
import com.irctc.railconnect.booking.domain.BookingStatus;
import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.booking.domain.PaymentStatus;
import com.irctc.railconnect.booking.dto.BookingResponse;
import com.irctc.railconnect.booking.dto.CreateBookingRequest;
import com.irctc.railconnect.booking.dto.PassengerBookingInput;
import com.irctc.railconnect.booking.dto.PassengerTicketDto;
import com.irctc.railconnect.booking.entity.BookedPassengerEntity;
import com.irctc.railconnect.booking.entity.BookingEntity;
import com.irctc.railconnect.booking.entity.PaymentTransactionEntity;
import com.irctc.railconnect.booking.repository.BookedPassengerRepository;
import com.irctc.railconnect.booking.repository.BookingRepository;
import com.irctc.railconnect.booking.repository.PaymentTransactionRepository;
import com.irctc.railconnect.catalog.dto.ClassFareDto;
import com.irctc.railconnect.catalog.dto.TrainSearchResultDto;
import com.irctc.railconnect.catalog.service.CatalogService;
import com.irctc.railconnect.common.exception.BusinessRuleException;
import com.irctc.railconnect.common.exception.ResourceNotFoundException;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.inventory.domain.SeatStatus;
import com.irctc.railconnect.inventory.entity.CoachSeatEntity;
import com.irctc.railconnect.inventory.entity.DailyTrainQuotaEntity;
import com.irctc.railconnect.inventory.repository.CoachSeatRepository;
import com.irctc.railconnect.inventory.repository.DailyTrainQuotaRepository;
import com.irctc.railconnect.inventory.service.InventoryService;
import com.irctc.railconnect.inventory.service.SeatLockService;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.UserRepository;
import com.irctc.railconnect.notification.domain.NotificationEventType;
import com.irctc.railconnect.notification.event.BookingNotificationEvent;
import com.irctc.railconnect.notification.service.BookingEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookedPassengerRepository bookedPassengerRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final DailyTrainQuotaRepository dailyTrainQuotaRepository;
    private final CoachSeatRepository coachSeatRepository;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final SeatLockService seatLockService;
    private final BookingProperties bookingProperties;
    private final BookingEventProducer bookingEventProducer;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            BookedPassengerRepository bookedPassengerRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            DailyTrainQuotaRepository dailyTrainQuotaRepository,
            CoachSeatRepository coachSeatRepository,
            CatalogService catalogService,
            InventoryService inventoryService,
            SeatLockService seatLockService,
            BookingProperties bookingProperties,
            BookingEventProducer bookingEventProducer,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.bookedPassengerRepository = bookedPassengerRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.dailyTrainQuotaRepository = dailyTrainQuotaRepository;
        this.coachSeatRepository = coachSeatRepository;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.seatLockService = seatLockService;
        this.bookingProperties = bookingProperties;
        this.bookingEventProducer = bookingEventProducer;
        this.clock = clock;
    }

    public void validateTatkalWindowIfRequired(LocalDate journeyDate, String classCode, QuotaType quotaType) {
        if (quotaType == QuotaType.TATKAL && bookingProperties.enforceTatkalTimeWindow()) {
            LocalDate tatkalOpenDate = journeyDate.minusDays(bookingProperties.tatkalAdvanceDays());
            LocalTime openTime = bookingProperties.nonAcClasses().contains(classCode.toUpperCase())
                    ? LocalTime.parse(bookingProperties.tatkalNonAcOpenTime())
                    : LocalTime.parse(bookingProperties.tatkalAcOpenTime());

            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime openDateTime = LocalDateTime.of(tatkalOpenDate, openTime);

            if (now.isBefore(openDateTime) || now.toLocalDate().isAfter(journeyDate)) {
                throw new BusinessRuleException("Tatkal booking for class " + classCode + " opens on " + tatkalOpenDate + " at " + openTime);
            }
        }
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        // 1. Enforce passenger limits
        int passengerCount = request.passengers().size();
        if (request.quotaType() == QuotaType.TATKAL && passengerCount > bookingProperties.maxTatkalPassengersPerPnr()) {
            throw new BusinessRuleException("Tatkal booking cannot exceed " + bookingProperties.maxTatkalPassengersPerPnr() + " passengers per PNR");
        }
        if (passengerCount > bookingProperties.maxGeneralPassengersPerPnr()) {
            throw new BusinessRuleException("Booking cannot exceed " + bookingProperties.maxGeneralPassengersPerPnr() + " passengers per PNR");
        }

        // 2. Validate Tatkal time window if required
        validateTatkalWindowIfRequired(request.journeyDate(), request.classCode(), request.quotaType());

        // 3. User verification
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 4. Validate train, route, and calculate fare
        TrainSearchResultDto trainResult = catalogService
                .searchTrains(request.fromStationCode(), request.toStationCode(), request.journeyDate())
                .stream()
                .filter(t -> t.trainNumber().equalsIgnoreCase(request.trainNumber()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Train " + request.trainNumber() + " not found between "
                        + request.fromStationCode() + " and " + request.toStationCode() + " on " + request.journeyDate()));

        ClassFareDto classFare = trainResult.availableClasses().stream()
                .filter(c -> c.classCode().equalsIgnoreCase(request.classCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Class " + request.classCode() + " is not available on train " + request.trainNumber()));

        BigDecimal farePerPassenger = (request.quotaType() == QuotaType.TATKAL)
                ? classFare.tatkalFare()
                : classFare.generalFare();
        BigDecimal totalFare = farePerPassenger.multiply(BigDecimal.valueOf(passengerCount));

        // 5. Ensure inventory is initialized & lock quota row
        inventoryService.ensureDailyInventoryInitialized(request.trainNumber(), request.journeyDate());

        DailyTrainQuotaEntity quota = dailyTrainQuotaRepository.findAndLockQuota(
                request.trainNumber(), request.journeyDate(), request.classCode().toUpperCase(), request.quotaType()
        ).orElseThrow(() -> new ResourceNotFoundException("Quota not found for train " + request.trainNumber()
                + ", class " + request.classCode() + ", quota " + request.quotaType()));

        int remainingCapacity = quota.getAvailableConfirmedSeats()
                + quota.getAvailableRacSlots()
                + (quota.getMaxWaitlistLimit() - quota.getCurrentWaitlistCount());

        if (remainingCapacity < passengerCount) {
            throw new BusinessRuleException("Insufficient quota capacity. Required: " + passengerCount + ", Available: " + remainingCapacity);
        }

        // 6. Build BookingEntity
        String pnr = generateUniquePnr();
        BookingEntity booking = BookingEntity.builder()
                .pnrNumber(pnr)
                .user(user)
                .trainNumber(trainResult.trainNumber())
                .trainName(trainResult.trainName())
                .fromStationCode(request.fromStationCode().trim().toUpperCase())
                .toStationCode(request.toStationCode().trim().toUpperCase())
                .journeyDate(request.journeyDate())
                .departureTime(trainResult.departureTime())
                .arrivalTime(trainResult.arrivalTime())
                .classCode(request.classCode().trim().toUpperCase())
                .quotaType(request.quotaType())
                .totalFare(totalFare)
                .build();

        // 7. Allocate each passenger
        int cnfCount = 0;
        int racCount = 0;
        int wlCount = 0;

        for (PassengerBookingInput input : request.passengers()) {
            if (quota.getAvailableConfirmedSeats() > 0) {
                CoachSeatEntity assignedSeat = null;

                // 7a. Check locked seat if specified
                if (input.lockedCoachNumber() != null && input.lockedSeatNumber() != null) {
                    Optional<CoachSeatEntity> lockedOpt = coachSeatRepository
                            .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
                                    request.trainNumber(), request.journeyDate(), request.classCode()
                            ).stream()
                            .filter(s -> s.getCoachNumber().equalsIgnoreCase(input.lockedCoachNumber())
                                    && s.getSeatNumber() == input.lockedSeatNumber()
                                    && (s.getStatus() == SeatStatus.AVAILABLE
                                    || (s.getStatus() == SeatStatus.LOCKED && user.getEmail().equalsIgnoreCase(s.getLockedByUserEmail()))))
                            .findFirst();

                    if (lockedOpt.isPresent()) {
                        assignedSeat = lockedOpt.get();
                        seatLockService.releaseSeatLock(
                                request.trainNumber(), request.journeyDate(), assignedSeat.getCoachNumber(), assignedSeat.getSeatNumber(), user.getEmail()
                        );
                    }
                }

                // 7b. Preferred berth lookup
                if (assignedSeat == null && input.berthPreference() != BerthPreference.NO_PREFERENCE) {
                    try {
                        BerthType preferredType = BerthType.valueOf(input.berthPreference().name());
                        assignedSeat = coachSeatRepository
                                .findFirstByTrainNumberAndJourneyDateAndClassCodeAndBerthTypeAndStatusOrderByCoachNumberAscSeatNumberAsc(
                                        request.trainNumber(), request.journeyDate(), request.classCode(), preferredType, SeatStatus.AVAILABLE
                                ).orElse(null);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                // 7c. Fallback to any available seat
                if (assignedSeat == null) {
                    assignedSeat = coachSeatRepository
                            .findFirstByTrainNumberAndJourneyDateAndClassCodeAndStatusOrderByCoachNumberAscSeatNumberAsc(
                                    request.trainNumber(), request.journeyDate(), request.classCode(), SeatStatus.AVAILABLE
                            ).orElse(null);
                }

                if (assignedSeat != null) {
                    assignedSeat.setStatus(SeatStatus.BOOKED);
                    assignedSeat.setLockedByUserEmail(null);
                    assignedSeat.setLockExpiresAt(null);
                    coachSeatRepository.save(assignedSeat);

                    quota.setAvailableConfirmedSeats(quota.getAvailableConfirmedSeats() - 1);
                    String details = "CNF/" + assignedSeat.getCoachNumber() + "/" + assignedSeat.getSeatNumber() + "/" + assignedSeat.getBerthType();

                    BookedPassengerEntity p = BookedPassengerEntity.builder()
                            .passengerName(input.passengerName().trim())
                            .age(input.age())
                            .gender(input.gender())
                            .berthPreference(input.berthPreference())
                            .bookingStatus(PassengerTicketStatus.CNF)
                            .bookingStatusDetails(details)
                            .currentStatus(PassengerTicketStatus.CNF)
                            .currentCoach(assignedSeat.getCoachNumber())
                            .currentSeatNumber(assignedSeat.getSeatNumber())
                            .currentBerthType(assignedSeat.getBerthType())
                            .currentSequenceNumber(null)
                            .build();
                    booking.addPassenger(p);
                    cnfCount++;
                } else {
                    // Fallback to RAC if physical seats in DB were exhausted
                    if (quota.getAvailableRacSlots() > 0) {
                        int racNum = quota.getTotalRacSlots() - quota.getAvailableRacSlots() + 1;
                        quota.setAvailableRacSlots(quota.getAvailableRacSlots() - 1);
                        BookedPassengerEntity p = BookedPassengerEntity.builder()
                                .passengerName(input.passengerName().trim())
                                .age(input.age())
                                .gender(input.gender())
                                .berthPreference(input.berthPreference())
                                .bookingStatus(PassengerTicketStatus.RAC)
                                .bookingStatusDetails("RAC/" + racNum)
                                .currentStatus(PassengerTicketStatus.RAC)
                                .currentSequenceNumber(racNum)
                                .build();
                        booking.addPassenger(p);
                        racCount++;
                    } else {
                        int wlNum = quota.getCurrentWaitlistCount() + 1;
                        quota.setCurrentWaitlistCount(wlNum);
                        BookedPassengerEntity p = BookedPassengerEntity.builder()
                                .passengerName(input.passengerName().trim())
                                .age(input.age())
                                .gender(input.gender())
                                .berthPreference(input.berthPreference())
                                .bookingStatus(PassengerTicketStatus.WL)
                                .bookingStatusDetails("WL/" + wlNum)
                                .currentStatus(PassengerTicketStatus.WL)
                                .currentSequenceNumber(wlNum)
                                .build();
                        booking.addPassenger(p);
                        wlCount++;
                    }
                }
            } else if (quota.getAvailableRacSlots() > 0) {
                int racNum = quota.getTotalRacSlots() - quota.getAvailableRacSlots() + 1;
                quota.setAvailableRacSlots(quota.getAvailableRacSlots() - 1);
                BookedPassengerEntity p = BookedPassengerEntity.builder()
                        .passengerName(input.passengerName().trim())
                        .age(input.age())
                        .gender(input.gender())
                        .berthPreference(input.berthPreference())
                        .bookingStatus(PassengerTicketStatus.RAC)
                        .bookingStatusDetails("RAC/" + racNum)
                        .currentStatus(PassengerTicketStatus.RAC)
                        .currentSequenceNumber(racNum)
                        .build();
                booking.addPassenger(p);
                racCount++;
            } else if (quota.getCurrentWaitlistCount() < quota.getMaxWaitlistLimit()) {
                int wlNum = quota.getCurrentWaitlistCount() + 1;
                quota.setCurrentWaitlistCount(wlNum);
                BookedPassengerEntity p = BookedPassengerEntity.builder()
                        .passengerName(input.passengerName().trim())
                        .age(input.age())
                        .gender(input.gender())
                        .berthPreference(input.berthPreference())
                        .bookingStatus(PassengerTicketStatus.WL)
                        .bookingStatusDetails("WL/" + wlNum)
                        .currentStatus(PassengerTicketStatus.WL)
                        .currentSequenceNumber(wlNum)
                        .build();
                booking.addPassenger(p);
                wlCount++;
            } else {
                throw new BusinessRuleException("REGRET: Quota full");
            }
        }

        // 8. Overall booking status
        BookingStatus status;
        if (cnfCount == passengerCount) {
            status = BookingStatus.CONFIRMED;
        } else if (cnfCount > 0) {
            status = BookingStatus.PARTIALLY_CONFIRMED;
        } else if (racCount == passengerCount) {
            status = BookingStatus.RAC;
        } else {
            status = BookingStatus.WAITLISTED;
        }
        booking.setBookingStatus(status);

        dailyTrainQuotaRepository.save(quota);
        BookingEntity savedBooking = bookingRepository.save(booking);

        // 9. Payment transaction ledger
        PaymentTransactionEntity payment = PaymentTransactionEntity.builder()
                .booking(savedBooking)
                .transactionReference("TXN-" + System.currentTimeMillis() + "-" + pnr)
                .paymentMethod(request.paymentMethod())
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(totalFare)
                .build();
        PaymentTransactionEntity savedPayment = paymentTransactionRepository.save(payment);

        log.info("Booking created successfully: PNR={}, status={}, passengers={}", pnr, status, passengerCount);

        // 10. Publish BOOKING_CONFIRMED event
        String passengerSummary = savedBooking.getPassengers().stream()
                .map(p -> p.getPassengerName() + " (" + p.getBookingStatusDetails() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        BookingNotificationEvent confirmedEvent = new BookingNotificationEvent(
                UUID.randomUUID().toString(),
                NotificationEventType.BOOKING_CONFIRMED,
                savedBooking.getUser().getId(),
                savedBooking.getUser().getEmail(),
                savedBooking.getPnrNumber(),
                savedBooking.getTrainNumber(),
                savedBooking.getTrainName(),
                savedBooking.getJourneyDate(),
                "Booking Confirmed - PNR " + savedBooking.getPnrNumber(),
                String.format("Your booking for train %s (%s) on %s is confirmed. PNR: %s. Total Fare: ₹%s. Passengers: %s.",
                        savedBooking.getTrainNumber(), savedBooking.getTrainName(), savedBooking.getJourneyDate(),
                        savedBooking.getPnrNumber(), savedBooking.getTotalFare(), passengerSummary),
                clock.instant()
        );
        bookingEventProducer.publishEvent(confirmedEvent);

        return mapToBookingResponse(savedBooking, savedPayment);
    }

    @Transactional
    public BookingResponse cancelBooking(Long userId, String pnrNumber) {
        BookingEntity booking = bookingRepository.findAndLockByPnrNumber(pnrNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnrNumber));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Unauthorized to cancel booking with PNR: " + pnrNumber);
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }

        DailyTrainQuotaEntity quota = dailyTrainQuotaRepository.findAndLockQuota(
                booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType()
        ).orElseThrow(() -> new ResourceNotFoundException("Quota not found for train " + booking.getTrainNumber()));

        for (BookedPassengerEntity passenger : booking.getPassengers()) {
            if (passenger.getCurrentStatus() == PassengerTicketStatus.CAN) {
                continue;
            }

            PassengerTicketStatus prevStatus = passenger.getCurrentStatus();
            String vacatedCoach = passenger.getCurrentCoach();
            Integer vacatedSeatNum = passenger.getCurrentSeatNumber();
            BerthType vacatedBerthType = passenger.getCurrentBerthType();

            // Mark cancelled
            passenger.setCurrentStatus(PassengerTicketStatus.CAN);
            passenger.setCurrentCoach(null);
            passenger.setCurrentSeatNumber(null);
            passenger.setCurrentBerthType(null);
            passenger.setCurrentSequenceNumber(null);
            bookedPassengerRepository.save(passenger);

            if (prevStatus == PassengerTicketStatus.CNF) {
                // Check if RAC passengers exist to promote
                List<BookedPassengerEntity> racQueue = bookedPassengerRepository.findPassengersForPromotion(
                        booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.RAC
                );

                if (!racQueue.isEmpty()) {
                    BookedPassengerEntity promotedRac = racQueue.get(0);
                    promotedRac.setCurrentStatus(PassengerTicketStatus.CNF);
                    promotedRac.setCurrentCoach(vacatedCoach);
                    promotedRac.setCurrentSeatNumber(vacatedSeatNum);
                    promotedRac.setCurrentBerthType(vacatedBerthType);
                    promotedRac.setCurrentSequenceNumber(null);
                    bookedPassengerRepository.save(promotedRac);
                    updateBookingStatusAfterPromotion(promotedRac.getBooking());

                    BookingNotificationEvent promoRacEvent = new BookingNotificationEvent(
                            UUID.randomUUID().toString(),
                            NotificationEventType.PASSENGER_PROMOTED,
                            promotedRac.getBooking().getUser().getId(),
                            promotedRac.getBooking().getUser().getEmail(),
                            promotedRac.getBooking().getPnrNumber(),
                            promotedRac.getBooking().getTrainNumber(),
                            promotedRac.getBooking().getTrainName(),
                            promotedRac.getBooking().getJourneyDate(),
                            "Ticket Status Upgraded - PNR " + promotedRac.getBooking().getPnrNumber(),
                            String.format("Passenger %s on Train %s (%s) has been upgraded from RAC to CNF (Coach %s, Seat %d [%s]).",
                                    promotedRac.getPassengerName(), promotedRac.getBooking().getTrainNumber(),
                                    promotedRac.getBooking().getTrainName(), vacatedCoach, vacatedSeatNum, vacatedBerthType),
                            clock.instant()
                    );
                    bookingEventProducer.publishEvent(promoRacEvent);

                    // Re-number remaining RAC (1..N-1)
                    for (int i = 1; i < racQueue.size(); i++) {
                        BookedPassengerEntity p = racQueue.get(i);
                        p.setCurrentSequenceNumber(i);
                        bookedPassengerRepository.save(p);
                    }

                    // Check if WL can be promoted to RAC
                    List<BookedPassengerEntity> wlQueue = bookedPassengerRepository.findPassengersForPromotion(
                            booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.WL
                    );

                    if (!wlQueue.isEmpty()) {
                        BookedPassengerEntity promotedWl = wlQueue.get(0);
                        promotedWl.setCurrentStatus(PassengerTicketStatus.RAC);
                        promotedWl.setCurrentSequenceNumber(racQueue.size()); // new sequence at tail of remaining RAC
                        bookedPassengerRepository.save(promotedWl);
                        updateBookingStatusAfterPromotion(promotedWl.getBooking());

                        BookingNotificationEvent promoWlEvent = new BookingNotificationEvent(
                                UUID.randomUUID().toString(),
                                NotificationEventType.PASSENGER_PROMOTED,
                                promotedWl.getBooking().getUser().getId(),
                                promotedWl.getBooking().getUser().getEmail(),
                                promotedWl.getBooking().getPnrNumber(),
                                promotedWl.getBooking().getTrainNumber(),
                                promotedWl.getBooking().getTrainName(),
                                promotedWl.getBooking().getJourneyDate(),
                                "Ticket Status Upgraded - PNR " + promotedWl.getBooking().getPnrNumber(),
                                String.format("Passenger %s on Train %s (%s) has been upgraded from WL to RAC/%d.",
                                        promotedWl.getPassengerName(), promotedWl.getBooking().getTrainNumber(),
                                        promotedWl.getBooking().getTrainName(), promotedWl.getCurrentSequenceNumber()),
                                clock.instant()
                        );
                        bookingEventProducer.publishEvent(promoWlEvent);

                        // Re-number remaining WL
                        for (int i = 1; i < wlQueue.size(); i++) {
                            BookedPassengerEntity p = wlQueue.get(i);
                            p.setCurrentSequenceNumber(i);
                            bookedPassengerRepository.save(p);
                        }
                        quota.setCurrentWaitlistCount(quota.getCurrentWaitlistCount() - 1);
                    } else {
                        quota.setAvailableRacSlots(quota.getAvailableRacSlots() + 1);
                    }
                } else {
                    // No RAC passengers. Check if WL passengers exist (e.g. TATKAL quota)
                    List<BookedPassengerEntity> wlQueue = bookedPassengerRepository.findPassengersForPromotion(
                            booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.WL
                    );

                    if (!wlQueue.isEmpty()) {
                        BookedPassengerEntity promotedWl = wlQueue.get(0);
                        promotedWl.setCurrentStatus(PassengerTicketStatus.CNF);
                        promotedWl.setCurrentCoach(vacatedCoach);
                        promotedWl.setCurrentSeatNumber(vacatedSeatNum);
                        promotedWl.setCurrentBerthType(vacatedBerthType);
                        promotedWl.setCurrentSequenceNumber(null);
                        bookedPassengerRepository.save(promotedWl);
                        updateBookingStatusAfterPromotion(promotedWl.getBooking());

                        BookingNotificationEvent promoWlEvent = new BookingNotificationEvent(
                                UUID.randomUUID().toString(),
                                NotificationEventType.PASSENGER_PROMOTED,
                                promotedWl.getBooking().getUser().getId(),
                                promotedWl.getBooking().getUser().getEmail(),
                                promotedWl.getBooking().getPnrNumber(),
                                promotedWl.getBooking().getTrainNumber(),
                                promotedWl.getBooking().getTrainName(),
                                promotedWl.getBooking().getJourneyDate(),
                                "Ticket Status Upgraded - PNR " + promotedWl.getBooking().getPnrNumber(),
                                String.format("Passenger %s on Train %s (%s) has been upgraded from WL to CNF (Coach %s, Seat %d [%s]).",
                                        promotedWl.getPassengerName(), promotedWl.getBooking().getTrainNumber(),
                                        promotedWl.getBooking().getTrainName(), vacatedCoach, vacatedSeatNum, vacatedBerthType),
                                clock.instant()
                        );
                        bookingEventProducer.publishEvent(promoWlEvent);

                        // Re-number remaining WL
                        for (int i = 1; i < wlQueue.size(); i++) {
                            BookedPassengerEntity p = wlQueue.get(i);
                            p.setCurrentSequenceNumber(i);
                            bookedPassengerRepository.save(p);
                        }
                        quota.setCurrentWaitlistCount(quota.getCurrentWaitlistCount() - 1);
                    } else {
                        // Mark physical seat AVAILABLE and restore confirmed seat count
                        if (vacatedCoach != null && vacatedSeatNum != null) {
                            coachSeatRepository
                                    .findByTrainNumberAndJourneyDateAndClassCodeOrderByCoachNumberAscSeatNumberAsc(
                                            booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode()
                                    ).stream()
                                    .filter(s -> s.getCoachNumber().equalsIgnoreCase(vacatedCoach) && s.getSeatNumber() == vacatedSeatNum)
                                    .findFirst()
                                    .ifPresent(s -> {
                                        s.setStatus(SeatStatus.AVAILABLE);
                                        s.setLockedByUserEmail(null);
                                        s.setLockExpiresAt(null);
                                        coachSeatRepository.save(s);
                                    });
                        }
                        quota.setAvailableConfirmedSeats(quota.getAvailableConfirmedSeats() + 1);
                    }
                }
            } else if (prevStatus == PassengerTicketStatus.RAC) {
                // Re-number remaining RAC
                List<BookedPassengerEntity> racQueue = bookedPassengerRepository.findPassengersForPromotion(
                        booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.RAC
                );
                int rSeq = 1;
                for (BookedPassengerEntity p : racQueue) {
                    if (!p.getId().equals(passenger.getId())) {
                        p.setCurrentSequenceNumber(rSeq++);
                        bookedPassengerRepository.save(p);
                    }
                }

                // Promote WL to RAC if available
                List<BookedPassengerEntity> wlQueue = bookedPassengerRepository.findPassengersForPromotion(
                        booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.WL
                );

                if (!wlQueue.isEmpty()) {
                    BookedPassengerEntity promotedWl = wlQueue.get(0);
                    promotedWl.setCurrentStatus(PassengerTicketStatus.RAC);
                    promotedWl.setCurrentSequenceNumber(rSeq);
                    bookedPassengerRepository.save(promotedWl);
                    updateBookingStatusAfterPromotion(promotedWl.getBooking());

                    BookingNotificationEvent promoWlEvent = new BookingNotificationEvent(
                            UUID.randomUUID().toString(),
                            NotificationEventType.PASSENGER_PROMOTED,
                            promotedWl.getBooking().getUser().getId(),
                            promotedWl.getBooking().getUser().getEmail(),
                            promotedWl.getBooking().getPnrNumber(),
                            promotedWl.getBooking().getTrainNumber(),
                            promotedWl.getBooking().getTrainName(),
                            promotedWl.getBooking().getJourneyDate(),
                            "Ticket Status Upgraded - PNR " + promotedWl.getBooking().getPnrNumber(),
                            String.format("Passenger %s on Train %s (%s) has been upgraded from WL to RAC/%d.",
                                    promotedWl.getPassengerName(), promotedWl.getBooking().getTrainNumber(),
                                    promotedWl.getBooking().getTrainName(), promotedWl.getCurrentSequenceNumber()),
                            clock.instant()
                    );
                    bookingEventProducer.publishEvent(promoWlEvent);

                    for (int i = 1; i < wlQueue.size(); i++) {
                        BookedPassengerEntity p = wlQueue.get(i);
                        p.setCurrentSequenceNumber(i);
                        bookedPassengerRepository.save(p);
                    }
                    quota.setCurrentWaitlistCount(quota.getCurrentWaitlistCount() - 1);
                } else {
                    quota.setAvailableRacSlots(quota.getAvailableRacSlots() + 1);
                }
            } else if (prevStatus == PassengerTicketStatus.WL) {
                // Re-number remaining WL
                List<BookedPassengerEntity> wlQueue = bookedPassengerRepository.findPassengersForPromotion(
                        booking.getTrainNumber(), booking.getJourneyDate(), booking.getClassCode(), booking.getQuotaType(), PassengerTicketStatus.WL
                );
                int wSeq = 1;
                for (BookedPassengerEntity p : wlQueue) {
                    if (!p.getId().equals(passenger.getId())) {
                        p.setCurrentSequenceNumber(wSeq++);
                        bookedPassengerRepository.save(p);
                    }
                }
                quota.setCurrentWaitlistCount(quota.getCurrentWaitlistCount() - 1);
            }
        }

        // 10. Mark cancelled & calculate refund
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(clock.instant());

        BigDecimal refundAmount = booking.getTotalFare()
                .multiply(BigDecimal.valueOf(bookingProperties.cancellationRefundRatio()))
                .setScale(2, RoundingMode.HALF_UP);

        PaymentTransactionEntity payment = paymentTransactionRepository.findByBookingId(booking.getId())
                .orElse(null);
        if (payment != null) {
            payment.setRefundAmount(refundAmount);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentTransactionRepository.save(payment);
        }

        dailyTrainQuotaRepository.save(quota);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Booking cancelled: PNR={}, refundAmount={}", pnrNumber, refundAmount);

        BookingNotificationEvent cancelEvent = new BookingNotificationEvent(
                UUID.randomUUID().toString(),
                NotificationEventType.BOOKING_CANCELLED,
                savedBooking.getUser().getId(),
                savedBooking.getUser().getEmail(),
                savedBooking.getPnrNumber(),
                savedBooking.getTrainNumber(),
                savedBooking.getTrainName(),
                savedBooking.getJourneyDate(),
                "Booking Cancelled - PNR " + savedBooking.getPnrNumber(),
                String.format("Your booking for PNR %s on Train %s (%s) has been cancelled. Refund amount: ₹%s has been processed.",
                        savedBooking.getPnrNumber(), savedBooking.getTrainNumber(),
                        savedBooking.getTrainName(), refundAmount),
                clock.instant()
        );
        bookingEventProducer.publishEvent(cancelEvent);

        return mapToBookingResponse(savedBooking, payment);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnrNumber) {
        BookingEntity booking = bookingRepository.findByPnrNumber(pnrNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnrNumber));

        PaymentTransactionEntity payment = paymentTransactionRepository.findByBookingId(booking.getId())
                .orElse(null);

        return mapToBookingResponse(booking, payment);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        List<BookingEntity> bookings = bookingRepository.findByUserIdOrderByBookedAtDesc(userId);
        return bookings.stream().map(b -> {
            PaymentTransactionEntity payment = paymentTransactionRepository.findByBookingId(b.getId()).orElse(null);
            return mapToBookingResponse(b, payment);
        }).toList();
    }

    private void updateBookingStatusAfterPromotion(BookingEntity booking) {
        List<BookedPassengerEntity> passengers = booking.getPassengers();
        long cnf = passengers.stream().filter(p -> p.getCurrentStatus() == PassengerTicketStatus.CNF).count();
        long rac = passengers.stream().filter(p -> p.getCurrentStatus() == PassengerTicketStatus.RAC).count();
        long wl = passengers.stream().filter(p -> p.getCurrentStatus() == PassengerTicketStatus.WL).count();
        long active = cnf + rac + wl;

        if (active == 0) {
            return;
        }

        if (cnf == active) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
        } else if (cnf > 0) {
            booking.setBookingStatus(BookingStatus.PARTIALLY_CONFIRMED);
        } else if (rac == active) {
            booking.setBookingStatus(BookingStatus.RAC);
        } else {
            booking.setBookingStatus(BookingStatus.WAITLISTED);
        }
        bookingRepository.save(booking);
    }

    private String generateUniquePnr() {
        while (true) {
            long number = 1000000000L + (long) (ThreadLocalRandom.current().nextDouble() * 9000000000L);
            String pnr = String.valueOf(number);
            if (!bookingRepository.existsByPnrNumber(pnr)) {
                return pnr;
            }
        }
    }

    private BookingResponse mapToBookingResponse(BookingEntity b, PaymentTransactionEntity payment) {
        List<PassengerTicketDto> passengerDtos = b.getPassengers().stream().map(p -> new PassengerTicketDto(
                p.getId(),
                p.getPassengerName(),
                p.getAge(),
                p.getGender(),
                p.getBerthPreference(),
                p.getBookingStatus(),
                p.getBookingStatusDetails(),
                p.getCurrentStatus(),
                p.getCurrentStatusDetails(),
                p.getCurrentCoach(),
                p.getCurrentSeatNumber(),
                p.getCurrentBerthType()
        )).toList();

        return new BookingResponse(
                b.getId(),
                b.getPnrNumber(),
                b.getTrainNumber(),
                b.getTrainName(),
                b.getFromStationCode(),
                b.getToStationCode(),
                b.getJourneyDate(),
                b.getDepartureTime(),
                b.getArrivalTime(),
                b.getClassCode(),
                b.getQuotaType(),
                b.getBookingStatus(),
                b.getTotalFare(),
                payment != null ? payment.getTransactionReference() : null,
                payment != null ? payment.getPaymentStatus() : null,
                payment != null ? payment.getRefundAmount() : null,
                b.getBookedAt(),
                b.getCancelledAt(),
                passengerDtos
        );
    }
}
