package com.irctc.railconnect.user.controller;

import com.irctc.railconnect.TestcontainersConfiguration;
import com.irctc.railconnect.user.domain.BerthPreference;
import com.irctc.railconnect.user.domain.Gender;
import com.irctc.railconnect.user.dto.LoginRequest;
import com.irctc.railconnect.user.dto.RegisterRequest;
import com.irctc.railconnect.user.dto.SavedPassengerRequest;
import com.irctc.railconnect.user.repository.SavedPassengerRepository;
import com.irctc.railconnect.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthAndMasterListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedPassengerRepository savedPassengerRepository;

    @BeforeEach
    void setUp() {
        savedPassengerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register, login, and access protected master passenger endpoints with JWT")
    void shouldRegisterLoginAndAccessProtectedMasterPassengerEndpointsWithJwt() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest(
                "Ankit Sharma",
                "ankit.sharma@example.com",
                "9876543210",
                "Password123!"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ankit.sharma@example.com"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // 2. Login to obtain access token
        LoginRequest loginRequest = new LoginRequest(
                "ankit.sharma@example.com",
                "Password123!"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.path("data").path("accessToken").asText();

        // 3. Add passenger to master list with Bearer token
        SavedPassengerRequest passengerRequest = new SavedPassengerRequest(
                "Ramesh Sharma",
                62,
                Gender.MALE,
                BerthPreference.LOWER,
                true // Eligible male senior citizen (age 62 >= 60)
        );

        MvcResult addPassengerResult = mockMvc.perform(post("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.passengerName").value("Ramesh Sharma"))
                .andExpect(jsonPath("$.data.age").value(62))
                .andExpect(jsonPath("$.data.seniorCitizenOptIn").value(true))
                .andReturn();

        JsonNode passengerNode = objectMapper.readTree(addPassengerResult.getResponse().getContentAsString());
        long passengerId = passengerNode.path("data").path("id").asLong();

        // 4. Retrieve master list and verify passenger is present
        mockMvc.perform(get("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(passengerId))
                .andExpect(jsonPath("$.data[0].passengerName").value("Ramesh Sharma"));

        // 5. Delete passenger from master list
        mockMvc.perform(delete("/api/v1/users/me/passengers/" + passengerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Passenger removed from master list"));

        // 6. Verify master list is now empty
        mockMvc.perform(get("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Should reject ineligible senior citizen and enforce 6-passenger limit")
    void shouldRejectIneligibleSeniorCitizenAndEnforceSixPassengerLimit() throws Exception {
        // Register and login user
        RegisterRequest registerRequest = new RegisterRequest(
                "Priya Verma",
                "priya.verma@example.com",
                "9812345678",
                "Password123!"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("priya.verma@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 1. Attempt to add ineligible senior citizen (Male, age 55 < 60)
        SavedPassengerRequest ineligibleMaleSenior = new SavedPassengerRequest(
                "Suresh Verma",
                55,
                Gender.MALE,
                BerthPreference.LOWER,
                true
        );

        mockMvc.perform(post("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ineligibleMaleSenior)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        // Attempt to add ineligible senior citizen (Female, age 50 < 58)
        SavedPassengerRequest ineligibleFemaleSenior = new SavedPassengerRequest(
                "Anita Verma",
                50,
                Gender.FEMALE,
                BerthPreference.LOWER,
                true
        );

        mockMvc.perform(post("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ineligibleFemaleSenior)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        // 2. Add 6 valid passengers
        for (int i = 1; i <= 6; i++) {
            SavedPassengerRequest validPassenger = new SavedPassengerRequest(
                    "Passenger " + i,
                    25 + i,
                    Gender.FEMALE,
                    BerthPreference.NO_PREFERENCE,
                    false
            );

            mockMvc.perform(post("/api/v1/users/me/passengers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPassenger)))
                    .andExpect(status().isCreated());
        }

        // Verify count is 6
        mockMvc.perform(get("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6)));

        // 3. Attempt to add 7th passenger -> reject with 400 Bad Request
        SavedPassengerRequest seventhPassenger = new SavedPassengerRequest(
                "Passenger 7",
                    30,
                    Gender.MALE,
                    BerthPreference.UPPER,
                    false
        );

        mockMvc.perform(post("/api/v1/users/me/passengers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(seventhPassenger)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("Should return 401 when accessing master list without JWT token")
    void shouldReturn401WhenAccessingMasterListWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/passengers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
