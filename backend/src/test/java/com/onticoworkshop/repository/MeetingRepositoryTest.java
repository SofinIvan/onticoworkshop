package com.onticoworkshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseRepositoryTest;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.MeetingTimeRule;

class MeetingRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private MeetingRepository meetingRepository;

  @Test
  void shouldSaveMeetingWithTimeRules() {
    Meeting meeting = new Meeting("m_t1", "org1", "Test Meeting", "desc", 30,
        "UTC", true, 30, 120, 10, 10, null, "uuid-test-1",
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", new ArrayList<>());
    meeting.getTimeRules().add(new MeetingTimeRule(null, "monday", "09:00", "17:00"));
    meeting.getTimeRules().add(new MeetingTimeRule(null, "tuesday", "10:00", "14:00"));

    meetingRepository.save(meeting);

    Optional<Meeting> found = meetingRepository.findById("m_t1");
    assertThat(found).isPresent();
    assertThat(found.get().getTimeRules()).hasSize(2);
  }

  @Test
  void shouldFindByOrganizerId() {
    Meeting m1 = new Meeting("m_o1", "orgX", "M1", "desc", 30,
        "UTC", true, 30, 120, 10, 10, null, "uuid-o1",
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", new ArrayList<>());
    Meeting m2 = new Meeting("m_o2", "orgX", "M2", "desc", 30,
        "UTC", true, 30, 120, 10, 10, null, "uuid-o2",
        "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z", new ArrayList<>());
    Meeting m3 = new Meeting("m_o3", "orgY", "M3", "desc", 30,
        "UTC", true, 30, 120, 10, 10, null, "uuid-o3",
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", new ArrayList<>());

    meetingRepository.saveAll(List.of(m1, m2, m3));

    List<Meeting> result = meetingRepository.findByOrganizerIdOrderByCreatedAtAsc("orgX");

    assertThat(result).hasSize(2);
    assertThat(result).extracting("title").contains("M1", "M2");
  }

  @Test
  void shouldFindByUuid() {
    Meeting meeting = new Meeting("m_u1", "org1", "UUID Test", "desc", 30,
        "UTC", true, 30, 120, 10, 10, null, "special-uuid-123",
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", new ArrayList<>());
    meetingRepository.save(meeting);

    Optional<Meeting> found = meetingRepository.findByUuid("special-uuid-123");

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("UUID Test");
  }

  @Test
  void shouldReturnEmptyForUnknownUuid() {
    Optional<Meeting> found = meetingRepository.findByUuid("nonexistent-uuid");

    assertThat(found).isEmpty();
  }
}
