package com.onticoworkshop.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.UpdateMeetingRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.MeetingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService {

  private final MeetingRepository repository;
  private final BookingRepository bookingRepository;

  @Transactional(readOnly = true)
  public List<Meeting> listMeetings(String organizerId) {
    if (organizerId == null || organizerId.isBlank()) {
      return repository.findAll();
    }
    List<Meeting> meetings = repository.findByOrganizerIdOrderByCreatedAtAsc(organizerId);
    meetings.forEach(m -> m.getTimeRules().size());
    return meetings;
  }

  @Transactional(readOnly = true)
  public Meeting getMeeting(String meetingId) {
    Meeting meeting = repository.findById(meetingId)
        .orElseThrow(() -> new NotFoundException("Meeting not found: " + meetingId));
    meeting.getTimeRules().size();
    return meeting;
  }

  @Transactional(readOnly = true)
  public Meeting getMeetingByUuid(String uuid) {
    Meeting meeting = repository.findByUuid(uuid)
        .orElseThrow(() -> new NotFoundException("Meeting not found for uuid: " + uuid));
    meeting.getTimeRules().size();
    return meeting;
  }

  @Transactional
  public Meeting createMeeting(CreateMeetingRequest request) {
    String now = Instant.now().toString();
    Meeting meeting = new Meeting(
        "m_" + UUID.randomUUID().toString().substring(0, 8),
        request.getOrganizerId(),
        request.getTitle(),
        request.getDescription(),
        30,
        request.getTimezone(),
        true,
        30,
        120,
        10,
        10,
        null,
        UUID.randomUUID().toString(),
        now,
        now,
        new ArrayList<>()
    );
    return repository.save(meeting);
  }

  @Transactional
  public Meeting updateMeeting(String meetingId, UpdateMeetingRequest request) {
    Meeting meeting = getMeeting(meetingId);
    if (request.getTitle() != null) meeting.setTitle(request.getTitle());
    if (request.getDescription() != null) meeting.setDescription(request.getDescription());
    if (request.getDurationMinutes() != null) meeting.setDurationMinutes(request.getDurationMinutes());
    if (request.getTimezone() != null) meeting.setTimezone(request.getTimezone());
    if (request.getIsActive() != null) meeting.setActive(request.getIsActive());
    if (request.getSlotIntervalMinutes() != null) meeting.setSlotIntervalMinutes(request.getSlotIntervalMinutes());
    if (request.getMinimumNoticeMinutes() != null) meeting.setMinimumNoticeMinutes(request.getMinimumNoticeMinutes());
    if (request.getBufferBeforeMinutes() != null) meeting.setBufferBeforeMinutes(request.getBufferBeforeMinutes());
    if (request.getBufferAfterMinutes() != null) meeting.setBufferAfterMinutes(request.getBufferAfterMinutes());
    if (request.getMeetingUrl() != null) meeting.setMeetingUrl(request.getMeetingUrl());
    if (request.getTimeRules() != null) {
      meeting.getTimeRules().clear();
      meeting.getTimeRules().addAll(request.getTimeRules());
    }
    meeting.setUpdatedAt(Instant.now().toString());
    return repository.save(meeting);
  }

  @Transactional
  public void deleteMeeting(String meetingId) {
    if (!repository.existsById(meetingId)) {
      throw new NotFoundException("Meeting not found: " + meetingId);
    }
    bookingRepository.deleteByMeetingId(meetingId);
    repository.deleteById(meetingId);
  }
}
