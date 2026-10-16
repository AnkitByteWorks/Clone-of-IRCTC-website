package com.irctc.railconnect.booking.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.booking.domain.BookingStatus;
import com.irctc.railconnect.booking.domain.PassengerTicketStatus;
import com.irctc.railconnect.booking.domain.PaymentMethod;
import com.irctc.railconnect.booking.domain.PaymentStatus;
import com.irctc.railconnect.booking.entity.BookedPassengerEntity;
import com.irctc.railconnect.booking.entity.BookingEntity;
import com.irctc.railconnect.booking.entity.PaymentTransactionEntity;
import com.irctc.railconnect.inventory.domain.BerthType;
import com.irctc.railconnect.inventory.domain.QuotaType;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class BookingRepositoryIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookedPassengerRepository bookedPassengerRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity createTestUser(String email, String phone) {
        UserEntity user = UserEntity.builder()
                .fullName("Booking Test User")
                .email(email)
                .phoneNumber(phone)
                .passwordHash("passwordHash123")
                .role(UserRole.ROLE_PASSENGER)
                .active(true)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("Should save booking with passengers and format booking vs current status correctly")
    void shouldSaveBookingWithPassengersAndFormatBookingVsCurrentStatusCorrectly() {
        UserEntity user = createTestUser("passenger.test@example.com", "9876500001");
        LocalDate journeyDate = LocalDate.of(2026, 11, 10);

        BookingEntity booking = BookingEntity.builder()
                .pnrNumber("2415678901")
                .user(user)
                .trainNumber("12309")
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .fromStationCode("PNBE")
                .toStationCode("NDLS")
                .journeyDate(journeyDate)
                .departureTime("19:10")
                .arrivalTime("07:40")
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .bookingStatus(BookingStatus.CONFIRMED)
                .totalFare(new BigDecimal("3100.00"))
                .build();

        BookedPassengerEntity passenger1 = BookedPassengerEntity.builder()
                .passengerName("Aarav Sharma")
                .age(32)
                .gender(Gender.MALE)
                .berthPreference(BerthPreference.LOWER)
                .bookingStatus(PassengerTicketStatus.CNF)
                .bookingStatusDetails("CNF/B1/4/LOWER")
                .currentStatus(PassengerTicketStatus.CNF)
                .currentCoach("B1")
                .currentSeatNumber(4)
                .currentBerthType(BerthType.LOWER)
                .currentSequenceNumber(null)
                .build();

        BookedPassengerEntity passenger2 = BookedPassengerEntity.builder()
                .passengerName("Meera Sharma")
                .age(29)
                .gender(Gender.FEMALE)
                .berthPreference(BerthPreference.LOWER)
                .bookingStatus(PassengerTicketStatus.WL)
                .bookingStatusDetails("WL/1")
                .currentStatus(PassengerTicketStatus.WL)
                .currentCoach(null)
                .currentSeatNumber(null)
                .currentBerthType(null)
                .currentSequenceNumber(1)
                .build();

        booking.addPassenger(passenger1);
        booking.addPassenger(passenger2);

        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);
        assertThat(savedBooking.getId()).isNotNull();
        assertThat(savedBooking.getPassengers()).hasSize(2);

        // Verify initial statuses
        BookedPassengerEntity savedP1 = savedBooking.getPassengers().get(0);
        assertThat(savedP1.getBookingStatusDetails()).isEqualTo("CNF/B1/4/LOWER");
        assertThat(savedP1.getCurrentStatusDetails()).isEqualTo("CNF/B1/4/LOWER");

        BookedPassengerEntity savedP2 = savedBooking.getPassengers().get(1);
        assertThat(savedP2.getBookingStatusDetails()).isEqualTo("WL/1");
        assertThat(savedP2.getCurrentStatusDetails()).isEqualTo("WL/1");

        // Upgrade passenger 2 in-place: WL/1 -> RAC/1
        savedP2.setCurrentStatus(PassengerTicketStatus.RAC);
        savedP2.setCurrentSequenceNumber(1);
        bookingRepository.saveAndFlush(savedBooking);

        // Verify immutable booking status vs dynamic current status
        assertThat(savedP2.getBookingStatusDetails()).isEqualTo("WL/1");
        assertThat(savedP2.getCurrentStatusDetails()).isEqualTo("RAC/1");
    }

    @Test
    @DisplayName("Should fetch waitlisted and RAC passengers in exact promotion sequence order")
    void shouldFetchWaitlistedAndRacPassengersInExactPromotionSequenceOrder() {
        UserEntity user = createTestUser("promotion.test@example.com", "9876500002");
        LocalDate journeyDate = LocalDate.of(2026, 11, 10);
        String trainNumber = "12309";
        String classCode = "3A";
        QuotaType quotaType = QuotaType.GENERAL;

        // Create 3 separate bookings inserted OUT OF ORDER: WL/3, WL/1, WL/2
        BookingEntity booking1 = BookingEntity.builder()
                .pnrNumber("1111111111")
                .user(user)
                .trainNumber(trainNumber)
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .fromStationCode("PNBE")
                .toStationCode("NDLS")
                .journeyDate(journeyDate)
                .departureTime("19:10")
                .arrivalTime("07:40")
                .classCode(classCode)
                .quotaType(quotaType)
                .bookingStatus(BookingStatus.WAITLISTED)
                .totalFare(new BigDecimal("1550.00"))
                .build();

        BookedPassengerEntity p3 = BookedPassengerEntity.builder()
                .passengerName("Passenger Three")
                .age(40)
                .gender(Gender.MALE)
                .berthPreference(BerthPreference.NO_PREFERENCE)
                .bookingStatus(PassengerTicketStatus.WL)
                .bookingStatusDetails("WL/3")
                .currentStatus(PassengerTicketStatus.WL)
                .currentSequenceNumber(3)
                .build();
        booking1.addPassenger(p3);
        bookingRepository.saveAndFlush(booking1);

        BookingEntity booking2 = BookingEntity.builder()
                .pnrNumber("2222222222")
                .user(user)
                .trainNumber(trainNumber)
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .fromStationCode("PNBE")
                .toStationCode("NDLS")
                .journeyDate(journeyDate)
                .departureTime("19:10")
                .arrivalTime("07:40")
                .classCode(classCode)
                .quotaType(quotaType)
                .bookingStatus(BookingStatus.WAITLISTED)
                .totalFare(new BigDecimal("1550.00"))
                .build();

        BookedPassengerEntity p1 = BookedPassengerEntity.builder()
                .passengerName("Passenger One")
                .age(30)
                .gender(Gender.FEMALE)
                .berthPreference(BerthPreference.NO_PREFERENCE)
                .bookingStatus(PassengerTicketStatus.WL)
                .bookingStatusDetails("WL/1")
                .currentStatus(PassengerTicketStatus.WL)
                .currentSequenceNumber(1)
                .build();
        booking2.addPassenger(p1);
        bookingRepository.saveAndFlush(booking2);

        BookingEntity booking3 = BookingEntity.builder()
                .pnrNumber("3333333333")
                .user(user)
                .trainNumber(trainNumber)
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .fromStationCode("PNBE")
                .toStationCode("NDLS")
                .journeyDate(journeyDate)
                .departureTime("19:10")
                .arrivalTime("07:40")
                .classCode(classCode)
                .quotaType(quotaType)
                .bookingStatus(BookingStatus.WAITLISTED)
                .totalFare(new BigDecimal("1550.00"))
                .build();

        BookedPassengerEntity p2 = BookedPassengerEntity.builder()
                .passengerName("Passenger Two")
                .age(25)
                .gender(Gender.MALE)
                .berthPreference(BerthPreference.NO_PREFERENCE)
                .bookingStatus(PassengerTicketStatus.WL)
                .bookingStatusDetails("WL/2")
                .currentStatus(PassengerTicketStatus.WL)
                .currentSequenceNumber(2)
                .build();
        booking3.addPassenger(p2);
        bookingRepository.saveAndFlush(booking3);

        // Fetch passengers for promotion
        List<BookedPassengerEntity> promotionQueue = bookedPassengerRepository.findPassengersForPromotion(
                trainNumber,
                journeyDate,
                classCode,
                quotaType,
                PassengerTicketStatus.WL
        );

        assertThat(promotionQueue).hasSize(3);
        // Must be in strict sequence: WL/1, WL/2, WL/3
        assertThat(promotionQueue.get(0).getCurrentSequenceNumber()).isEqualTo(1);
        assertThat(promotionQueue.get(0).getPassengerName()).isEqualTo("Passenger One");

        assertThat(promotionQueue.get(1).getCurrentSequenceNumber()).isEqualTo(2);
        assertThat(promotionQueue.get(1).getPassengerName()).isEqualTo("Passenger Two");

        assertThat(promotionQueue.get(2).getCurrentSequenceNumber()).isEqualTo(3);
        assertThat(promotionQueue.get(2).getPassengerName()).isEqualTo("Passenger Three");
    }

    @Test
    @DisplayName("Should save and find payment transaction ledger by booking and reference")
    void shouldSaveAndFindPaymentTransactionLedgerByBookingAndReference() {
        UserEntity user = createTestUser("payment.test@example.com", "9876500003");
        LocalDate journeyDate = LocalDate.of(2026, 11, 10);

        BookingEntity booking = BookingEntity.builder()
                .pnrNumber("9876543210")
                .user(user)
                .trainNumber("12309")
                .trainName("Patna New Delhi Tejas Rajdhani Express")
                .fromStationCode("PNBE")
                .toStationCode("NDLS")
                .journeyDate(journeyDate)
                .departureTime("19:10")
                .arrivalTime("07:40")
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .bookingStatus(BookingStatus.CONFIRMED)
                .totalFare(new BigDecimal("1550.00"))
                .build();

        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .booking(savedBooking)
                .transactionReference("TXN-20261110-12345")
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("1550.00"))
                .build();

        PaymentTransactionEntity savedTx = paymentTransactionRepository.saveAndFlush(transaction);
        assertThat(savedTx.getId()).isNotNull();
        assertThat(savedTx.getCreatedAt()).isNotNull();

        // 1. Lookup by bookingId
        Optional<PaymentTransactionEntity> byBooking = paymentTransactionRepository.findByBookingId(savedBooking.getId());
        assertThat(byBooking).isPresent();
        assertThat(byBooking.get().getTransactionReference()).isEqualTo("TXN-20261110-12345");
        assertThat(byBooking.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // 2. Lookup by transactionReference
        Optional<PaymentTransactionEntity> byRef = paymentTransactionRepository.findByTransactionReference("TXN-20261110-12345");
        assertThat(byRef).isPresent();
        assertThat(byRef.get().getBooking().getId()).isEqualTo(savedBooking.getId());

        // 3. Enforce unique PNR constraint
        BookingEntity duplicatePnrBooking = BookingEntity.builder()
                .pnrNumber("9876543210")
                .user(user)
                .trainNumber("12310")
                .trainName("New Delhi Patna Tejas Rajdhani Express")
                .fromStationCode("NDLS")
                .toStationCode("PNBE")
                .journeyDate(journeyDate)
                .departureTime("17:10")
                .arrivalTime("05:15")
                .classCode("3A")
                .quotaType(QuotaType.GENERAL)
                .bookingStatus(BookingStatus.CONFIRMED)
                .totalFare(new BigDecimal("1550.00"))
                .build();

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(duplicatePnrBooking))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
