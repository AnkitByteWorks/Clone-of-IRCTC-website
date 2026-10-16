package com.irctc.railconnect.user.repository;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.entity.SavedPassengerEntity;
import com.irctc.railconnect.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedPassengerRepository savedPassengerRepository;

    @Test
    @DisplayName("Should save and find user by email case-insensitively and populate audit fields")
    void shouldSaveAndFindUserByEmailIgnoreCase() {
        UserEntity user = UserEntity.builder()
                .fullName("Rahul Sharma")
                .email("rahul.sharma@example.com")
                .phoneNumber("9876543210")
                .passwordHash("$2a$12$e8m4f9d4/mockhash")
                .active(true)
                .build();

        UserEntity saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_PASSENGER);

        // Case-insensitive email lookup
        Optional<UserEntity> found = userRepository.findByEmailIgnoreCase("RAHUL.SHARMA@EXAMPLE.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFullName()).isEqualTo("Rahul Sharma");
        assertThat(found.get().getPhoneNumber()).isEqualTo("9876543210");

        // Phone and email existence checks
        assertThat(userRepository.existsByEmailIgnoreCase("rahul.sharma@example.com")).isTrue();
        assertThat(userRepository.existsByPhoneNumber("9876543210")).isTrue();
        assertThat(userRepository.existsByPhoneNumber("9999999999")).isFalse();
    }

    @Test
    @DisplayName("Should enforce unique email constraint upon saveAndFlush")
    void shouldEnforceUniqueEmailAndPhoneConstraints() {
        UserEntity user1 = UserEntity.builder()
                .fullName("First User")
                .email("duplicate@example.com")
                .phoneNumber("9123456780")
                .passwordHash("secrethash")
                .active(true)
                .build();
        userRepository.saveAndFlush(user1);

        // Attempt duplicate email with different phone
        UserEntity duplicateEmailUser = UserEntity.builder()
                .fullName("Second User")
                .email("duplicate@example.com")
                .phoneNumber("9123456781")
                .passwordHash("secrethash")
                .active(true)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicateEmailUser));
    }

    @Test
    @DisplayName("Should enforce unique phone constraint upon saveAndFlush")
    void shouldEnforceUniquePhoneConstraint() {
        UserEntity user1 = UserEntity.builder()
                .fullName("Original User")
                .email("original@example.com")
                .phoneNumber("9988776655")
                .passwordHash("secrethash")
                .active(true)
                .build();
        userRepository.saveAndFlush(user1);

        // Attempt duplicate phone with different email
        UserEntity duplicatePhoneUser = UserEntity.builder()
                .fullName("Duplicate Phone User")
                .email("other@example.com")
                .phoneNumber("9988776655")
                .passwordHash("secrethash")
                .active(true)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicatePhoneUser));
    }

    @Test
    @DisplayName("Should save and query master passengers for specific user only")
    void shouldSaveAndQueryMasterPassengersForSpecificUserOnly() {
        // Create user 1
        UserEntity user1 = userRepository.saveAndFlush(UserEntity.builder()
                .fullName("Priya Verma")
                .email("priya.verma@example.com")
                .phoneNumber("9811223344")
                .passwordHash("pwd123")
                .active(true)
                .build());

        // Create user 2
        UserEntity user2 = userRepository.saveAndFlush(UserEntity.builder()
                .fullName("Vikram Singh")
                .email("vikram.singh@example.com")
                .phoneNumber("9855667788")
                .passwordHash("pwd456")
                .active(true)
                .build());

        // Attach 2 passengers to user 1
        SavedPassengerEntity p1 = savedPassengerRepository.saveAndFlush(SavedPassengerEntity.builder()
                .user(user1)
                .passengerName("Priya Verma")
                .age(28)
                .gender(Gender.FEMALE)
                .berthPreference(BerthPreference.LOWER)
                .seniorCitizenOptIn(false)
                .build());

        SavedPassengerEntity p2 = savedPassengerRepository.saveAndFlush(SavedPassengerEntity.builder()
                .user(user1)
                .passengerName("Ananya Verma")
                .age(5)
                .gender(Gender.FEMALE)
                .berthPreference(BerthPreference.SIDE_LOWER)
                .seniorCitizenOptIn(false)
                .build());

        // Attach 1 passenger to user 2
        SavedPassengerEntity p3 = savedPassengerRepository.saveAndFlush(SavedPassengerEntity.builder()
                .user(user2)
                .passengerName("Vikram Singh")
                .age(35)
                .gender(Gender.MALE)
                .berthPreference(BerthPreference.UPPER)
                .seniorCitizenOptIn(false)
                .build());

        // Verify user 1 has exactly 2 passengers in ascending ID order
        List<SavedPassengerEntity> user1Passengers = savedPassengerRepository.findByUserIdOrderByIdAsc(user1.getId());
        assertThat(user1Passengers).hasSize(2);
        assertThat(user1Passengers.get(0).getId()).isEqualTo(p1.getId());
        assertThat(user1Passengers.get(0).getBerthPreference()).isEqualTo(BerthPreference.LOWER);
        assertThat(user1Passengers.get(1).getId()).isEqualTo(p2.getId());
        assertThat(user1Passengers.get(1).getBerthPreference()).isEqualTo(BerthPreference.SIDE_LOWER);

        // Verify counts
        assertThat(savedPassengerRepository.countByUserId(user1.getId())).isEqualTo(2);
        assertThat(savedPassengerRepository.countByUserId(user2.getId())).isEqualTo(1);

        // Security boundary check: user 2 cannot access user 1's saved passenger
        Optional<SavedPassengerEntity> unauthorizedAccess = savedPassengerRepository.findByIdAndUserId(p1.getId(), user2.getId());
        assertThat(unauthorizedAccess).isEmpty();

        // Authorized access: user 1 can access own passenger
        Optional<SavedPassengerEntity> authorizedAccess = savedPassengerRepository.findByIdAndUserId(p1.getId(), user1.getId());
        assertThat(authorizedAccess).isPresent();
        assertThat(authorizedAccess.get().getPassengerName()).isEqualTo("Priya Verma");
    }
}
