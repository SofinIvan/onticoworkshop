package com.onticoworkshop.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.UpdateAvailabilityRequest;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.service.AvailabilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
public class AvailabilityController {

  private final AvailabilityService service;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listAvailabilities(
      @RequestParam(required = false) String organizerId) {
    return ResponseEntity.ok(Map.of("items", service.listAvailabilities(organizerId)));
  }

  @PostMapping
  public ResponseEntity<Availability> createAvailability(
      @Valid @RequestBody CreateAvailabilityRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createAvailability(request));
  }

  @GetMapping("/{availabilityId}")
  public ResponseEntity<Availability> getAvailability(@PathVariable String availabilityId) {
    return ResponseEntity.ok(service.getAvailability(availabilityId));
  }

  @PatchMapping("/{availabilityId}")
  public ResponseEntity<Availability> updateAvailability(
      @PathVariable String availabilityId,
      @Valid @RequestBody UpdateAvailabilityRequest request) {
    return ResponseEntity.ok(service.updateAvailability(availabilityId, request));
  }

  @DeleteMapping("/{availabilityId}")
  public ResponseEntity<Void> deleteAvailability(@PathVariable String availabilityId) {
    service.deleteAvailability(availabilityId);
    return ResponseEntity.noContent().build();
  }
}
