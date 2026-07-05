package com.onticoworkshop.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onticoworkshop.service.BookingService;
import com.onticoworkshop.service.MeetingService;
import com.onticoworkshop.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

  private final MeetingService meetingService;
  private final UserService userService;
  private final BookingService bookingService;

  @GetMapping("/meeting")
  public ResponseEntity<?> getMeetingByUuid(@RequestParam String uuid) {
    var meeting = meetingService.getMeetingByUuid(uuid);
    var user = userService.getUser(meeting.getOrganizerId());
    return ResponseEntity.ok(Map.of("profile", user, "meeting", meeting));
  }

  @GetMapping("/slots")
  public ResponseEntity<Map<String, Object>> listSlots(
      @RequestParam String uuid,
      @RequestParam String startDate,
      @RequestParam String endDate,
      @RequestParam String timezone) {
    var meeting = meetingService.getMeetingByUuid(uuid);
    return ResponseEntity.ok(
        Map.of("items", bookingService.listSlotsByMeeting(
            meeting.getOrganizerId(), meeting.getId(), startDate, endDate, timezone)));
  }
}
