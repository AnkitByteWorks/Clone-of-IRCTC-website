package com.irctc.railconnect.inventory.controller;

import com.irctc.railconnect.common.response.ApiResponse;
import com.irctc.railconnect.inventory.dto.QuotaAvailabilityDto;
import com.irctc.railconnect.inventory.dto.QuotaUpdateRequest;
import com.irctc.railconnect.inventory.dto.SeatDto;
import com.irctc.railconnect.inventory.dto.SeatLockRequest;
import com.irctc.railconnect.inventory.dto.SeatLockResponse;
import com.irctc.railconnect.inventory.dto.TrainAvailabilityResponse;
import com.irctc.railconnect.inventory.service.InventoryService;
import com.irctc.railconnect.user.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/trains/{trainNumber}/availability")
    public ResponseEntity<ApiResponse<TrainAvailabilityResponse>> getTrainAvailability(
            @PathVariable String trainNumber,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        TrainAvailabilityResponse response = inventoryService.getTrainAvailability(trainNumber, date);
        return ResponseEntity.ok(ApiResponse.ok("Train availability retrieved", response));
    }

    @GetMapping("/trains/{trainNumber}/seats")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getCoachSeatMap(
            @PathVariable String trainNumber,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("classCode") String classCode,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String userEmail = user != null ? user.email() : null;
        List<SeatDto> seatMap = inventoryService.getCoachSeatMap(trainNumber, date, classCode, userEmail);
        return ResponseEntity.ok(ApiResponse.ok("Coach seat map retrieved", seatMap));
    }

    @PostMapping("/seats/lock")
    public ResponseEntity<ApiResponse<SeatLockResponse>> lockSpecificSeat(
            @Valid @RequestBody SeatLockRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SeatLockResponse response = inventoryService.lockSpecificSeat(request, user.email());
        return ResponseEntity.ok(ApiResponse.ok("Seat locked successfully", response));
    }

    @PostMapping("/seats/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockSpecificSeat(
            @Valid @RequestBody SeatLockRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        inventoryService.unlockSpecificSeat(request, user.email());
        return ResponseEntity.ok(ApiResponse.ok("Seat unlocked successfully", null));
    }

    @PutMapping("/quotas")
    public ResponseEntity<ApiResponse<QuotaAvailabilityDto>> updateQuotaConfig(
            @Valid @RequestBody QuotaUpdateRequest request
    ) {
        QuotaAvailabilityDto response = inventoryService.upsertQuotaConfig(request);
        return ResponseEntity.ok(ApiResponse.ok("Quota configuration updated", response));
    }
}
