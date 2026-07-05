package com.onticoworkshop.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {

  @NotBlank
  private String organizerId;

  @NotBlank
  private String title;

  private String description;

  @NotBlank
  private String timezone;
}
