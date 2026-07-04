package com.onticoworkshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String guestName;

  @NotBlank
  @Email
  private String guestEmail;

  @NotBlank
  private String guestTimezone;

  @NotBlank
  private String start;

  private String notes;
}
