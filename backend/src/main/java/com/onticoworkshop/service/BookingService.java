package com.onticoworkshop.service;

import java.time.DayOfWeek;
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
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.AvailabilitySchedule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.DateOverride;
import com.onticoworkshop.model.OnlineCallSettings;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

  private final UserService userService;
  private final AvailabilityScheduleService scheduleService;
  private final BookingRepository bookingRepository;

  public record BookingInfo(User profile, OnlineCallSettings onlineCall) {}

  public BookingInfo getBookingInfo(String username) {
    User user = userService.getUserByUsername(username);
    OnlineCallSettings settings = userService.getOnlineCallSettings(user.getId());
    return new BookingInfo(user, settings);
  }

  public List<Booking> listBookings(String userId, String status) {
    if (status != null && !status.isBlank()) {
      return bookingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }
    return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public Booking getBooking(String bookingId) {
    return bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
  }

  @Transactional
  public Booking createBooking(CreateBookingRequest request) {
    User user = userService.getUserByUsername(request.getUsername());
    OnlineCallSettings settings = userService.getOnlineCallSettings(user.getId());

    ZonedDateTime start = ZonedDateTime.parse(request.getStart(), DateTimeFormatter.ISO_DATE_TIME);
    ZonedDateTime end = start.plusMinutes(settings.getDurationMinutes());

    String now = Instant.now().toString();
    Booking booking = new Booking(
        "bkg_" + UUID.randomUUID().toString().substring(0, 8),
        user.getId(),
        "confirmed",
        request.getGuestName(),
        request.getGuestEmail(),
        request.getGuestTimezone(),
        start.toInstant().toString(),
        end.toInstant().toString(),
        request.getGuestTimezone(),
        settings.getMeetingUrl(),
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
    OnlineCallSettings settings = userService.getOnlineCallSettings(booking.getUserId());
    ZonedDateTime start = ZonedDateTime.parse(request.getStart(), DateTimeFormatter.ISO_DATE_TIME);
    ZonedDateTime end = start.plusMinutes(settings.getDurationMinutes());

    String newBookingId = "bkg_" + UUID.randomUUID().toString().substring(0, 8);
    Booking rescheduled = new Booking(
        newBookingId,
        booking.getUserId(),
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
    booking.setRescheduledFromBookingId(bookingId);
    booking.setUpdatedAt(now);
    bookingRepository.save(booking);

    return bookingRepository.save(rescheduled);
  }

  public List<TimeSlot> listSlots(String username, String startDate, String endDate, String timezone) {
    User user = userService.getUserByUsername(username);
    OnlineCallSettings settings = userService.getOnlineCallSettings(user.getId());
    List<AvailabilitySchedule> schedules = scheduleService.listSchedules(user.getId());

    if (schedules.isEmpty()) {
      return List.of();
    }

    AvailabilitySchedule activeSchedule = schedules.get(0);
    ZoneId zoneId = ZoneId.of(timezone);
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    List<Booking> existingBookings = bookingRepository.findByUserIdAndStartBetweenOrderByStartAsc(
        user.getId(), start.atStartOfDay(zoneId).toInstant().toString(),
        end.plusDays(1).atStartOfDay(zoneId).toInstant().toString());

    List<TimeSlot> slots = new ArrayList<>();
    LocalDate current = start;
    while (!current.isAfter(end)) {
      slots.addAll(generateSlotsForDay(
          current, activeSchedule, settings, zoneId, existingBookings));
      current = current.plusDays(1);
    }

    return slots;
  }

  private List<TimeSlot> generateSlotsForDay(LocalDate date, AvailabilitySchedule schedule,
      OnlineCallSettings settings, ZoneId zoneId, List<Booking> existingBookings) {
    String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();

    List<DateOverride> overrides = schedule.getDateOverrides().stream()
        .filter(o -> o.getDate().equals(date.toString()))
        .toList();

    if (!overrides.isEmpty()) {
      DateOverride override = overrides.get(0);
      if (override.isUnavailable()) {
        return List.of();
      }
      if (override.getStartTime() != null && override.getEndTime() != null) {
        return generateSlots(date, override.getStartTime(), override.getEndTime(),
            settings, zoneId, existingBookings);
      }
    }

    AvailabilityRule rule = schedule.getRules().stream()
        .filter(r -> r.getWeekday().equals(dayOfWeek))
        .findFirst()
        .orElse(null);

    if (rule == null) {
      return List.of();
    }

    return generateSlots(date, rule.getStartTime(), rule.getEndTime(),
        settings, zoneId, existingBookings);
  }

  private List<TimeSlot> generateSlots(LocalDate date, String startTime, String endTime,
      OnlineCallSettings settings, ZoneId zoneId, List<Booking> existingBookings) {
    List<TimeSlot> slots = new ArrayList<>();
    LocalTime start = LocalTime.parse(startTime);
    LocalTime end = LocalTime.parse(endTime);
    int interval = settings.getSlotIntervalMinutes();
    int duration = settings.getDurationMinutes();

    LocalTime slotStart = start;
    while (!slotStart.isAfter(end.minusMinutes(duration))) {
      ZonedDateTime startZoned = ZonedDateTime.of(date, slotStart, zoneId);

      Instant now = Instant.now();
      if (startZoned.toInstant().isBefore(now.plus(java.time.Duration.ofMinutes(settings.getMinimumNoticeMinutes())))) {
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
}
