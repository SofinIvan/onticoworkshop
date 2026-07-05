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

import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.UpdateMeetingRequest;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.service.MeetingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/meeting")
@RequiredArgsConstructor
public class MeetingController {

  private final MeetingService service;

  @GetMapping
  public ResponseEntity<Map<String, Object>> listMeetings(
      @RequestParam(required = false) String organizerId) {
    return ResponseEntity.ok(Map.of("items", service.listMeetings(organizerId)));
  }

  @PostMapping
  public ResponseEntity<Meeting> createMeeting(
      @Valid @RequestBody CreateMeetingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createMeeting(request));
  }

  @GetMapping("/{meetingId}")
  public ResponseEntity<Meeting> getMeeting(@PathVariable String meetingId) {
    return ResponseEntity.ok(service.getMeeting(meetingId));
  }

  @PatchMapping("/{meetingId}")
  public ResponseEntity<Meeting> updateMeeting(
      @PathVariable String meetingId,
      @Valid @RequestBody UpdateMeetingRequest request) {
    return ResponseEntity.ok(service.updateMeeting(meetingId, request));
  }

  @DeleteMapping("/{meetingId}")
  public ResponseEntity<Void> deleteMeeting(@PathVariable String meetingId) {
    service.deleteMeeting(meetingId);
    return ResponseEntity.noContent().build();
  }
}
