package com.onticoworkshop.config;

import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.Booking;
import com.onticoworkshop.model.Meeting;
import com.onticoworkshop.model.User;
import com.onticoworkshop.repository.AvailabilityRepository;
import com.onticoworkshop.repository.BookingRepository;
import com.onticoworkshop.repository.MeetingRepository;
import com.onticoworkshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final MeetingRepository meetingRepository;
  private final AvailabilityRepository availabilityRepository;
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

    Availability availability = new Availability(
        "av_01", "usr_01", "Default working hours", "Europe/Moscow",
        List.of(
            new AvailabilityRule(null, "monday", "09:00", "17:00"),
            new AvailabilityRule(null, "tuesday", "09:00", "17:00"),
            new AvailabilityRule(null, "wednesday", "10:00", "18:00"),
            new AvailabilityRule(null, "thursday", "09:00", "17:00"),
            new AvailabilityRule(null, "friday", "09:00", "15:00")),
        List.of(),
        now, now);
    availabilityRepository.save(availability);

    Meeting meeting = new Meeting(
        "m_01", "usr_01", "Intro call",
        "A focused 30 minute call to define scope and next steps.",
        30, "Europe/Moscow", true, 30, 120, 10, 10,
        "https://meet.example.com/sofia", "550e8400-e29b-41d4-a716-446655440000",
        now, now, new ArrayList<>());
    meetingRepository.save(meeting);

    ZonedDateTime today = ZonedDateTime.now(ZoneId.of("UTC")).withHour(0).withMinute(0).withSecond(0).withNano(0);
    ZonedDateTime day1 = today.plusDays(1);
    ZonedDateTime day2 = today.plusDays(2);

    Booking booking1 = new Booking(
        "bkg_1024", "usr_01", "m_01", "confirmed",
        "Maya Chen", "maya@example.com", "Europe/Berlin",
        day1.withHour(10).toInstant().toString(),
        day1.withHour(10).withMinute(30).toInstant().toString(),
        "Europe/Berlin",
        "https://meet.example.com/sofia", "Review onboarding flow.",
        null, null, null, now, now);
    bookingRepository.save(booking1);

    Booking booking2 = new Booking(
        "bkg_1025", "usr_01", "m_01", "confirmed",
        "Ivan Petrov", "ivan@example.com", "Europe/Moscow",
        day2.withHour(12).toInstant().toString(),
        day2.withHour(12).withMinute(30).toInstant().toString(),
        "Europe/Moscow",
        null, null, null, null, null, now, now);
    bookingRepository.save(booking2);
  }
}
