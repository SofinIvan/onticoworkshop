package com.onticoworkshop.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.UpdateAvailabilityRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.repository.AvailabilityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

  private final AvailabilityRepository repository;

  @Transactional(readOnly = true)
  public List<Availability> listAvailabilities(String organizerId) {
    if (organizerId == null || organizerId.isBlank()) {
      return repository.findAll();
    }
    List<Availability> availabilities = repository.findByOrganizerIdOrderByCreatedAtAsc(organizerId);
    availabilities.forEach(a -> {
      a.getRules().size();
      a.getDateOverrides().size();
    });
    return availabilities;
  }

  @Transactional(readOnly = true)
  public Availability getAvailability(String availabilityId) {
    Availability availability = repository.findById(availabilityId)
        .orElseThrow(() -> new NotFoundException("Availability not found: " + availabilityId));
    availability.getRules().size();
    availability.getDateOverrides().size();
    return availability;
  }

  @Transactional
  public Availability createAvailability(CreateAvailabilityRequest request) {
    String now = Instant.now().toString();
    Availability availability = new Availability(
        "av_" + UUID.randomUUID().toString().substring(0, 8),
        request.getOrganizerId(),
        request.getName(),
        request.getTimezone(),
        request.getRules() != null ? request.getRules() : new ArrayList<>(),
        request.getDateOverrides() != null ? request.getDateOverrides() : new ArrayList<>(),
        now,
        now
    );
    return repository.save(availability);
  }

  @Transactional
  public Availability updateAvailability(String availabilityId, UpdateAvailabilityRequest request) {
    Availability availability = getAvailability(availabilityId);
    if (request.getName() != null) availability.setName(request.getName());
    if (request.getTimezone() != null) availability.setTimezone(request.getTimezone());
    if (request.getRules() != null) {
      availability.getRules().clear();
      availability.getRules().addAll(request.getRules());
    }
    if (request.getDateOverrides() != null) {
      availability.getDateOverrides().clear();
      availability.getDateOverrides().addAll(request.getDateOverrides());
    }
    availability.setUpdatedAt(Instant.now().toString());
    return repository.save(availability);
  }

  @Transactional
  public void deleteAvailability(String availabilityId) {
    if (!repository.existsById(availabilityId)) {
      throw new NotFoundException("Availability not found: " + availabilityId);
    }
    repository.deleteById(availabilityId);
  }
}
