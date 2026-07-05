package com.onticoworkshop.dto;

import java.util.List;

import com.onticoworkshop.model.AvailabilityRule;
import com.onticoworkshop.model.DateOverride;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvailabilityRequest {

  private String name;

  private String timezone;

  private List<AvailabilityRule> rules;

  private List<DateOverride> dateOverrides;
}
