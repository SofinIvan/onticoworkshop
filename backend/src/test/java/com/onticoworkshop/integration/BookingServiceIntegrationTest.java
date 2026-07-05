package com.onticoworkshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.CreateBookingRequest;
import com.onticoworkshop.dto.CreateMeetingRequest;
import com.onticoworkshop.dto.CreateUserRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.exception.ConflictException;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.User;
import com.onticoworkshop.service.AvailabilityService;
import com.onticoworkshop.service.BookingService;
import com.onticoworkshop.service.MeetingService;
import com.onticoworkshop.service.UserService;

class BookingServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private UserService userService;

  @Autowired
  private MeetingService meetingService;

  @Autowired
  private AvailabilityService availabilityService;

  @Autowired
  private BookingService bookingService;

  @Test
  void shouldCreateBooking() {
    User user = createTestUser("org_alice", "alice_int");
    Meeting meeting = createTestMeeting(user.getId(), "Alice Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest request = new CreateBookingRequest(
        "alice_int", meeting.getId(), null, "Bob Guest", "bob@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    Booking booking = bookingService.createBooking(request);

    assertThat(booking.getId()).startsWith("bkg_");
    assertThat(booking.getStatus()).isEqualTo("confirmed");
    assertThat(booking.getGuestEmail()).isEqualTo("bob@test.com");
    assertThat(booking.getMeetingId()).isEqualTo(meeting.getId());
  }

  @Test
  void shouldPreventDuplicateBooking() {
    User user = createTestUser("org_dup", "dupe_user");
    Meeting meeting = createTestMeeting(user.getId(), "Dup Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest request = new CreateBookingRequest(
        "dupe_user", meeting.getId(), null, "Charlie", "charlie@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);

    bookingService.createBooking(request);

    assertThatThrownBy(() -> bookingService.createBooking(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  void shouldCancelBooking() {
    User user = createTestUser("org_cancel", "cancel_user");
    Meeting meeting = createTestMeeting(user.getId(), "Cancel Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest request = new CreateBookingRequest(
        "cancel_user", meeting.getId(), null, "Diana", "diana@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);
    Booking created = bookingService.createBooking(request);

    Booking cancelled = bookingService.cancelBooking(created.getId(),
        new CancelBookingRequest("Schedule changed"));

    assertThat(cancelled.getStatus()).isEqualTo("cancelled");
    assertThat(cancelled.getCancellationReason()).isEqualTo("Schedule changed");
    assertThat(cancelled.getCancelledAt()).isNotNull();
  }

  @Test
  void shouldThrowWhenCancellingAlreadyCancelled() {
    User user = createTestUser("org_cancel2", "cancel2_user");
    Meeting meeting = createTestMeeting(user.getId(), "Cancel2 Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest request = new CreateBookingRequest(
        "cancel2_user", meeting.getId(), null, "Eve", "eve@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);
    Booking created = bookingService.createBooking(request);
    bookingService.cancelBooking(created.getId(), new CancelBookingRequest("reason"));

    assertThatThrownBy(() -> bookingService.cancelBooking(created.getId(), new CancelBookingRequest()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already cancelled");
  }

  @Test
  void shouldRescheduleBooking() {
    User user = createTestUser("org_resched", "resched_user");
    Meeting meeting = createTestMeeting(user.getId(), "Resched Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest request = new CreateBookingRequest(
        "resched_user", meeting.getId(), null, "Frank", "frank@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);
    Booking original = bookingService.createBooking(request);

    RescheduleBookingRequest reschedReq = new RescheduleBookingRequest(
        "2024-07-11T14:00:00Z", "UTC", "Moved to afternoon");
    Booking rescheduled = bookingService.rescheduleBooking(original.getId(), reschedReq);

    assertThat(rescheduled.getId()).isNotEqualTo(original.getId());
    assertThat(rescheduled.getStatus()).isEqualTo("confirmed");
    assertThat(rescheduled.getRescheduledFromBookingId()).isEqualTo(original.getId());

    Booking oldBooking = bookingService.getBooking(original.getId());
    assertThat(oldBooking.getStatus()).isEqualTo("rescheduled");
  }

  @Test
  void shouldGetBookingInfo() {
    User user = createTestUser("org_info", "info_user");
    createTestMeeting(user.getId(), "Info Meeting");

    BookingService.BookingInfo info = bookingService.getBookingInfo("info_user");

    assertThat(info.profile().getUsername()).isEqualTo("info_user");
    assertThat(info.meetings()).isNotEmpty();
  }

  @Test
  void shouldListSlotsWithAvailability() {
    User user = createTestUser("org_slots", "slots_user");
    Meeting meeting = createTestMeeting(user.getId(), "Slots Meeting");
    createTestAvailability(user.getId());

    LocalDate monday = LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));

    List<BookingService.TimeSlot> slots = bookingService.listSlots(
        "slots_user", meeting.getId(),
        monday.toString(), monday.plusDays(2).toString(), "UTC");

    assertThat(slots).isNotEmpty();
    for (BookingService.TimeSlot slot : slots) {
      assertThat(slot.start()).isNotNull();
      assertThat(slot.end()).isNotNull();
      assertThat(slot.timezone()).isEqualTo("UTC");
    }
  }

  @Test
  void shouldListBookingsWithStatusFilter() {
    User user = createTestUser("org_filter", "filter_user");
    Meeting meeting = createTestMeeting(user.getId(), "Filter Meeting");
    createTestAvailability(user.getId());

    CreateBookingRequest req1 = new CreateBookingRequest(
        "filter_user", meeting.getId(), null, "G1", "g1@test.com", "UTC",
        "2024-07-10T10:00:00Z", null);
    CreateBookingRequest req2 = new CreateBookingRequest(
        "filter_user", meeting.getId(), null, "G2", "g2@test.com", "UTC",
        "2024-07-11T10:00:00Z", null);
    Booking b1 = bookingService.createBooking(req1);
    Booking b2 = bookingService.createBooking(req2);
    bookingService.cancelBooking(b2.getId(), new CancelBookingRequest("test"));

    List<Booking> confirmed = bookingService.listBookings(user.getId(), "confirmed");
    List<Booking> cancelled = bookingService.listBookings(user.getId(), "cancelled");

    assertThat(confirmed).hasSize(1);
    assertThat(cancelled).hasSize(1);
  }

  private User createTestUser(String id, String username) {
    CreateUserRequest req = new CreateUserRequest(username, username, username + "@org.com",
        "UTC", null, null);
    return userService.createUser(req);
  }

  private Meeting createTestMeeting(String organizerId, String title) {
    CreateMeetingRequest req = new CreateMeetingRequest(organizerId, title, "test desc", "UTC");
    return meetingService.createMeeting(req);
  }

  private void createTestAvailability(String organizerId) {
    List<AvailabilityRule> rules = List.of(
        new AvailabilityRule(null, "monday", "08:00", "18:00"),
        new AvailabilityRule(null, "tuesday", "08:00", "18:00"),
        new AvailabilityRule(null, "wednesday", "08:00", "18:00"),
        new AvailabilityRule(null, "thursday", "08:00", "18:00"),
        new AvailabilityRule(null, "friday", "08:00", "18:00"));
    CreateAvailabilityRequest req = new CreateAvailabilityRequest(organizerId, "Test Hours",
        "UTC", rules, new ArrayList<>());
    availabilityService.createAvailability(req);
  }
}
