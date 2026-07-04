package com.onticoworkshop.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String displayName;

  @NotBlank
  private String email;

  @NotBlank
  private String timezone;

  private String bio;

  private String avatarUrl;
}
