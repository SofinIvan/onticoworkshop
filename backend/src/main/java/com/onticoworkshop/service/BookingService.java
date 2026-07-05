package com.onticoworkshop.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onticoworkshop.dto.CancelBookingRequest;
import com.onticoworkshop.dto.CreateBookingRequest;
import com.onticoworkshop.dto.RescheduleBookingRequest;
import com.onticoworkshop.exception.ConflictException;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.DateOverride;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.MeetingTimeRule;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final UserService userService;
  private final MeetingService meetingService;
  private final AvailabilityService availabilityService;
  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;

  public record BookingInfo(User profile, List<Meeting> meetings) {}

  public BookingInfo getBookingInfo(String username) {
    User user = userService.getUserByUsername(username);
    List<Meeting> meetings = meetingService.listMeetings(user.getId());
    return new BookingInfo(user, meetings);
  }

  public List<Booking> listBookings(String organizerId, String status) {
    if (status != null && !status.isBlank()) {
      return bookingRepository.findByOrganizerIdAndStatusOrderByCreatedAtDesc(organizerId, status);
    }
    return bookingRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId);
  }

  public Booking getBooking(String bookingId) {
    return bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
  }

  @Transactional
  public Booking createBooking(CreateBookingRequest request) {
    User user = userService.getUserByUsername(request.getUsername());

    String meetingId = request.getMeetingId();
    if (meetingId == null || meetingId.isBlank()) {
      Meeting meeting = meetingService.getMeetingByUuid(request.getMeetingUuid());
      meetingId = meeting.getId();
    }

    Meeting meeting = meetingService.getMeeting(meetingId);

    List<Booking> existing = bookingRepository.findByMeetingIdAndGuestEmailAndStatusNot(
        meeting.getId(), request.getGuestEmail(), "cancelled");
    if (!existing.isEmpty()) {
      throw new ConflictException("You are already registered for this meeting");
    }

    userRepository.findByEmail(request.getGuestEmail()).orElseGet(() -> {
      String now = Instant.now().toString();
      User guestUser = new User(
          "usr_" + UUID.randomUUID().toString().substring(0, 8),
          request.getGuestEmail().replace("@", "_").replaceAll("[^a-zA-Z0-9_]", ""),
          request.getGuestName(),
          request.getGuestEmail(),
          request.getGuestTimezone(),
          null,
          null,
          now,
          now
      );
      return userRepository.save(guestUser);
    });

    ZonedDateTime start = ZonedDateTime.parse(request.getStart(), DateTimeFormatter.ISO_DATE_TIME);
    ZonedDateTime end = start.plusMinutes(meeting.getDurationMinutes());

    String now = Instant.now().toString();
    Booking booking = new Booking(
        "bkg_" + UUID.randomUUID().toString().substring(0, 8),
        user.getId(),
        meeting.getId(),
        "confirmed",
        request.getGuestName(),
        request.getGuestEmail(),
        request.getGuestTimezone(),
        start.toInstant().toString(),
        end.toInstant().toString(),
        request.getGuestTimezone(),
        meeting.getMeetingUrl(),
        request.getNotes(),
        null,
        null,
        null,
        now,
        now
    );
    return bookingRepository.save(booking);
  }

  @Transactional
  public Booking cancelBooking(String bookingId, CancelBookingRequest request) {
    Booking booking = getBooking(bookingId);
    if ("cancelled".equals(booking.getStatus())) {
      throw new ConflictException("Booking is already cancelled");
    }
    booking.setStatus("cancelled");
    booking.setCancellationReason(request.getReason());
    booking.setCancelledAt(Instant.now().toString());
    booking.setUpdatedAt(Instant.now().toString());
    return bookingRepository.save(booking);
  }

  @Transactional
  public Booking rescheduleBooking(String bookingId, RescheduleBookingRequest request) {
    Booking booking = getBooking(bookingId);
    if ("cancelled".equals(booking.getStatus())) {
      throw new ConflictException("Cannot reschedule a cancelled booking");
    }

    String now = Instant.now().toString();
    Meeting meeting = meetingService.getMeeting(booking.getMeetingId());
    ZonedDateTime start = ZonedDateTime.parse(request.getStart(), DateTimeFormatter.ISO_DATE_TIME);
    ZonedDateTime end = start.plusMinutes(meeting.getDurationMinutes());

    String newBookingId = "bkg_" + UUID.randomUUID().toString().substring(0, 8);
    Booking rescheduled = new Booking(
        newBookingId,
        booking.getOrganizerId(),
        booking.getMeetingId(),
        "confirmed",
        booking.getGuestName(),
        booking.getGuestEmail(),
        request.getGuestTimezone() != null ? request.getGuestTimezone() : booking.getGuestTimezone(),
        start.toInstant().toString(),
        end.toInstant().toString(),
        request.getGuestTimezone() != null ? request.getGuestTimezone() : booking.getTimezone(),
        booking.getMeetingUrl(),
        request.getNotes() != null ? request.getNotes() : booking.getNotes(),
        null,
        null,
        bookingId,
        now,
        now
    );

    booking.setStatus("rescheduled");
    booking.setUpdatedAt(now);
    bookingRepository.save(booking);

    return bookingRepository.save(rescheduled);
  }

  public List<TimeSlot> listSlots(String username, String meetingId, String startDate, String endDate, String timezone) {
    User user = userService.getUserByUsername(username);
    Meeting meeting = meetingService.getMeeting(meetingId);
    List<Availability> availabilities = availabilityService.listAvailabilities(user.getId());

    if (availabilities.isEmpty()) {
      return List.of();
    }

    Availability availability = availabilities.get(0);
    ZoneId zoneId = ZoneId.of(timezone);
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    List<Booking> existingBookings = bookingRepository.findByOrganizerIdAndStartBetweenOrderByStartAsc(
        user.getId(), start.atStartOfDay(zoneId).toInstant().toString(),
        end.plusDays(1).atStartOfDay(zoneId).toInstant().toString());

    List<TimeSlot> slots = new ArrayList<>();
    LocalDate current = start;
    while (!current.isAfter(end)) {
      slots.addAll(generateSlotsForDay(
          current, availability, meeting, zoneId, existingBookings));
      current = current.plusDays(1);
    }

    return slots;
  }

  private List<TimeSlot> generateSlotsForDay(LocalDate date, Availability availability,
      Meeting meeting, ZoneId zoneId, List<Booking> existingBookings) {
    String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();

    String windowStart = null;
    String windowEnd = null;

    List<DateOverride> overrides = availability.getDateOverrides().stream()
        .filter(o -> o.getDate().equals(date.toString()))
        .toList();

    if (!overrides.isEmpty()) {
      DateOverride override = overrides.get(0);
      if (override.isUnavailable()) {
        return List.of();
      }
      if (override.getStartTime() != null && override.getEndTime() != null) {
        windowStart = override.getStartTime();
        windowEnd = override.getEndTime();
      }
    }

    if (windowStart == null) {
      AvailabilityRule rule = availability.getRules().stream()
          .filter(r -> r.getWeekday().equals(dayOfWeek))
          .findFirst()
          .orElse(null);
      if (rule == null) {
        return List.of();
      }
      windowStart = rule.getStartTime();
      windowEnd = rule.getEndTime();
    }

    if (!meeting.getTimeRules().isEmpty()) {
      MeetingTimeRule meetingRule = meeting.getTimeRules().stream()
          .filter(r -> r.getWeekday().equals(dayOfWeek))
          .findFirst()
          .orElse(null);
      if (meetingRule == null) {
        return List.of();
      }
      LocalTime avStart = LocalTime.parse(windowStart);
      LocalTime avEnd = LocalTime.parse(windowEnd);
      LocalTime mtStart = LocalTime.parse(meetingRule.getStartTime());
      LocalTime mtEnd = LocalTime.parse(meetingRule.getEndTime());

      LocalTime isectStart = avStart.isAfter(mtStart) ? avStart : mtStart;
      LocalTime isectEnd = avEnd.isBefore(mtEnd) ? avEnd : mtEnd;

      if (!isectStart.isBefore(isectEnd)) {
        return List.of();
      }
      windowStart = isectStart.toString();
      windowEnd = isectEnd.toString();
    }

    return generateSlots(date, windowStart, windowEnd, meeting, zoneId, existingBookings);
  }

  private List<TimeSlot> generateSlots(LocalDate date, String startTime, String endTime,
      Meeting meeting, ZoneId zoneId, List<Booking> existingBookings) {
    List<TimeSlot> slots = new ArrayList<>();
    LocalTime start = LocalTime.parse(startTime);
    LocalTime end = LocalTime.parse(endTime);
    int interval = meeting.getSlotIntervalMinutes();
    int duration = meeting.getDurationMinutes();

    LocalTime slotStart = start;
    while (!slotStart.isAfter(end.minusMinutes(duration))) {
      ZonedDateTime startZoned = ZonedDateTime.of(date, slotStart, zoneId);

      Instant now = Instant.now();
      if (startZoned.toInstant().isBefore(now.plus(Duration.ofMinutes(meeting.getMinimumNoticeMinutes())))) {
        slotStart = slotStart.plusMinutes(interval);
        continue;
      }

      ZonedDateTime slotEnd = startZoned.plusMinutes(duration);
      boolean isBooked = existingBookings.stream().anyMatch(b -> {
        Instant bStart = Instant.parse(b.getStart());
        Instant bEnd = Instant.parse(b.getEnd());
        return startZoned.toInstant().isBefore(bEnd) && slotEnd.toInstant().isAfter(bStart);
      });

      if (!isBooked) {
        slots.add(new TimeSlot(
            startZoned.toInstant().toString(),
            slotEnd.toInstant().toString(),
            zoneId.toString()));
      }

      slotStart = slotStart.plusMinutes(interval);
    }

    return slots;
  }

  public record TimeSlot(String start, String end, String timezone) {}

  public List<TimeSlot> listSlotsByMeeting(String organizerId, String meetingId, String startDate, String endDate, String timezone) {
    Meeting meeting = meetingService.getMeeting(meetingId);
    List<Availability> availabilities = availabilityService.listAvailabilities(organizerId);

    if (availabilities.isEmpty()) {
      return List.of();
    }

    Availability availability = availabilities.get(0);
    ZoneId zoneId = ZoneId.of(timezone);
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    List<Booking> existingBookings = bookingRepository.findByOrganizerIdAndStartBetweenOrderByStartAsc(
        organizerId, start.atStartOfDay(zoneId).toInstant().toString(),
        end.plusDays(1).atStartOfDay(zoneId).toInstant().toString());

    List<TimeSlot> slots = new ArrayList<>();
    LocalDate current = start;
    while (!current.isAfter(end)) {
      slots.addAll(generateSlotsForDay(current, availability, meeting, zoneId, existingBookings));
      current = current.plusDays(1);
    }

    return slots;
  }
}
