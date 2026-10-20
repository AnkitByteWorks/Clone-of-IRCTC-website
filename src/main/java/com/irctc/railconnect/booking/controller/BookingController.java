package com.irctc.railconnect.booking.controller;

import com.irctc.railconnect.booking.dto.BookingResponse;
import com.irctc.railconnect.booking.dto.CreateBookingRequest;
import com.irctc.railconnect.booking.service.BookingService;
import com.irctc.railconnect.booking.service.TicketPdfService;
import com.irctc.railconnect.common.response.ApiResponse;
import com.irctc.railconnect.user.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final TicketPdfService ticketPdfService;

    public BookingController(BookingService bookingService, TicketPdfService ticketPdfService) {
        this.bookingService = bookingService;
        this.ticketPdfService = ticketPdfService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        BookingResponse response = bookingService.createBooking(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking created successfully", response));
    }

    @GetMapping("/pnr/{pnrNumber}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByPnr(@PathVariable String pnrNumber) {
        BookingResponse response = bookingService.getBookingByPnr(pnrNumber);
        return ResponseEntity.ok(ApiResponse.ok("Booking details retrieved", response));
    }

    @GetMapping(value = "/pnr/{pnrNumber}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable String pnrNumber) {
        BookingResponse booking = bookingService.getBookingByPnr(pnrNumber);
        byte[] pdf = ticketPdfService.generateTicketPdf(booking);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"RailConnect_Ticket_" + pnrNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<BookingResponse> bookings = bookingService.getUserBookings(user.userId());
        return ResponseEntity.ok(ApiResponse.ok("User bookings retrieved", bookings));
    }

    @PostMapping("/{pnrNumber}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String pnrNumber,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        BookingResponse response = bookingService.cancelBooking(user.userId(), pnrNumber);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", response));
    }
}
