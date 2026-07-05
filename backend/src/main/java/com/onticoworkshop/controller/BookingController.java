package com.onticoworkshop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.CreateBookingRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listBookings(
      @RequestParam(required = false) String organizerId,
      @RequestParam(required = false) String status) {
    if (organizerId == null || organizerId.isBlank()) {
      return ResponseEntity.ok(Map.of("items", List.of()));
    }
    return ResponseEntity.ok(Map.of("items", bookingService.listBookings(organizerId, status)));
  }

  @GetMapping("/new")
  public ResponseEntity<BookingService.BookingInfo> getBookingInfo(
      @RequestParam String username) {
    return ResponseEntity.ok(bookingService.getBookingInfo(username));
  }

  @GetMapping("/slots")
  public ResponseEntity<Map<String, Object>> listSlots(
      @RequestParam String username,
      @RequestParam String meetingId,
      @RequestParam String startDate,
      @RequestParam String endDate,
      @RequestParam String timezone) {
    return ResponseEntity.ok(
        Map.of("items", bookingService.listSlots(username, meetingId, startDate, endDate, timezone)));
  }

  @PostMapping
  public ResponseEntity<Booking> createBooking(
      @Valid @RequestBody CreateBookingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
  }

  @GetMapping("/{bookingId}")
  public ResponseEntity<Booking> getBooking(@PathVariable String bookingId) {
    return ResponseEntity.ok(bookingService.getBooking(bookingId));
  }

  @PostMapping("/{bookingId}/cancel")
  public ResponseEntity<Booking> cancelBooking(
      @PathVariable String bookingId,
      @RequestBody CancelBookingRequest request) {
    return ResponseEntity.ok(bookingService.cancelBooking(bookingId, request));
  }

  @PostMapping("/{bookingId}/reschedule")
  public ResponseEntity<Booking> rescheduleBooking(
      @PathVariable String bookingId,
      @Valid @RequestBody RescheduleBookingRequest request) {
    return ResponseEntity.ok(bookingService.rescheduleBooking(bookingId, request));
  }
}
