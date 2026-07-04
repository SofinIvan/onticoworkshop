package com.onticoworkshop.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.onticoworkshop.dto.CreateAvailabilityScheduleRequest;
import com.onticoworkshop.dto.UpdateAvailabilityScheduleRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.AvailabilitySchedule;
import com.onticoworkshop.repository.AvailabilityScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityScheduleService {

  private final AvailabilityScheduleRepository repository;

  @Transactional(readOnly = true)
  public List<AvailabilitySchedule> listSchedules(String userId) {
    if (userId == null || userId.isBlank()) {
      return repository.findAll();
    }
    List<AvailabilitySchedule> schedules = repository.findByUserIdOrderByCreatedAtAsc(userId);
    schedules.forEach(s -> {
      s.getRules().size();
      s.getDateOverrides().size();
    });
    return schedules;
  }

  @Transactional(readOnly = true)
  public AvailabilitySchedule getSchedule(String scheduleId) {
    AvailabilitySchedule schedule = repository.findById(scheduleId)
        .orElseThrow(() -> new NotFoundException("Availability schedule not found: " + scheduleId));
    schedule.getRules().size();
    schedule.getDateOverrides().size();
    return schedule;
  }

  @Transactional
  public AvailabilitySchedule createSchedule(CreateAvailabilityScheduleRequest request) {
    String now = Instant.now().toString();
    AvailabilitySchedule schedule = new AvailabilitySchedule(
        "sch_" + UUID.randomUUID().toString().substring(0, 8),
        request.getUserId(),
        request.getName(),
        request.getTimezone(),
        request.getRules() != null ? request.getRules() : new ArrayList<>(),
        request.getDateOverrides() != null ? request.getDateOverrides() : new ArrayList<>(),
        now,
        now
    );
    return repository.save(schedule);
  }

  @Transactional
  public AvailabilitySchedule updateSchedule(String scheduleId, UpdateAvailabilityScheduleRequest request) {
    AvailabilitySchedule schedule = getSchedule(scheduleId);
    if (request.getName() != null) schedule.setName(request.getName());
    if (request.getTimezone() != null) schedule.setTimezone(request.getTimezone());
    if (request.getRules() != null) {
      schedule.getRules().clear();
      schedule.getRules().addAll(request.getRules());
    }
    if (request.getDateOverrides() != null) {
      schedule.getDateOverrides().clear();
      schedule.getDateOverrides().addAll(request.getDateOverrides());
    }
    schedule.setUpdatedAt(Instant.now().toString());
    return repository.save(schedule);
  }

  @Transactional
  public void deleteSchedule(String scheduleId) {
    if (!repository.existsById(scheduleId)) {
      throw new NotFoundException("Availability schedule not found: " + scheduleId);
    }
    repository.deleteById(scheduleId);
  }
}
