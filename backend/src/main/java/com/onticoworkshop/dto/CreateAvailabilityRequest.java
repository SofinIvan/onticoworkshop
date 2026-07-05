package com.onticoworkshop.dto;

import java.util.List;

import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvailabilityRequest {

  @NotBlank
  private String organizerId;

  @NotBlank
  private String name;

  @NotBlank
  private String timezone;

  private List<AvailabilityRule> rules;

  private List<DateOverride> dateOverrides;
}
