package com.onticoworkshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.UpdateMeetingRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.MeetingTimeRule;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.service.MeetingService;

class MeetingServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MeetingService meetingService;

  @Autowired
  private BookingRepository bookingRepository;

  @Test
  void shouldCreateAndRetrieveMeeting() {
    CreateMeetingRequest request = new CreateMeetingRequest("org1", "Strategy Call",
        "Quarterly review", "UTC");
    Meeting created = meetingService.createMeeting(request);

    assertThat(created.getId()).startsWith("m_");
    assertThat(created.getTitle()).isEqualTo("Strategy Call");
    assertThat(created.getDurationMinutes()).isEqualTo(30);
    assertThat(created.isActive()).isTrue();

    Meeting found = meetingService.getMeeting(created.getId());
    assertThat(found.getDescription()).isEqualTo("Quarterly review");
    assertThat(found.getUuid()).isNotNull();
  }

  @Test
  void shouldUpdateMeeting() {
    CreateMeetingRequest createReq = new CreateMeetingRequest("org1", "Old Title",
        "desc", "UTC");
    Meeting created = meetingService.createMeeting(createReq);

    UpdateMeetingRequest updateReq = new UpdateMeetingRequest();
    updateReq.setTitle("New Title");
    updateReq.setDurationMinutes(60);
    updateReq.setDescription("Updated description");

    Meeting updated = meetingService.updateMeeting(created.getId(), updateReq);

    assertThat(updated.getTitle()).isEqualTo("New Title");
    assertThat(updated.getDurationMinutes()).isEqualTo(60);
    assertThat(updated.getDescription()).isEqualTo("Updated description");
  }

  @Test
  void shouldDeleteMeetingAndCascadeBookings() {
    CreateMeetingRequest createReq = new CreateMeetingRequest("org1", "To Delete",
        "desc", "UTC");
    Meeting created = meetingService.createMeeting(createReq);

    Booking booking = new Booking("bkg_del1", "org1", created.getId(), "confirmed",
        "Guest", "guest@test.com", "UTC",
        "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    bookingRepository.save(booking);

    meetingService.deleteMeeting(created.getId());

    assertThatThrownBy(() -> meetingService.getMeeting(created.getId()))
        .isInstanceOf(NotFoundException.class);

    List<Booking> remainingBookings = bookingRepository.findByOrganizerIdOrderByCreatedAtDesc("org1");
    assertThat(remainingBookings).isEmpty();
  }

  @Test
  void shouldListMeetingsByOrganizer() {
    CreateMeetingRequest req1 = new CreateMeetingRequest("orgZ", "M1", "desc", "UTC");
    CreateMeetingRequest req2 = new CreateMeetingRequest("orgZ", "M2", "desc", "UTC");
    CreateMeetingRequest req3 = new CreateMeetingRequest("orgW", "Other", "desc", "UTC");
    meetingService.createMeeting(req1);
    meetingService.createMeeting(req2);
    meetingService.createMeeting(req3);

    List<Meeting> orgZMeetings = meetingService.listMeetings("orgZ");

    assertThat(orgZMeetings).hasSize(2);
    assertThat(orgZMeetings).extracting("title").contains("M1", "M2");
  }

  @Test
  void shouldCreateMeetingWithTimeRules() {
    CreateMeetingRequest request = new CreateMeetingRequest("org1",
        "Meeting with rules", "desc", "UTC");
    Meeting created = meetingService.createMeeting(request);

    UpdateMeetingRequest updateReq = new UpdateMeetingRequest();
    updateReq.setTimeRules(List.of(
        new MeetingTimeRule(null, "monday", "09:00", "12:00"),
        new MeetingTimeRule(null, "wednesday", "14:00", "17:00")));

    Meeting updated = meetingService.updateMeeting(created.getId(), updateReq);

    assertThat(updated.getTimeRules()).hasSize(2);
    assertThat(updated.getTimeRules()).extracting("weekday").contains("monday", "wednesday");
  }

  @Test
  void shouldReturnAllMeetingsWhenOrganizerIdNull() {
    CreateMeetingRequest req1 = new CreateMeetingRequest("orgA", "A", "desc", "UTC");
    CreateMeetingRequest req2 = new CreateMeetingRequest("orgB", "B", "desc", "UTC");
    meetingService.createMeeting(req1);
    meetingService.createMeeting(req2);

    List<Meeting> all = meetingService.listMeetings(null);

    assertThat(all).hasSizeGreaterThanOrEqualTo(2);
  }
}
