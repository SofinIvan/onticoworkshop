package com.onticoworkshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.AvailabilitySchedule;

public interface AvailabilityScheduleRepository extends JpaRepository<AvailabilitySchedule, String> {

  List<AvailabilitySchedule> findByUserIdOrderByCreatedAtAsc(String userId);
}
