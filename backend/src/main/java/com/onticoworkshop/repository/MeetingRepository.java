package com.onticoworkshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onticoworkshop.model.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, String> {

  List<Meeting> findByOrganizerIdOrderByCreatedAtAsc(String organizerId);

  Optional<Meeting> findByUuid(String uuid);
}
