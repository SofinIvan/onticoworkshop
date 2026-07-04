package com.onticoworkshop.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {

  List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);

  List<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

  List<Booking> findByUserIdAndStartBetweenOrderByStartAsc(String userId, String start, String end);
}
