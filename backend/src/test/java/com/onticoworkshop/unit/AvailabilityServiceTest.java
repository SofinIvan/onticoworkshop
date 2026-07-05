package com.onticoworkshop.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onticoworkshop.dto.CreateAvailabilityRequest;
import com.onticoworkshop.dto.UpdateAvailabilityRequest;
import com.onticoworkshop.model.Availability;
import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;
import com.onticoworkshop.repository.AvailabilityRepository;
import com.onticoworkshop.service.AvailabilityService;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

  @Mock
  private AvailabilityRepository repository;

  @InjectMocks
  private AvailabilityService availabilityService;

  private Availability buildAvailability(String id, String name) {
    return new Availability(id, "org1", name, "UTC",
        new ArrayList<>(), new ArrayList<>(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
  }

  @Test
  void createAvailability_shouldCreate() {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "My Availability", "UTC", null, null);
    when(repository.save(any(Availability.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Availability result = availabilityService.createAvailability(request);

    assertThat(result.getId()).startsWith("av_");
    assertThat(result.getName()).isEqualTo("My Availability");
    assertThat(result.getRules()).isEmpty();
    assertThat(result.getDateOverrides()).isEmpty();
  }

  @Test
  void createAvailability_shouldAcceptNullRulesAsEmpty() {
    CreateAvailabilityRequest request = new CreateAvailabilityRequest("org1", "Test", "UTC", null, null);
    when(repository.save(any(Availability.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Availability result = availabilityService.createAvailability(request);

    assertThat(result.getRules()).isNotNull().isEmpty();
    assertThat(result.getDateOverrides()).isNotNull().isEmpty();
  }

  @Test
  void updateAvailability_shouldPartiallyUpdate() {
    Availability existing = buildAvailability("av_1", "Old Name");
    when(repository.findById("av_1")).thenReturn(Optional.of(existing));
    when(repository.save(any(Availability.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateAvailabilityRequest request = new UpdateAvailabilityRequest();
    request.setName("New Name");

    Availability result = availabilityService.updateAvailability("av_1", request);

    assertThat(result.getName()).isEqualTo("New Name");
  }

  @Test
  void updateAvailability_shouldReplaceRulesAndDateOverrides() {
    Availability existing = buildAvailability("av_1", "Test");
    existing.getRules().add(new AvailabilityRule(1L, "monday", "09:00", "17:00"));
    existing.getDateOverrides().add(new DateOverride(1L, "2024-12-25", true, null, null));
    when(repository.findById("av_1")).thenReturn(Optional.of(existing));
    when(repository.save(any(Availability.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateAvailabilityRequest request = new UpdateAvailabilityRequest();
    request.setRules(List.of(new AvailabilityRule(null, "tuesday", "10:00", "14:00")));
    request.setDateOverrides(List.of(new DateOverride(null, "2024-12-31", false, "09:00", "13:00")));

    Availability result = availabilityService.updateAvailability("av_1", request);

    assertThat(result.getRules()).hasSize(1);
    assertThat(result.getRules().get(0).getWeekday()).isEqualTo("tuesday");
    assertThat(result.getDateOverrides()).hasSize(1);
  }

  @Test
  void deleteAvailability_shouldDelete() {
    when(repository.existsById("av_1")).thenReturn(true);

    availabilityService.deleteAvailability("av_1");

    verify(repository).deleteById("av_1");
  }
}
