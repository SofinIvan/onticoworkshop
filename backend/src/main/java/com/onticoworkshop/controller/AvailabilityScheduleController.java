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

import com.onticoworkshop.dto.CreateAvailabilityScheduleRequest;
import com.onticoworkshop.dto.UpdateAvailabilityScheduleRequest;
import com.onticoworkshop.model.AvailabilitySchedule;
import com.onticoworkshop.service.AvailabilityScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/availability-schedule")
@RequiredArgsConstructor
public class AvailabilityScheduleController {

  private final AvailabilityScheduleService service;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listSchedules(
      @RequestParam(required = false) String userId) {
    return ResponseEntity.ok(Map.of("items", service.listSchedules(userId)));
  }

  @PostMapping
  public ResponseEntity<AvailabilitySchedule> createSchedule(
      @Valid @RequestBody CreateAvailabilityScheduleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createSchedule(request));
  }

  @GetMapping("/{scheduleId}")
  public ResponseEntity<AvailabilitySchedule> getSchedule(@PathVariable String scheduleId) {
    return ResponseEntity.ok(service.getSchedule(scheduleId));
  }

  @PatchMapping("/{scheduleId}")
  public ResponseEntity<AvailabilitySchedule> updateSchedule(
      @PathVariable String scheduleId,
      @Valid @RequestBody UpdateAvailabilityScheduleRequest request) {
    return ResponseEntity.ok(service.updateSchedule(scheduleId, request));
  }

  @DeleteMapping("/{scheduleId}")
  public ResponseEntity<Void> deleteSchedule(@PathVariable String scheduleId) {
    service.deleteSchedule(scheduleId);
    return ResponseEntity.noContent().build();
  }
}
