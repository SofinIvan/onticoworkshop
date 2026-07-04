package com.onticoworkshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleBookingRequest {

  @NotBlank
  private String start;

  private String guestTimezone;

  private String notes;
}
