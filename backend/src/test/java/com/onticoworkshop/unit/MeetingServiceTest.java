package com.onticoworkshop.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.UpdateMeetingRequest;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.MeetingTimeRule;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.MeetingRepository;
import com.onticoworkshop.service.MeetingService;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock
  private MeetingRepository repository;

  @Mock
  private BookingRepository bookingRepository;

  @InjectMocks
  private MeetingService meetingService;

  private Meeting buildMeeting(String id, String organizerId, String title) {
    return new Meeting(id, organizerId, title, "desc", 30, "UTC", true, 30, 120, 10, 10,
        "https://meet.example.com/test", "uuid-" + id, "2024-01-01T00:00:00Z",
        "2024-01-01T00:00:00Z", new ArrayList<>());
  }

  @Test
  void createMeeting_shouldSetDefaults() {
    CreateMeetingRequest request = new CreateMeetingRequest("org1", "New Meeting", "desc", "UTC");
    when(repository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Meeting result = meetingService.createMeeting(request);

    assertThat(result.getId()).startsWith("m_");
    assertThat(result.getDurationMinutes()).isEqualTo(30);
    assertThat(result.isActive()).isTrue();
    assertThat(result.getSlotIntervalMinutes()).isEqualTo(30);
    assertThat(result.getMinimumNoticeMinutes()).isEqualTo(120);
    assertThat(result.getUuid()).isNotNull();
  }

  @Test
  void updateMeeting_shouldPartiallyUpdate() {
    Meeting existing = buildMeeting("m_1", "org1", "Old Title");
    when(repository.findById("m_1")).thenReturn(Optional.of(existing));
    when(repository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateMeetingRequest request = new UpdateMeetingRequest();
    request.setTitle("New Title");

    Meeting result = meetingService.updateMeeting("m_1", request);

    assertThat(result.getTitle()).isEqualTo("New Title");
    assertThat(result.getOrganizerId()).isEqualTo("org1");
  }

  @Test
  void updateMeeting_shouldClearAndReplaceTimeRules() {
    Meeting existing = buildMeeting("m_1", "org1", "Test");
    existing.getTimeRules().add(new MeetingTimeRule(1L, "monday", "09:00", "17:00"));
    when(repository.findById("m_1")).thenReturn(Optional.of(existing));
    when(repository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateMeetingRequest request = new UpdateMeetingRequest();
    request.setTimeRules(List.of(new MeetingTimeRule(null, "tuesday", "10:00", "14:00")));

    Meeting result = meetingService.updateMeeting("m_1", request);

    assertThat(result.getTimeRules()).hasSize(1);
    assertThat(result.getTimeRules().get(0).getWeekday()).isEqualTo("tuesday");
  }

  @Test
  void deleteMeeting_shouldCascadeDeleteBookings() {
    when(repository.existsById("m_1")).thenReturn(true);

    meetingService.deleteMeeting("m_1");

    verify(bookingRepository).deleteByMeetingId("m_1");
    verify(repository).deleteById("m_1");
  }
}
