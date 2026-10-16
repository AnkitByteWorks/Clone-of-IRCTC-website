package com.irctc.railconnect.user.service;

import com.irctc.railconnect.common.exception.BusinessRuleException;
import com.irctc.railconnect.common.exception.ResourceConflictException;
import com.irctc.railconnect.common.exception.ResourceNotFoundException;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.domain.UserRole;
import com.irctc.railconnect.user.dto.AuthResponse;
import com.irctc.railconnect.user.dto.LoginRequest;
import com.irctc.railconnect.user.dto.RegisterRequest;
import com.irctc.railconnect.user.dto.SavedPassengerRequest;
import com.irctc.railconnect.user.dto.SavedPassengerResponse;
import com.irctc.railconnect.user.entity.SavedPassengerEntity;
import com.irctc.railconnect.user.entity.UserEntity;
import com.irctc.railconnect.user.repository.SavedPassengerRepository;
import com.irctc.railconnect.user.repository.UserRepository;
import com.irctc.railconnect.user.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SavedPassengerRepository savedPassengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            SavedPassengerRepository savedPassengerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.savedPassengerRepository = savedPassengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String phoneNumber = request.phoneNumber().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Email already registered: " + email);
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ResourceConflictException("Phone number already registered: " + phoneNumber);
        }

        UserEntity user = UserEntity.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ROLE_PASSENGER)
                .active(true)
                .build();

        UserEntity savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.bearer(
                token,
                jwtService.getExpirationSeconds(),
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BusinessRuleException("User account is inactive");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.bearer(
                token,
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional
    public SavedPassengerResponse addSavedPassenger(Long userId, SavedPassengerRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        long currentCount = savedPassengerRepository.countByUserId(userId);
        if (currentCount >= 6) {
            throw new BusinessRuleException("Master passenger list cannot exceed 6 passengers per user");
        }

        if (request.seniorCitizenOptIn()) {
            boolean eligible = (request.gender() == Gender.FEMALE && request.age() >= 58)
                    || (request.gender() != Gender.FEMALE && request.age() >= 60);
            if (!eligible) {
                throw new BusinessRuleException("Passenger does not qualify for Senior Citizen concession (Female >= 58, Male/Other >= 60)");
            }
        }

        SavedPassengerEntity passenger = SavedPassengerEntity.builder()
                .user(user)
                .passengerName(request.passengerName().trim())
                .age(request.age())
                .gender(request.gender())
                .berthPreference(request.berthPreference())
                .seniorCitizenOptIn(request.seniorCitizenOptIn())
                .build();

        SavedPassengerEntity saved = savedPassengerRepository.save(passenger);
        return SavedPassengerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedPassengerResponse> getSavedPassengers(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return savedPassengerRepository.findByUserIdOrderByIdAsc(userId)
                .stream()
                .map(SavedPassengerResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteSavedPassenger(Long userId, Long passengerId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        SavedPassengerEntity passenger = savedPassengerRepository.findByIdAndUserId(passengerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));

        savedPassengerRepository.delete(passenger);
    }
}
