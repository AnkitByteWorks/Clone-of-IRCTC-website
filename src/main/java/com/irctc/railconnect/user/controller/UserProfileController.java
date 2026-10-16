package com.irctc.railconnect.user.controller;

import com.irctc.railconnect.common.response.ApiResponse;
import com.irctc.railconnect.user.dto.SavedPassengerRequest;
import com.irctc.railconnect.user.dto.SavedPassengerResponse;
import com.irctc.railconnect.user.security.AuthenticatedUser;
import com.irctc.railconnect.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/passengers")
    public ResponseEntity<ApiResponse<List<SavedPassengerResponse>>> getSavedPassengers(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        List<SavedPassengerResponse> passengers = userService.getSavedPassengers(currentUser.userId());
        return ResponseEntity.ok(ApiResponse.ok("Master passenger list retrieved", passengers));
    }

    @PostMapping("/passengers")
    public ResponseEntity<ApiResponse<SavedPassengerResponse>> addSavedPassenger(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody @Valid SavedPassengerRequest request
    ) {
        SavedPassengerResponse response = userService.addSavedPassenger(currentUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Passenger added to master list", response));
    }

    @DeleteMapping("/passengers/{passengerId}")
    public ResponseEntity<ApiResponse<Void>> deleteSavedPassenger(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long passengerId
    ) {
        userService.deleteSavedPassenger(currentUser.userId(), passengerId);
        return ResponseEntity.ok(ApiResponse.ok("Passenger removed from master list", null));
    }
}
