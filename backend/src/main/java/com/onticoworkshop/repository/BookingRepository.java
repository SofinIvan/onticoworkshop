package com.onticoworkshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {

  List<Booking> findByOrganizerIdOrderByCreatedAtDesc(String organizerId);

  List<Booking> findByOrganizerIdAndStatusOrderByCreatedAtDesc(String organizerId, String status);

  List<Booking> findByOrganizerIdAndStartBetweenOrderByStartAsc(String organizerId, String start, String end);

  void deleteByMeetingId(String meetingId);

  List<Booking> findByMeetingIdAndGuestEmailAndStatusNot(String meetingId, String guestEmail, String status);
}
