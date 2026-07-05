package com.onticoworkshop.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.CreateBookingRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.exception.ConflictException;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.UserRepository;
import com.onticoworkshop.service.AvailabilityService;
import com.onticoworkshop.service.BookingService;
import com.onticoworkshop.service.MeetingService;
import com.onticoworkshop.service.UserService;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private MeetingService meetingService;

  @Mock
  private AvailabilityService availabilityService;

  @Mock
  private BookingRepository bookingRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private BookingService bookingService;

  private User buildUser(String id, String username, String email) {
    return new User(id, username, username, email, "UTC", null, null,
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
  }

  private Meeting buildMeeting(String id, String organizerId, int durationMinutes) {
    return new Meeting(id, organizerId, "Test Meeting", "desc", durationMinutes, "UTC",
        true, 30, 120, 10, 10, null, "uuid-" + id,
        "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", new ArrayList<>());
  }

  private Booking buildBooking(String id, String meetingId, String status, String guestEmail) {
    return new Booking(id, "org1", meetingId, status, "Guest", guestEmail, "UTC",
        "2024-06-01T10:00:00Z", "2024-06-01T10:30:00Z", "UTC",
        null, null, null, null, null, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
  }

  @Test
  void createBooking_shouldCreateBooking() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);
    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot("m_1", "guest@test.com", "cancelled"))
        .thenReturn(List.of());
    when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CreateBookingRequest request = new CreateBookingRequest(
        "alice", "m_1", null, "Guest Name", "guest@test.com", "UTC",
        "2024-06-10T10:00:00Z", null);

    Booking result = bookingService.createBooking(request);

    assertThat(result.getId()).startsWith("bkg_");
    assertThat(result.getStatus()).isEqualTo("confirmed");
    assertThat(result.getGuestEmail()).isEqualTo("guest@test.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void createBooking_shouldResolveMeetingByUuid() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);
    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeetingByUuid("uuid-m_1")).thenReturn(meeting);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot("m_1", "guest@test.com", "cancelled"))
        .thenReturn(List.of());
    when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CreateBookingRequest request = new CreateBookingRequest(
        "alice", null, "uuid-m_1", "Guest", "guest@test.com", "UTC",
        "2024-06-10T10:00:00Z", null);

    Booking result = bookingService.createBooking(request);

    assertThat(result.getStatus()).isEqualTo("confirmed");
    verify(meetingService).getMeetingByUuid("uuid-m_1");
  }

  @Test
  void createBooking_shouldThrowConflict_whenDuplicate() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);
    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot("m_1", "guest@test.com", "cancelled"))
        .thenReturn(List.of(buildBooking("bkg_1", "m_1", "confirmed", "guest@test.com")));

    CreateBookingRequest request = new CreateBookingRequest(
        "alice", "m_1", null, "Guest", "guest@test.com", "UTC",
        "2024-06-10T10:00:00Z", null);

    assertThatThrownBy(() -> bookingService.createBooking(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  void createBooking_shouldNotAutoCreateGuest_whenGuestExists() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);
    User existingGuest = buildUser("usr_g1", "guest_user", "guest@test.com");
    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot("m_1", "guest@test.com", "cancelled"))
        .thenReturn(List.of());
    when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.of(existingGuest));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CreateBookingRequest request = new CreateBookingRequest(
        "alice", "m_1", null, "Guest", "guest@test.com", "UTC",
        "2024-06-10T10:00:00Z", null);

    bookingService.createBooking(request);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void cancelBooking_shouldCancel() {
    Booking booking = buildBooking("bkg_1", "m_1", "confirmed", "a@test.com");
    when(bookingRepository.findById("bkg_1")).thenReturn(Optional.of(booking));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Booking result = bookingService.cancelBooking("bkg_1", new CancelBookingRequest("No longer needed"));

    assertThat(result.getStatus()).isEqualTo("cancelled");
    assertThat(result.getCancellationReason()).isEqualTo("No longer needed");
  }

  @Test
  void cancelBooking_shouldThrowConflict_whenAlreadyCancelled() {
    Booking booking = buildBooking("bkg_1", "m_1", "cancelled", "a@test.com");
    when(bookingRepository.findById("bkg_1")).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.cancelBooking("bkg_1", new CancelBookingRequest()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already cancelled");
  }

  @Test
  void rescheduleBooking_shouldCreateNewAndMarkOld() {
    Booking booking = buildBooking("bkg_1", "m_1", "confirmed", "a@test.com");
    Meeting meeting = buildMeeting("m_1", "org1", 30);
    when(bookingRepository.findById("bkg_1")).thenReturn(Optional.of(booking));
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

    RescheduleBookingRequest request = new RescheduleBookingRequest(
        "2024-06-15T14:00:00Z", "UTC", "Rescheduled note");

    Booking result = bookingService.rescheduleBooking("bkg_1", request);

    assertThat(result.getStatus()).isEqualTo("confirmed");
    assertThat(result.getRescheduledFromBookingId()).isEqualTo("bkg_1");
    verify(bookingRepository, times(2)).save(any(Booking.class));
  }

  @Test
  void rescheduleBooking_shouldThrowConflict_whenAlreadyCancelled() {
    Booking booking = buildBooking("bkg_1", "m_1", "cancelled", "a@test.com");
    when(bookingRepository.findById("bkg_1")).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> bookingService.rescheduleBooking("bkg_1",
        new RescheduleBookingRequest("2024-06-15T14:00:00Z", null, null)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("cancelled");
  }

  @Test
  void listSlots_shouldReturnEmpty_whenNoAvailability() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);
    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(availabilityService.listAvailabilities("usr_1")).thenReturn(List.of());

    List<BookingService.TimeSlot> slots = bookingService.listSlots(
        "alice", "m_1", "2024-06-10", "2024-06-12", "UTC");

    assertThat(slots).isEmpty();
  }

  @Test
  void listSlots_shouldGenerateSlots_withAvailabilityRules() {
    User user = buildUser("usr_1", "alice", "alice@test.com");
    Meeting meeting = buildMeeting("m_1", "usr_1", 30);

    Availability availability = new Availability("av_1", "usr_1", "Working hours", "UTC",
        List.of(new AvailabilityRule(1L, "monday", "09:00", "17:00")),
        List.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");

    when(userService.getUserByUsername("alice")).thenReturn(user);
    when(meetingService.getMeeting("m_1")).thenReturn(meeting);
    when(availabilityService.listAvailabilities("usr_1")).thenReturn(List.of(availability));
    when(bookingRepository.findByOrganizerIdAndStartBetweenOrderByStartAsc(
        anyString(), anyString(), anyString())).thenReturn(List.of());

    LocalDate monday = LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));

    List<BookingService.TimeSlot> slots = bookingService.listSlots(
        "alice", "m_1", monday.toString(), monday.toString(), "UTC");

    assertThat(slots).isNotEmpty();
    slots.forEach(s -> assertThat(s.timezone()).isEqualTo("UTC"));
  }
}
