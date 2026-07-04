package com.onticoworkshop.config;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.AvailabilitySchedule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.OnlineCallSettings;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.AvailabilityScheduleRepository;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.OnlineCallSettingsRepository;
import com.onticoworkshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final OnlineCallSettingsRepository onlineCallSettingsRepository;
  private final AvailabilityScheduleRepository availabilityScheduleRepository;
  private final BookingRepository bookingRepository;

  @Override
  public void run(String... args) {
    if (userRepository.count() > 0) {
      return;
    }

    String now = Instant.now().toString();

    User user = new User(
        "usr_01", "sofia", "Sofia Ivanova",
        "sofia@example.com", "Europe/Moscow",
        "Product strategy, calendar audits, and calm execution planning.",
        null, now, now);
    userRepository.save(user);

    AvailabilitySchedule schedule = new AvailabilitySchedule(
        "sch_01", "usr_01", "Default working hours", "Europe/Moscow",
        List.of(
            new AvailabilityRule(null, "monday", "09:00", "17:00"),
            new AvailabilityRule(null, "tuesday", "09:00", "17:00"),
            new AvailabilityRule(null, "wednesday", "10:00", "18:00"),
            new AvailabilityRule(null, "thursday", "09:00", "17:00"),
            new AvailabilityRule(null, "friday", "09:00", "15:00")),
        List.of(),
        now, now);
    availabilityScheduleRepository.save(schedule);

    OnlineCallSettings settings = new OnlineCallSettings(
        "usr_01", "sch_01", "Intro call",
        "A focused 30 minute call to define scope and next steps.",
        30, "Europe/Moscow", true, 120, 30, 10, 10,
        "https://meet.example.com/sofia", now, now);
    onlineCallSettingsRepository.save(settings);

    ZonedDateTime today = ZonedDateTime.now(ZoneId.of("UTC")).withHour(0).withMinute(0).withSecond(0).withNano(0);
    ZonedDateTime day1 = today.plusDays(1);
    ZonedDateTime day2 = today.plusDays(2);

    Booking booking1 = new Booking(
        "bkg_1024", "usr_01", "confirmed",
        "Maya Chen", "maya@example.com", "Europe/Berlin",
        day1.withHour(10).toInstant().toString(),
        day1.withHour(10).withMinute(30).toInstant().toString(),
        "Europe/Berlin",
        "https://meet.example.com/sofia", "Review onboarding flow.",
        null, null, null, now, now);
    bookingRepository.save(booking1);

    Booking booking2 = new Booking(
        "bkg_1025", "usr_01", "confirmed",
        "Ivan Petrov", "ivan@example.com", "Europe/Moscow",
        day2.withHour(12).toInstant().toString(),
        day2.withHour(12).withMinute(30).toInstant().toString(),
        "Europe/Moscow",
        null, null, null, null, null, now, now);
    bookingRepository.save(booking2);
  }
}
