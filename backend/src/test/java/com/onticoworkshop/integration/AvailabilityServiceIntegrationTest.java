package com.onticoworkshop.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.onticoworkshop.BaseIntegrationTest;
import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.UpdateAvailabilityRequest;
import com.onticoworkshop.exception.NotFoundException;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;
import com.onticoworkshop.service.AvailabilityService;

class AvailabilityServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private AvailabilityService availabilityService;

  @Test
  void shouldCreateAndRetrieveAvailability() {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "Working hours",
        "UTC", null, null);
    Availability created = availabilityService.createAvailability(request);

    assertThat(created.getId()).startsWith("av_");
    assertThat(created.getName()).isEqualTo("Working hours");

    Availability found = availabilityService.getAvailability(created.getId());
    assertThat(found.getOrganizerId()).isEqualTo("org1");
  }

  @Test
  void shouldCreateAvailabilityWithRulesAndOverrides() {
    List<AvailabilityRule> rules = List.of(
        new AvailabilityRule(null, "monday", "09:00", "17:00"),
        new AvailabilityRule(null, "friday", "09:00", "15:00"));
    List<DateOverride> overrides = List.of(
        new DateOverride(null, "2024-12-25", true, null, null));

    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "Custom Hours",
        "UTC", rules, overrides);
    Availability created = availabilityService.createAvailability(request);

    assertThat(created.getRules()).hasSize(2);
    assertThat(created.getDateOverrides()).hasSize(1);
    assertThat(created.getDateOverrides().get(0).isUnavailable()).isTrue();
  }

  @Test
  void shouldUpdateAvailability() {
    CreateAvailabilityRequest createReq = new CreateAvailabilityRequest("org1", "Old Name",
        "UTC", null, null);
    Availability created = availabilityService.createAvailability(createReq);

    UpdateAvailabilityRequest updateReq = new UpdateAvailabilityRequest();
    updateReq.setName("New Name");
    updateReq.setRules(List.of(new AvailabilityRule(null, "tuesday", "10:00", "14:00")));

    Availability updated = availabilityService.updateAvailability(created.getId(), updateReq);

    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getRules()).hasSize(1);
    assertThat(updated.getRules().get(0).getWeekday()).isEqualTo("tuesday");
  }

  @Test
  void shouldDeleteAvailability() {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "To Delete",
        "UTC", null, null);
    Availability created = availabilityService.createAvailability(request);

    availabilityService.deleteAvailability(created.getId());

    assertThatThrownBy(() -> availabilityService.getAvailability(created.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void shouldListAvailabilitiesByOrganizer() {
    CreateAvailabilityRequest req1 = new CreateAvailabilityRequest("orgX", "A1", "UTC", null, null);
    CreateAvailabilityRequest req2 = new CreateAvailabilityRequest("orgX", "A2", "UTC", null, null);
    CreateAvailabilityRequest req3 = new CreateAvailabilityRequest("orgY", "A3", "UTC", null, null);
    availabilityService.createAvailability(req1);
    availabilityService.createAvailability(req2);
    availabilityService.createAvailability(req3);

    List<Availability> orgXList = availabilityService.listAvailabilities("orgX");

    assertThat(orgXList).hasSize(2);
    assertThat(orgXList).extracting("name").contains("A1", "A2");
  }

  @Test
  void shouldThrowNotFoundForDeleteMissing() {
    assertThatThrownBy(() -> availabilityService.deleteAvailability("av_nonexistent"))
        .isInstanceOf(NotFoundException.class);
  }
}
