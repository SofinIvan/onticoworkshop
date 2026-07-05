package com.onticoworkshop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseRepositoryTest;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;

class AvailabilityRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private AvailabilityRepository availabilityRepository;

  @Test
  void shouldSaveAvailabilityWithRulesAndOverrides() {
    List<AvailabilityRule> rules = List.of(
        new AvailabilityRule(null, "monday", "09:00", "17:00"),
        new AvailabilityRule(null, "wednesday", "10:00", "18:00"));
    List<DateOverride> overrides = List.of(
        new DateOverride(null, "2024-12-25", true, null, null));

    Availability availability = new Availability("av_t1", "org1", "Working hours",
        "UTC", rules, overrides, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");

    availabilityRepository.save(availability);

    Availability found = availabilityRepository.findById("av_t1").orElseThrow();

    assertThat(found.getRules()).hasSize(2);
    assertThat(found.getDateOverrides()).hasSize(1);
    assertThat(found.getDateOverrides().get(0).isUnavailable()).isTrue();
  }

  @Test
  void shouldFindByOrganizerId() {
    Availability a1 = new Availability("av_a1", "orgA", "Default", "UTC",
        new ArrayList<>(), new ArrayList<>(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    Availability a2 = new Availability("av_a2", "orgB", "Custom", "UTC",
        new ArrayList<>(), new ArrayList<>(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    availabilityRepository.saveAll(List.of(a1, a2));

    List<Availability> result = availabilityRepository.findByOrganizerIdOrderByCreatedAtAsc("orgA");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Default");
  }
}
