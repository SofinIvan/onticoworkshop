package com.onticoworkshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseRepositoryTest;
import com.onticoworkshop.model.Booking;

class BookingRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private BookingRepository bookingRepository;

  @Test
  void shouldSaveAndFindByOrganizerId() {
    Booking b1 = new Booking("bkg_t1", "org1", "m_1", "confirmed", "Alice", "alice@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");

    bookingRepository.save(b1);

    List<Booking> found = bookingRepository.findByOrganizerIdOrderByCreatedAtDesc("org1");

    assertThat(found).hasSize(1);
    assertThat(found.get(0).getGuestEmail()).isEqualTo("alice@test.com");
  }

  @Test
  void shouldFilterByStatus() {
    Booking b1 = new Booking("bkg_s1", "org1", "m_1", "confirmed", "A", "a@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    Booking b2 = new Booking("bkg_s2", "org1", "m_1", "cancelled", "B", "b@test.com",
        "UTC", "2024-06-11T10:00:00Z", "2024-06-11T10:30:00Z", "UTC",
        null, "reason", "2024-06-01T00:00:00Z", null, null,
        "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");
    bookingRepository.saveAll(List.of(b1, b2));

    List<Booking> confirmed = bookingRepository.findByOrganizerIdAndStatusOrderByCreatedAtDesc("org1", "confirmed");
    List<Booking> cancelled = bookingRepository.findByOrganizerIdAndStatusOrderByCreatedAtDesc("org1", "cancelled");

    assertThat(confirmed).hasSize(1);
    assertThat(cancelled).hasSize(1);
  }

  @Test
  void shouldFindByDateRange() {
    Booking b1 = new Booking("bkg_d1", "org1", "m_1", "confirmed", "A", "a@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    Booking b2 = new Booking("bkg_d2", "org1", "m_1", "confirmed", "B", "b@test.com",
        "UTC", "2024-06-20T10:00:00Z", "2024-06-20T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");
    bookingRepository.saveAll(List.of(b1, b2));

    List<Booking> range = bookingRepository.findByOrganizerIdAndStartBetweenOrderByStartAsc(
        "org1", "2024-06-01T00:00:00Z", "2024-06-15T00:00:00Z");

    assertThat(range).hasSize(1);
    assertThat(range.get(0).getId()).isEqualTo("bkg_d1");
  }

  @Test
  void shouldDeleteByMeetingId() {
    Booking b1 = new Booking("bkg_dm1", "org1", "m_del", "confirmed", "A", "a@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    Booking b2 = new Booking("bkg_dm2", "org1", "m_del", "confirmed", "B", "b@test.com",
        "UTC", "2024-06-11T10:00:00Z", "2024-06-11T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");
    bookingRepository.saveAll(List.of(b1, b2));

    bookingRepository.deleteByMeetingId("m_del");

    List<Booking> remaining = bookingRepository.findByOrganizerIdOrderByCreatedAtDesc("org1");
    assertThat(remaining).isEmpty();
  }

  @Test
  void shouldFindByMeetingAndGuestExcludingStatus() {
    Booking b1 = new Booking("bkg_dup1", "org1", "m_dup", "confirmed", "GuestA", "dup@test.com",
        "UTC", "2024-06-10T10:00:00Z", "2024-06-10T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    Booking b2 = new Booking("bkg_dup2", "org1", "m_dup", "cancelled", "GuestA", "dup@test.com",
        "UTC", "2024-06-11T10:00:00Z", "2024-06-11T10:30:00Z", "UTC",
        null, "reason", "2024-06-01T00:00:00Z", null, null,
        "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");
    bookingRepository.saveAll(List.of(b1, b2));

    List<Booking> active = bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot(
        "m_dup", "dup@test.com", "cancelled");

    assertThat(active).hasSize(1);
    assertThat(active.get(0).getStatus()).isEqualTo("confirmed");
  }
}
