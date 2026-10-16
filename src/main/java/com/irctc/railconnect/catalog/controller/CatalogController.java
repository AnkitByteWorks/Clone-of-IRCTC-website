package com.irctc.railconnect.catalog.controller;

import com.irctc.railconnect.catalog.document.TrainScheduleDocument;
import com.irctc.railconnect.catalog.dto.StationRequest;
import com.irctc.railconnect.catalog.dto.StationResponse;
import com.irctc.railconnect.catalog.dto.TrainScheduleRequest;
import com.irctc.railconnect.catalog.dto.TrainSearchResultDto;
import com.irctc.railconnect.catalog.service.CatalogService;
import com.irctc.railconnect.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/stations/search")
    public ResponseEntity<ApiResponse<List<StationResponse>>> searchStations(
            @RequestParam("q") String query
    ) {
        List<StationResponse> stations = catalogService.searchStations(query);
        return ResponseEntity.ok(ApiResponse.ok("Stations retrieved successfully", stations));
    }

    @PostMapping("/stations")
    public ResponseEntity<ApiResponse<StationResponse>> createStation(
            @Valid @RequestBody StationRequest request
    ) {
        StationResponse response = catalogService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Station created successfully", response));
    }

    @GetMapping("/trains/{trainNumber}")
    public ResponseEntity<ApiResponse<TrainScheduleDocument>> getTrainByNumber(
            @PathVariable String trainNumber
    ) {
        TrainScheduleDocument train = catalogService.getTrainByNumber(trainNumber);
        return ResponseEntity.ok(ApiResponse.ok("Train schedule retrieved successfully", train));
    }

    @GetMapping("/trains/search")
    public ResponseEntity<ApiResponse<List<TrainSearchResultDto>>> searchTrains(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TrainSearchResultDto> results = catalogService.searchTrains(from, to, date);
        return ResponseEntity.ok(ApiResponse.ok("Train schedules retrieved successfully", results));
    }

    @PostMapping("/trains")
    public ResponseEntity<ApiResponse<TrainScheduleDocument>> createTrain(
            @Valid @RequestBody TrainScheduleRequest request
    ) {
        TrainScheduleDocument created = catalogService.createTrainSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Train schedule created successfully", created));
    }
}
