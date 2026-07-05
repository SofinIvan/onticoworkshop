package com.onticoworkshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.Availability;

public interface AvailabilityRepository extends JpaRepository<Availability, String> {

  List<Availability> findByOrganizerIdOrderByCreatedAtAsc(String organizerId);
}
